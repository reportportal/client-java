/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/client-core
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
package com.epam.reportportal.guice;

import com.epam.reportportal.exception.InternalReportPortalClientException;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * ReportPortal's Injector
 *
 * @author Andrei Varabyeu
 */
public class Injector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Injector.class);

    /* extension class can be provided as JVM or ENV variable */
    public static final String RP_EXTENSION_PROPERTY_NAME = "rp.extension";

    /**
     * Creates Injector with default modules added
     *
     * @return created Injector
     */
    public static Injector createDefault() {
        return create(defaultModules());
    }

    /**
     * Creates Injector based on default modules combining with provided extension modules
     *
     * @param extensions Modules to add to context
     * @return Created injector
     */
    public static Injector createDefault(Module... extensions) {
        return create(
                Modules.combine(ImmutableSet.<Module>builder()
                        .add(defaultModules())
                        .add(extensions)
                        .build()));
    }

    /**
     * Creates injector based on provided modules
     *
     * @param modules Modules to put in injector
     * @return
     */
    public static Injector create(Module... modules) {
        return new Injector(modules);
    }

    /**
     * @return Set of default modules used to initialize the context
     */
    public static Module[] defaultModules() {
        return new Module[] { new ConfigurationModule(), new ReportPortalClientModule() };
    }

    /**
     * Creates default report portal injector
     */
    private Injector(Module... modules) {
        String extensions = System.getProperty(RP_EXTENSION_PROPERTY_NAME, System.getenv(RP_EXTENSION_PROPERTY_NAME));
        if (!Strings.isNullOrEmpty(extensions)) {
            List<Module> extensionModules = buildExtensions(extensions);
            this.injector = Guice.createInjector(Modules.override(modules).with(extensionModules));
        } else {
            this.injector = Guice.createInjector(modules);
        }
    }

    /**
     * Guice BaseInjector
     */
    private com.google.inject.Injector injector;

    /**
     * Returns bean of provided type
     *
     * @param type Type of Bean
     */
    public <T> T getBean(Class<T> type) {
        return injector.getInstance(type);
    }

    /**
     * Returns bean by provided key
     *
     * @param key Bean Key
     */
    public <T> T getBean(Key<T> key) {
        return injector.getInstance(key);
    }

    /**
     * Returns bind property
     *
     * @param key Property type
     * @see ListenerProperty
     */
    public String getProperty(ListenerProperty key) {
        return injector.getInstance(Key.get(String.class, new ListenerPropertyValueImpl(key)));
    }

    /**
     * Injects members into provided instance
     *
     * @param object Instance members of need to be injected
     */
    public void injectMembers(Object object) {
        this.injector.injectMembers(object);
    }

    /**
     * Builds extensions based on environment variable
     *
     * @param extensions Command-separated list of extension module classes
     * @return List of Guice's modules
     */
    @SuppressWarnings("unchecked")
    private List<Module> buildExtensions(String extensions) {
        List<String> extensionClasses = Splitter.on(",").splitToList(extensions);
        List<Module> extensionModules = new ArrayList<Module>(extensionClasses.size());
        for (String extensionClass : extensionClasses) {
            try {
                Class<?> extensionClassObj = Class.forName(extensionClass);
                Preconditions.checkArgument(Module.class.isAssignableFrom(extensionClassObj),
                        "Extension class '%s' is not an Guice's Module", extensionClass);
                Class<Module> extension = (Class<Module>) extensionClassObj;
                extensionModules.add(extension.getConstructor(new Class[] {}).newInstance());
            } catch (ClassNotFoundException e) {
                String errorMessage = "Extension class with name '" + extensionClass + "' not found";
                LOGGER.error(errorMessage);
                throw new InternalReportPortalClientException(errorMessage, e);

            } catch (Exception e) {
                String errorMessage =
                        "Unable to create instance of '" + extensionClass + "'. Does it have empty constructor?";
                LOGGER.error(errorMessage);
                throw new InternalReportPortalClientException(errorMessage, e);
            }
        }
        return extensionModules;
    }
}
