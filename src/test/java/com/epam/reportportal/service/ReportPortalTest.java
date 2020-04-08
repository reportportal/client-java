/*
 * Copyright 2020 EPAM Systems
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
package com.epam.reportportal.service;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReportPortalTest {

	@Mock
	private ReportPortalClient rpClient;
	@Mock
	private ExecutorService executor;
	@Mock
	private ListenerParameters parameters;
	@Mock
	private LockFile lockFile;

	@InjectMocks
	private ReportPortal reportPortal;

	@Test
	public void noUrlResultsInException(){
		ListenerParameters listenerParameters = new ListenerParameters();
		assertThrows(InternalReportPortalClientException.class, () -> ReportPortal.builder().defaultClient(listenerParameters));
	}
}
