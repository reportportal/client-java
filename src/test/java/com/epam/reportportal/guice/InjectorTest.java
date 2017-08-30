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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.github.avarabyeu.restendpoint.http.exception.RestEndpointIOException;
import com.google.inject.AbstractModule;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test base injector for report portal related stuff
 *
 * @author Andrei Varabyeu
 */
public class InjectorTest {

    @Test
    public void testOverrideJvmVar() throws RestEndpointIOException {
        System.setProperty("rp.extension", "com.epam.reportportal.guice.InjectorTest$OverrideModule");
        ListenerParameters params = Injector.create().getBean(ListenerParameters.class);
        Assert.assertThat("Incorrect mock!", params.getBaseUrl(), Matchers.equalTo("MOCK"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverrideJvmVarNegative() throws RestEndpointIOException {
        System.setProperty("rp.extension", "com.epam.reportportal.guice.InjectorTest");
        Injector.create().getBean(ReportPortal.class);
    }

    public static class OverrideModule extends AbstractModule {

        @Override
        protected void configure() {
            ListenerParameters mock = Mockito.mock(ListenerParameters.class);

            try {
                Mockito.<Object>when(mock.getBaseUrl()).
                        thenReturn("MOCK");
            } catch (RestEndpointIOException e) {
                e.printStackTrace();
            }

            binder().bind(ListenerParameters.class).toInstance(mock);
        }
    }
}
