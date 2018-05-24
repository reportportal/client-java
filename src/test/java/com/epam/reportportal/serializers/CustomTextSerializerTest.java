package com.epam.reportportal.serializers;

import com.epam.reportportal.exception.GeneralReportPortalException;
import com.google.common.net.MediaType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomTextSerializerTest {

	@Test
	public void canRead() throws Exception {

		assertFalse(new NotJsonSerializer().canRead(MediaType.JSON_UTF_8, String.class));

		assertTrue(new NotJsonSerializer().canRead(MediaType.ANY_TYPE, String.class));
		assertTrue(new NotJsonSerializer().canRead(MediaType.ANY_IMAGE_TYPE, String.class));
		assertTrue(new NotJsonSerializer().canRead(MediaType.HTML_UTF_8, String.class));
		assertTrue(new NotJsonSerializer().canRead(MediaType.ANY_TEXT_TYPE, String.class));
		assertTrue(new NotJsonSerializer().canRead(MediaType.TEXT_JAVASCRIPT_UTF_8, String.class));
	}

	@Test
	public void canWrite() throws Exception {
		assertFalse(new NotJsonSerializer().canWrite(null));
		assertFalse(new NotJsonSerializer().canWrite(new Object()));
	}

	@Test(expected = GeneralReportPortalException.class)
	public void serialize() throws Exception {
		new NotJsonSerializer().serialize("test string");
	}
}
