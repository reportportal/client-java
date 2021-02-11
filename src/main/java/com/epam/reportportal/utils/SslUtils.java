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
package com.epam.reportportal.utils;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.restendpoint.http.IOUtils;
import com.epam.reportportal.service.ReportPortal;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.KeyStore;

/**
 * @author Andrei Varabyeu
 */
public class SslUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(SslUtils.class);

	/**
	 * Load keystore
	 *
	 * @param keyStore keystore resource
	 * @param password keystore password
	 * @return JKD keystore representation
	 */
	public static KeyStore loadKeyStore(String keyStore, String password) {
		InputStream is = null;
		try {
			is = Resources.asByteSource(Resources.getResource(keyStore)).openStream();
			KeyStore trustStore = KeyStore.getInstance("JKS");
			trustStore.load(is, password.toCharArray());
			return trustStore;
		} catch (Exception e) {
			String error = "Unable to load trust store";
			LOGGER.error(error, e);
			throw new InternalReportPortalClientException(error, e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
}
