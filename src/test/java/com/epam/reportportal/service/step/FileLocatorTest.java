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

package com.epam.reportportal.service.step;

import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.io.ByteStreams;
import io.reactivex.Maybe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class FileLocatorTest {

	private final String testLaunchUuid = "launch" + UUID.randomUUID().toString().substring(6);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);
	private final Maybe<String> launchUuid = TestUtils.getConstantMaybe(testLaunchUuid);

	@Mock
	private ReportPortalClient rpClient;

	private Launch launch;
	private Maybe<String> testMethodUuidMaybe;
	private StepReporter sr;

	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		return TestUtils.getConstantMaybe(new ItemCreatedRS(uuid, uuid));
	};

	@BeforeEach
	public void initMocks() {
		Maybe<ItemCreatedRS> testMethodCreatedMaybe = TestUtils.getConstantMaybe(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(rpClient.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);
		when(rpClient.log(any(MultiPartRequest.class))).thenReturn(TestUtils.getConstantMaybe(new BatchSaveOperatingRS()));

		// mock start nested steps
		when(rpClient.startTestItem(eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());

		ReportPortal rp = ReportPortal.create(rpClient, TestUtils.STANDARD_PARAMETERS);
		launch = rp.withLaunch(launchUuid);
		testMethodUuidMaybe = launch.startTestItem(TestUtils.getConstantMaybe(testClassUuid), TestUtils.standardStartStepRequest());
		sr = launch.getStepReporter();
	}

	@Test
	public void test_file_location_by_relative_workdir_path() throws IOException {
		sr.sendStep("Test image by relative workdir path", new File("src/test/resources/pug/lucky.jpg"));

		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(rpClient, timeout(1000).times(1)).log(logCaptor.capture());

		MultiPartRequest logRq = logCaptor.getValue();
		verifyFile(logRq, ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("pug/lucky.jpg")), "lucky.jpg");
	}

	@Test
	public void test_file_location_by_relative_classpath_path() throws IOException {
		sr.sendStep("Test image by relative classpath path", new File("pug/unlucky.jpg"));

		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(rpClient, timeout(1000).times(1)).log(logCaptor.capture());

		MultiPartRequest logRq = logCaptor.getValue();
		verifyFile(logRq, ByteStreams.toByteArray(getClass().getClassLoader().getResourceAsStream("pug/unlucky.jpg")), "unlucky.jpg");
	}

	@Test
	public void test_file_not_found_in_path() {
		sr.sendStep("Test image by relative classpath path", new File("pug/not_exists.jpg"));

		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(rpClient, timeout(1000).times(1)).log(logCaptor.capture());

		MultiPartRequest logRq = logCaptor.getValue();
		SaveLogRQ saveRq = verifyRq(logRq);
		assertThat(saveRq.getFile(), nullValue());
	}

	private void verifyFile(MultiPartRequest logRq, byte[] data, String fileName) {
		SaveLogRQ saveRq = verifyRq(logRq);

		assertThat(saveRq.getFile(), notNullValue());
		assertThat("File name is invalid", saveRq.getMessage(), equalTo(fileName));
		SaveLogRQ.File file = saveRq.getFile();
		assertThat("File binary content is invalid", file.getContent(), equalTo(data));
	}

	@SuppressWarnings("unchecked")
	private SaveLogRQ verifyRq(MultiPartRequest logRq) {
		List<MultiPartRequest.MultiPartSerialized<?>> mpRqs = logRq.getSerializedRQs();
		assertThat(mpRqs, hasSize(1));

		Object rqs = mpRqs.get(0).getRequest();
		assertThat(rqs, instanceOf(List.class));
		List<Object> rqList = (List<Object>) rqs;
		assertThat(rqList, hasSize(1));

		Object rq = rqList.get(0);
		assertThat(rq, instanceOf(SaveLogRQ.class));
		return (SaveLogRQ) rq;
	}
}
