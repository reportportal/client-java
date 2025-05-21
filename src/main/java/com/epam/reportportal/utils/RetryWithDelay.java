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

import io.reactivex.functions.Predicate;

import jakarta.annotation.Nonnull;

import static java.lang.Thread.sleep;

/**
 * Retry with Delay and attempts limits
 *
 * @author Andrei Varabyeu
 */
public class RetryWithDelay implements Predicate<Throwable> {

	private final Predicate<? super Throwable> predicate;
	private final long maxRetries;
	private final long retryDelayMillis;
	private int retryCount;

	public RetryWithDelay(Predicate<? super Throwable> predicate, final long maxRetries, final long retryDelayMillis) {
		this.maxRetries = maxRetries;
		this.retryDelayMillis = retryDelayMillis;
		this.retryCount = 0;
		this.predicate = predicate;
	}

	@Override
	public boolean test(@Nonnull final Throwable throwable) throws Exception {
		try {
			//check whether we should retry this exception
			if (!predicate.test(throwable)) {
				return false;
			}
		} catch (Exception e) {
			//pass the error if smth goes wrong
			return false;
		}

		if (++retryCount < maxRetries) {
			sleep(retryDelayMillis);
			return true;
		}

		// Max retries hit. Just pass the error along.
		return false;
	}
}
