/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Returns a supplier which caches the instance retrieved during the first call to get() and returns that value on subsequent calls to get().
 *
 * @param <T> the supplier type
 */
public class MemoizingSupplier<T> implements Supplier<T>, Serializable {
	private final Supplier<T> delegate;
	private transient volatile boolean initialized;
	private transient volatile T value;
	private static final long serialVersionUID = 0L;

	public MemoizingSupplier(@Nonnull final Supplier<T> delegate) {
		this.delegate = delegate;
	}

	public T get() {
		if (!initialized) {
			synchronized (this) {
				if (!initialized) {
					value = delegate.get();
					initialized = true;
					return value;
				}
			}
		}
		return value;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public synchronized void reset() {
		initialized = false;
	}

	public String toString() {
		return "MemoizingSupplier(" + delegate + ")";
	}
}
