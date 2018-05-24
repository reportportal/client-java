package com.epam.reportportal.utils;

import io.reactivex.CompletableObserver;
import io.reactivex.FlowableSubscriber;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionUtils.class);

	public static <T> MaybeObserver<T> logMaybeResults(final String type) {

		return new MaybeObserver<T>() {

			@Override
			public void onSubscribe(Disposable d) {
				//				ignore
			}

			@Override
			public void onSuccess(T Result) {
				LOGGER.debug("{} successfully completed", type);
			}

			@Override
			public void onError(Throwable e) {
				LOGGER.error("{} completed with error ", type, e);
			}

			@Override
			public void onComplete() {
				LOGGER.debug("{} completed", type);
			}
		};
	}

	public static <T> FlowableSubscriber<T> logFlowableResults(final String type) {

		return new FlowableSubscriber<T>() {

			@Override
			public void onSubscribe(Subscription s) {
				//				ignore
			}

			@Override
			public void onNext(T result) {
				//				ignore
			}

			@Override
			public void onError(Throwable e) {
				LOGGER.error("{} completed with error ", type, e);
			}

			@Override
			public void onComplete() {
				LOGGER.debug("{} completed", type);
			}
		};
	}

	public static CompletableObserver logCompletableResults(final String type) {

		return new CompletableObserver() {

			@Override
			public void onSubscribe(Disposable d) {
				//				ignore
			}

			@Override
			public void onError(Throwable e) {
				LOGGER.error("{} completed with error ", type, e);
			}

			@Override
			public void onComplete() {
				LOGGER.debug("{} completed", type);
			}
		};
	}
}
