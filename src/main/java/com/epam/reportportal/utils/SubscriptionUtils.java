/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.utils;

import com.epam.reportportal.listeners.ListenerParameters;
import io.reactivex.CompletableObserver;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dzianis_Shybeka
 */
public class SubscriptionUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionUtils.class);

	public static <T> MaybeObserver<T> logMaybeResults(final String type) {
		return new MaybeObserver<>() {

			@Override
			public void onSubscribe(@Nonnull Disposable d) {
				//				ignore
			}

			@Override
			public void onSuccess(@Nonnull T result) {
				LOGGER.debug("{} successfully completed", type);
			}

			@Override
			public void onError(@Nonnull Throwable e) {
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
			public void onSubscribe(@Nonnull Disposable d) {
				//				ignore
			}

			@Override
			public void onError(@Nonnull Throwable e) {
				LOGGER.error("[{}] ReportPortal {} execution error", Thread.currentThread().getId(), type, e);
			}

			@Override
			public void onComplete() {
				LOGGER.debug("{} completed", type);
			}
		};
	}

	public static MaybeObserver<String> printLaunch(@Nonnull ListenerParameters parameters) {
		return new MaybeObserver<>() {
			@Override
			public void onSubscribe(@Nonnull Disposable d) {
				// ignore
			}

			@Override
			public void onSuccess(@Nonnull String result) {
				parameters.getPrintLaunchUuidOutput().println("ReportPortal Launch UUID: " + result);
			}

			@Override
			public void onError(@Nonnull Throwable e) {
				// ignore
			}

			@Override
			public void onComplete() {
				// ignore
			}
		};
	}
}
