/*
 * Copyright 2024 EPAM Systems
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

package com.epam.reportportal.utils.reflect;

@SuppressWarnings("unused")
public class BaseReflectTest {
	public static final String PUBLIC_BASE_FIELD_VALUE = "public_base_field";
	public static final String PRIVATE_BASE_FIELD_VALUE = "private_base_field";
	public static final String PUBLIC_BASE_METHOD_NO_PARAMS_VALUE = "public_base_method_no_params";
	public static final String PRIVATE_BASE_METHOD_NO_PARAMS_VALUE = "private_base_method_no_params";
	public static final String PUBLIC_BASE_METHOD_PARAMS_VALUE = "public_base_method_params";
	public static final String PRIVATE_BASE_METHOD_PARAMS_VALUE = "private_base_method_params";

	public final String publicBaseField = PUBLIC_BASE_FIELD_VALUE;
	private final String privateBaseField = PRIVATE_BASE_FIELD_VALUE;

	public String publicBaseMethodNoParams() {
		return PUBLIC_BASE_METHOD_NO_PARAMS_VALUE;
	}

	private String privateBaseMethodNoParams() {
		return PRIVATE_BASE_METHOD_NO_PARAMS_VALUE;
	}

	public String publicBaseMethodParams(String param1, String param2) {
		return PUBLIC_BASE_METHOD_PARAMS_VALUE;
	}

	private String privateBaseMethodParams(String param1, String param2) {
		return PRIVATE_BASE_METHOD_PARAMS_VALUE;
	}

}
