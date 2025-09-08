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

package com.epam.reportportal.utils;

/**
 * Common constants used across the ReportPortal client.
 */
public class CommonConstants {
	private CommonConstants() {
		throw new IllegalStateException("Static only class");
	}

	public static int KILOBYTES = (int) Math.pow(2, 10);
	public static int MEGABYTES = (int) Math.pow(KILOBYTES, 2);
	public static int TEN_MEGABYTES = 10 * MEGABYTES;

	public static final String DEFAULT_TRUNCATE_REPLACEMENT = "...";
}
