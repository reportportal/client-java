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
package com.epam.reportportal.listeners;

import com.github.avarabyeu.restendpoint.http.exception.RestEndpointIOException;
import org.slf4j.Logger;

import com.epam.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.google.common.base.Throwables;

/**
 * Provide utilities for listeners
 * 
 * @author Aliaksei_Makayed
 * 
 */
public class ListenersUtils {

	private ListenersUtils() {
	}

	/**
	 * Handle exceptions in the listeners. log error in case of
	 * {@link ReportPortalException} or {@link com.github.avarabyeu.restendpoint.http.exception.RestEndpointIOException} or
	 * propagates exception exactly as-is, if and only if it is an instance of
	 * {@link RuntimeException} or {@link Error}.
	 * 
	 * @param exception
	 * @param logger
	 * @param message
	 */
	public static void handleException(Exception exception, Logger logger, String message) {
		if (ReportPortalException.class.isInstance(exception) || RestEndpointIOException.class.equals(exception.getClass())) {
			if (logger != null) {
				logger.error(message, exception);
			} else {
				System.out.println(exception.getMessage());
			}
		} else {
			Throwables.propagateIfPossible(exception);
		}
	}

	public static Mode getLaunchMode(String mode) {
		return Mode.isExists(mode) ? Mode.valueOf(mode.toUpperCase()) : Mode.DEFAULT;
	}

	public static Boolean getEnable(String enable) {
		return null == enable || Boolean.parseBoolean(enable);
	}

}
