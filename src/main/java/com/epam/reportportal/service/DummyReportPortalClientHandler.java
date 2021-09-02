/*
 *  Copyright 2021 EPAM Systems
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

package com.epam.reportportal.service;

import io.reactivex.Maybe;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * An {@link InvocationHandler} to mock {@link ReportPortalClient} interface for noop launch case.
 */
public class DummyReportPortalClientHandler implements InvocationHandler {
	@Override
	public String toString() {
		return getClass().getName() + '@' + Integer.toHexString(hashCode());
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		String methodName = method.getName();
		switch (methodName) {
			case "toString":
				return toString();
			case "hashCode":
				return hashCode();
			case "equals":
				return args[0] == this;
		}
		return method.getReturnType() == Void.class ? null : Maybe.empty();
	}
}
