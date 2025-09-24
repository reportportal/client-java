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

import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.utils.files.Utils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

@SuppressWarnings("unchecked")
public class FileLocatorTest {
	private static final InputStream EMPTY_STREAM = new ByteArrayInputStream(new byte[0]);

	private final String testLaunchUuid = "launch" + UUID.randomUUID().toString().substring(6);
	private final String testClassUuid = "class" + UUID.randomUUID().toString().substring(5);
	private final String testMethodUuid = "test" + UUID.randomUUID().toString().substring(4);
	private final Maybe<String> launchUuid = Maybe.just(testLaunchUuid);

	@Mock
	private ReportPortalClient rpClient;

	private StepReporter sr;

	private final Supplier<Maybe<ItemCreatedRS>> maybeSupplier = () -> {
		String uuid = UUID.randomUUID().toString();
		return Maybe.just(new ItemCreatedRS(uuid, uuid));
	};

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void initMocks() {
		Maybe<ItemCreatedRS> testMethodCreatedMaybe = Maybe.just(new ItemCreatedRS(testMethodUuid, testMethodUuid));
		when(rpClient.startTestItem(eq(testClassUuid), any())).thenReturn(testMethodCreatedMaybe);
		when(rpClient.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));

		// mock start nested steps
		when(rpClient.startTestItem(
				eq(testMethodUuid),
				any()
		)).thenAnswer((Answer<Maybe<ItemCreatedRS>>) invocation -> maybeSupplier.get());

		ReportPortal rp = ReportPortal.create(rpClient, TestUtils.STANDARD_PARAMETERS);
		Launch launch = rp.withLaunch(launchUuid);
		launch.startTestItem(Maybe.just(testClassUuid), TestUtils.standardStartStepRequest());
		sr = launch.getStepReporter();
	}

	@Test
	public void test_file_location_by_relative_workdir_path() throws IOException {
		sr.sendStep("Test image by relative workdir path", new File("src/test/resources/pug/lucky.jpg"));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(1)).log(logCaptor.capture());

		verifyFile(
				logCaptor.getValue(),
				Utils.readInputStreamToBytes(ofNullable(getClass().getClassLoader().getResourceAsStream("pug/lucky.jpg")).orElse(
						EMPTY_STREAM))
		);
	}

	@Test
	public void test_file_location_by_relative_classpath_path() throws IOException {
		sr.sendStep("Test image by relative classpath path", new File("pug/unlucky.jpg"));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(1)).log(logCaptor.capture());

		verifyFile(
				logCaptor.getValue(),
				Utils.readInputStreamToBytes(ofNullable(getClass().getClassLoader().getResourceAsStream("pug/unlucky.jpg")).orElse(
						EMPTY_STREAM))
		);
	}

	@Test
	public void test_file_not_found_in_path() {
		sr.sendStep("Test image by relative classpath path", new File("pug/not_exists.jpg"));

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, timeout(1000).times(1)).log(logCaptor.capture());

		SaveLogRQ saveRq = verifyRq(logCaptor.getValue());
		assertThat(saveRq.getFile(), nullValue());
	}

	@Test
	public void test_file_absolute_long_path() throws IOException {
		String basedir = System.getProperty("user.dir");
		File testFile = new File(String.join(
				File.separator,
				basedir,
				"src",
				"test",
				"resources",
				"pug",
				"Long directory name with spaces and Capital letters",
				"lucky.jpg"
		));

		sr.sendStep("Test image by relative classpath path", testFile);

		ArgumentCaptor<List<MultipartBody.Part>> logCaptor = ArgumentCaptor.forClass(List.class);
		verify(rpClient, after(1000).times(1)).log(logCaptor.capture());

		verifyFile(
				logCaptor.getValue(),
				Utils.readInputStreamToBytes(ofNullable(getClass().getClassLoader().getResourceAsStream("pug/lucky.jpg")).orElse(
						EMPTY_STREAM))
		);
	}

	private void verifyFile(List<MultipartBody.Part> logRq, byte[] data) {
		SaveLogRQ saveRq = verifyRq(logRq);

		String fileName = saveRq.getFile().getName();
		List<byte[]> binaries = logRq.stream()
				.filter(p -> ofNullable(p.headers()).map(h -> h.get("Content-Disposition"))
						.map(h -> h.contains(String.format("filename=\"%s\"", fileName)))
						.orElse(false))
				.map(MultipartBody.Part::body)
				.map(b -> {
					Buffer buf = new Buffer();
					try {
						b.writeTo(buf);
					} catch (IOException ignore) {
					}
					return buf.readByteArray();
				})
				.collect(Collectors.toList());

		assertThat(binaries, hasSize(1));

		assertThat(saveRq.getFile(), notNullValue());
		assertThat("Do not publish file name as message", saveRq.getMessage(), emptyString());
		assertThat("File binary content is invalid", binaries.get(0), equalTo(data));
	}

	private SaveLogRQ verifyRq(List<MultipartBody.Part> logRq) {
		List<SaveLogRQ> rqList = TestUtils.extractJsonParts(logRq);
		assertThat(rqList, hasSize(1));
		return rqList.get(0);
	}
}
