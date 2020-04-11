package com.epam.reportportal.service;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.listeners.ListenerParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

@RunWith(MockitoJUnitRunner.class)
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

	@Test(expected = InternalReportPortalClientException.class)
	public void noUrlResultsInException() throws MalformedURLException {
		ListenerParameters listenerParameters = new ListenerParameters();
		ReportPortal.builder().defaultClient(listenerParameters);
	}

}