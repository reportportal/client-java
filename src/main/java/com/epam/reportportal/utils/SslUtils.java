/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-java-core
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.utils;

import com.github.avarabyeu.restendpoint.http.IOUtils;
import com.google.common.io.Resources;

import java.io.InputStream;
import java.security.KeyStore;

/**
 * @author Andrei Varabyeu
 */
public class SslUtils {

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
            throw new RuntimeException("Unable to load trust store", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
