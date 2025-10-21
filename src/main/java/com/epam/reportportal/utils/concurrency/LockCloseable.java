/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.utils.concurrency;

import java.util.concurrent.locks.Lock;

/**
 * AutoCloseable wrapper for Lock that enables try-with-resources usage.
 */
public class LockCloseable implements AutoCloseable {
	private final Lock lock;

	public LockCloseable(Lock lock) {
		this.lock = lock;
	}

	/**
	 * Locks the underlying lock and returns this instance for use in try-with-resources.
	 *
	 * @return this LockCloseable instance
	 */
	public LockCloseable lock() {
		lock.lock();
		return this;
	}

	@Override
	public void close() {
		lock.unlock();
	}
}