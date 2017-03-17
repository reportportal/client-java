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
package com.epam.reportportal.exception;

import com.epam.ta.reportportal.ws.model.ErrorRS;

/**
 * ReportPortal Client Exception. Something goes wrong in client side
 * 
 * @author Andrei Varabyeu
 * 
 */
public class ReportPortalClientException extends ReportPortalException {

	private static final long serialVersionUID = -7286774672064765468L;

	public ReportPortalClientException(int statusCode, String statusMessage, ErrorRS errorContent) {
		super(statusCode, statusMessage, errorContent);
	}

}
