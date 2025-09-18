package com.epam.reportportal.utils;

import com.epam.reportportal.listeners.ListenerParameters;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for working with multithreading in ReportPortal client.
 * Provides methods for creating and shutting down executor services.
 */
public class MultithreadingUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultithreadingUtils.class);

	/**
	 * Creates a fixed thread pool executor service with daemon threads and custom naming pattern.
	 *
	 * @param namePrefix  prefix for thread names created by this executor
	 * @param threadCount number of threads in the pool
	 * @return a new fixed thread pool executor service
	 */
	public static ExecutorService buildExecutorService(String namePrefix, int threadCount) {
		AtomicLong threadCounter = new AtomicLong();
		ThreadFactory threadFactory = r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName(namePrefix + threadCounter.incrementAndGet());
			return t;
		};
		return Executors.newFixedThreadPool(threadCount, threadFactory);
	}

	/**
	 * Creates a fixed thread pool executor service with daemon threads using pool size from listener parameters.
	 *
	 * @param namePrefix prefix for thread names created by this executor
	 * @param params     listener parameters containing IO pool size configuration
	 * @return a new fixed thread pool executor service
	 */
	public static ExecutorService buildExecutorService(String namePrefix, ListenerParameters params) {
		return buildExecutorService(namePrefix, params.getIoPoolSize());
	}

	/**
	 * Gracefully shuts down an executor service with a specified timeout.
	 * If the executor service doesn't terminate within the specified time, it will be forcibly shut down.
	 * If the shutdown is interrupted, the executor service will also be forcibly shut down.
	 *
	 * @param executorService the executor service to shut down, must not be null
	 * @param duration        the maximum time to wait for termination
	 * @param timeUnit        the time unit of the duration parameter, must not be null
	 */
	public static void shutdownExecutorService(@Nonnull ExecutorService executorService, long duration, @Nonnull TimeUnit timeUnit) {
		if (!executorService.isShutdown()) {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(duration, timeUnit)) {
					LOGGER.warn("Executor service did not terminate in the specified time. Force shutting down.");
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				LOGGER.warn("Executor service was interrupted during shutdown. Force shutting down.");
				executorService.shutdownNow();
			}
		}
	}
}
