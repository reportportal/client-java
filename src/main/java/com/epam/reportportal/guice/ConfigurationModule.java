/*
 * Copyright 2017 EPAM Systems
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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.utils.properties.PropertiesLoader;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * Configuration Module
 *
 * @author Andrei Varabyeu
 */
public class ConfigurationModule implements Module {
    @Override
    public void configure(Binder binder) {
        final PropertiesLoader properties = PropertiesLoader.load();
        Names.bindProperties(binder, properties.getProperties());
        for (final ListenerProperty listenerProperty : ListenerProperty.values()) {
            binder.bind(Key.get(String.class, ListenerPropertyBinder.named(listenerProperty)))
                    .toProvider(new Provider<String>() {
                        @Override
                        public String get() {
                            return properties.getProperty(listenerProperty.getPropertyName());
                        }
                    });
        }

        binder.bind(PropertiesLoader.class).toInstance(properties);
    }

    /**
     * Provides wrapper for report portal properties
     */
    @Provides
    @Singleton
    public ListenerParameters provideListenerProperties(PropertiesLoader propertiesLoader) {
        return new ListenerParameters(propertiesLoader);
    }
}
