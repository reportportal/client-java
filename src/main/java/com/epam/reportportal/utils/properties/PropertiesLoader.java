/*
 * Copyright 2016 EPAM Systems
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
package com.epam.reportportal.utils.properties;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.io.Closer;
import com.google.common.io.Resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import static com.epam.reportportal.utils.properties.ListenerProperty.values;
import static com.google.common.base.Suppliers.memoize;

/**
 * Load report portal launch start properties
 */
public class PropertiesLoader {

    public static final String INNER_PATH = "reportportal.properties";
    public static final String PATH = "./reportportal.properties";
    private static final String[] PROXY_PROPERTIES = { "http.proxyHost", "http.proxyPort", "http.nonProxyHosts",
            "https.proxyHost",
            "https.proxyPort", "ftp.proxyHost", "ftp.proxyPort", "ftp.nonProxyHosts", "socksProxyHost",
            "socksProxyPort", "http.proxyUser",
            "http.proxyPassword" };

    private static Supplier<Properties> PROPERTIES_SUPPLIER = memoize(new Supplier<Properties>() {
        @Override
        public Properties get() {
            try {
                return loadProperties();
            } catch (IOException e) {
                throw new InternalReportPortalClientException("Unable to load properties", e);
            }
        }
    });

    /**
     * Get specified property loaded from properties file and reloaded from from
     * environment variables.
     *
     * @param propertyName Name of property
     */
    public static String getProperty(String propertyName) {
        return PROPERTIES_SUPPLIER.get().getProperty(propertyName);
    }

    /**
     * Get all properties loaded from properties file and reloaded from from
     * environment variables.
     */
    public static Properties getProperties() {
        return PROPERTIES_SUPPLIER.get();
    }

    /**
     * Try to load properties from file situated in the class path, and then
     * reload existing parameters from environment variables
     *
     * @return loaded properties
     * @throws IOException In case of IO error
     */
    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        Optional<URL> propertyFile = getResource(INNER_PATH);
        if (propertyFile.isPresent()) {
            props.load(Resources.asByteSource(propertyFile.get()).openBufferedStream());
        }
        overrideWith(props, System.getProperties());
        overrideWith(props, System.getenv());

        validateProperties(props);
        setProxyProperties(props);
        return props;
    }

    /**
     * Current version of agents should load properties only from properties
     * file on classpath
     */
    @SuppressWarnings("unused")
    @Deprecated()
    private static Properties loadFromFile() throws IOException {
        Properties props = new Properties();
        File propertiesFile = new File(PATH);
        InputStream is;
        try (Closer closer = Closer.create()) {
            is = propertiesFile.exists() ?
                    new FileInputStream(propertiesFile) :
                    PropertiesLoader.class.getResourceAsStream(INNER_PATH);
            closer.register(is);
            if (is == null) {
                throw new FileNotFoundException(INNER_PATH);
            }
            props.load(is);
        }
        return props;
    }

    /**
     * Validate required properties presence
     *
     * @param properties Properties to be validated
     */
    private static void validateProperties(Properties properties) {
        for (ListenerProperty listenerProperty : values()) {
            if (listenerProperty.isRequired() && properties.getProperty(listenerProperty.getPropertyName()) == null) {
                throw new IllegalArgumentException(
                        "Property '" + listenerProperty.getPropertyName() + "' should not be null.");
            }
        }
    }

    /**
     * Overrides properties from another source
     *
     * @param source    Properties to be overridden
     * @param overrides Overrides
     */
    @VisibleForTesting
    static void overrideWith(Properties source, Map<String, String> overrides) {
        for (ListenerProperty listenerProperty : values()) {
            if (overrides.get(listenerProperty.getPropertyName()) != null) {
                source.setProperty(listenerProperty.getPropertyName(),
                        overrides.get(listenerProperty.getPropertyName()));
            }
        }
    }

    /**
     * Overrides properties from another source
     *
     * @param source    Properties to be overridden
     * @param overrides Overrides
     */
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    static void overrideWith(Properties source, Properties overrides) {
        overrideWith(source, ((Map) overrides));
    }

    private static Optional<URL> getResource(String resourceName) {
        ClassLoader loader = MoreObjects.firstNonNull(Thread.currentThread().getContextClassLoader(),
                PropertiesLoader.class.getClassLoader());
        return Optional.fromNullable(loader.getResource(resourceName));
    }

    private static void setProxyProperties(Properties properties) {
        for (String property : PROXY_PROPERTIES) {
            if (properties.containsKey(property)) {
                System.setProperty(property, properties.get(property).toString());
            }
        }
        final String userName = System.getProperty("http.proxyUser");
        final String password = System.getProperty("http.proxyPassword");
        if (userName != null && password != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password.toCharArray());
                }
            });
        }
    }
}
