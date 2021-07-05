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

package com.epam.reportportal.service.launch.lock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hamcrest.Matchers.notNullValue;

public class LockTestUtil {
	public static final String WELCOME_MESSAGE = "Lock ready, press any key to continue...";

	public static final Predicate<String> WELCOME_MESSAGE_PREDICATE = WELCOME_MESSAGE::equals;
	public static final Predicate<String> ANY_STRING_PREDICATE = input -> !isEmpty(input);


	public static Triple<OutputStreamWriter, BufferedReader, BufferedReader> getProcessIos(Process process) {
		return ImmutableTriple.of(new OutputStreamWriter(process.getOutputStream()),
				new BufferedReader(new InputStreamReader(process.getInputStream())),
				new BufferedReader(new InputStreamReader(process.getErrorStream()))
		);
	}

	public static void closeIos(Triple<OutputStreamWriter, BufferedReader, BufferedReader> io) {
		try {
			io.getLeft().close();
			io.getMiddle().close();
			io.getRight().close();
		} catch (IOException ignore) {

		}
	}

	@SuppressWarnings("unchecked")
	public static String waitForLine(final BufferedReader reader, final BufferedReader errorReader, final Predicate<String> linePredicate)
			throws IOException {
		try {
			return Awaitility.await("Waiting for a line")
					.timeout(8, TimeUnit.SECONDS)
					.pollInterval(100, TimeUnit.MILLISECONDS)
					.until(() -> {
						if (!reader.ready()) {
							return null;
						}
						String line;
						while ((line = reader.readLine()) != null) {
							if (linePredicate.test(line)) {
								return line;
							}
						}
						return null;
					}, notNullValue());
		} catch (ConditionTimeoutException e) {
			List<String> errorLines = Collections.EMPTY_LIST;
			if (errorReader.ready()) {
				errorLines = IOUtils.readLines(errorReader);
			}
			String lineSeparator = System.getProperty("line.separator");
			throw new IllegalStateException("Unable to run test class: " + join(errorLines, lineSeparator));
		}
	}

}
