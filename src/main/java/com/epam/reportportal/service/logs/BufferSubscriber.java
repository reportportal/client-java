package com.epam.reportportal.service.logs;

import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A subscriber that buffers incoming {@link SaveLogRQ} objects until certain conditions are met,
 * then emits them as batches to the downstream subscriber.
 * <p>
 * This class implements a buffering mechanism with two boundary conditions:
 * <ul>
 *     <li>Maximum number of log items in a batch (specified by {@code batchMaxSize})</li>
 *     <li>Maximum payload size of a batch in bytes (specified by {@code batchPayloadLimit})</li>
 * </ul>
 * <p>
 * When either of these conditions is met, the current buffer is emitted downstream as a list and a new buffer is created.
 * This helps optimize network requests by batching multiple logs together while ensuring the batch size remains within
 * reasonable limits.
 * <p>
 * The class is thread-safe, using a {@link ReentrantLock} to protect access to the buffer during concurrent operations.
 */
public class BufferSubscriber implements FlowableSubscriber<SaveLogRQ>, Subscription {
	private final ReentrantLock lock = new ReentrantLock();
	private final Subscriber<List<SaveLogRQ>> downstream;
	private final int maxSize;
	private final long payloadLimit;

	private volatile List<SaveLogRQ> buffer;
	private volatile long payloadSize;
	private volatile Subscription upstream;
	private volatile boolean done;

	public BufferSubscriber(Subscriber<List<SaveLogRQ>> actual, int batchMaxSize, long batchPayloadLimit) {
		downstream = actual;
		maxSize = batchMaxSize;
		payloadLimit = batchPayloadLimit;
	}

	@Override
	public void onSubscribe(@Nonnull Subscription s) {
		if (!SubscriptionHelper.validate(upstream, s)) {
			return;
		}
		upstream = s;
		buffer = new ArrayList<>();
		payloadSize = HttpRequestUtils.TYPICAL_MULTIPART_FOOTER_LENGTH;

		downstream.onSubscribe(this);
	}

	private void reset() {
		buffer = new ArrayList<>();
		payloadSize = HttpRequestUtils.TYPICAL_MULTIPART_FOOTER_LENGTH;
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public void onNext(SaveLogRQ t) {
		if (done) {
			return;
		}
		long size = HttpRequestUtils.calculateRequestSize(t);
		List<List<SaveLogRQ>> toSend = new ArrayList<>();
		lock.lock();
		if (buffer == null) {
			lock.unlock();
			return;
		}
		if (payloadSize + size > payloadLimit) {
			if (!buffer.isEmpty()) {
				toSend.add(buffer);
				reset();
			}
		}
		buffer.add(t);
		long newSize = payloadSize + size;
		payloadSize = newSize;
		if (buffer.size() >= maxSize) {
			toSend.add(buffer);
			reset();
		}
		lock.unlock();
		toSend.forEach(downstream::onNext);
	}

	@Override
	public void onError(Throwable t) {
		if (done) {
			RxJavaPlugins.onError(t);
			return;
		}
		done = true;
		downstream.onError(t);
	}

	@Override
	public void onComplete() {
		if (done) {
			return;
		}
		done = true;

		List<List<SaveLogRQ>> toSend = new ArrayList<>();
		lock.lock();
		if (buffer != null && !buffer.isEmpty()) {
			toSend.add(buffer);
			reset();
		}
		lock.unlock();
		toSend.forEach(downstream::onNext);
		downstream.onComplete();
	}

	@Override
	public void request(long n) {
		if (SubscriptionHelper.validate(n)) {
			upstream.request(BackpressureHelper.multiplyCap(n, maxSize));
		}
	}

	@Override
	public void cancel() {
		upstream.cancel();
	}
}
