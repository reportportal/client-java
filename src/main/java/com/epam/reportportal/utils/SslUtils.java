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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static com.epam.reportportal.utils.files.Utils.getFile;
import static java.util.Optional.ofNullable;

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
	@Nonnull
	public static KeyStore loadKeyStore(@Nonnull String keyStore, @Nullable String password) {
		try (InputStream is = getFile(new File(keyStore)).openStream()) {
			KeyStore trustStore = KeyStore.getInstance("JKS");
			trustStore.load(is, ofNullable(password).map(String::toCharArray).orElse(null));
			return trustStore;
		} catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
			String error = "Unable to load key store";
			LOGGER.error(error, e);
			throw new InternalReportPortalClientException(error, e);
		}
	}
}
