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
package com.epam.reportportal.listeners;

import com.epam.reportportal.utils.properties.PropertiesLoader;
import org.junit.jupiter.api.Test;

import static com.epam.ta.reportportal.ws.model.launch.Mode.DEBUG;
import static com.epam.ta.reportportal.ws.model.launch.Mode.DEFAULT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ListenerParametersTest {

	@Test
	public void testParseLaunchMode() {
		assertThat(new ListenerParameters().parseLaunchMode("notvalid"), equalTo(DEFAULT));
		assertThat(new ListenerParameters().parseLaunchMode("Debug"), equalTo(DEBUG));
	}

	@Test
	public void testNoNPEs() {
		PropertiesLoader properties = PropertiesLoader.load();
		ListenerParameters listenerParameters = new ListenerParameters(properties);

		assertThat(listenerParameters.getBaseUrl(), nullValue());
	}

}