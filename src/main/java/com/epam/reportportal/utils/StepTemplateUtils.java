/*
 *  Copyright 2022 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils;

import com.epam.reportportal.annotations.StepTemplateConfig;
import com.epam.reportportal.utils.templating.TemplateConfiguration;
import com.epam.reportportal.utils.templating.TemplateProcessing;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 * @deprecated Use {@link TemplateProcessing}
 */
@Deprecated
public class StepTemplateUtils {

	private StepTemplateUtils() {
		throw new IllegalStateException("Static only class");
	}

	/**
	 * Example:
	 * fields = {"object", "names", "hash"} from 'template part' = {object.names.hash}
	 * templateConfig - default
	 * <p>
	 * Given:
	 * object - some object
	 * names - the {@link java.util.List} containing 3 {@link String} objects with hashes: {25,32,57}
	 * hash - field, which value should be retrieved
	 * <p>
	 * Result:
	 * "[25,32,57]"
	 *
	 * @param templateConfig {@link StepTemplateConfig} for result formatting
	 * @param index          Index of the current field, from the template part
	 * @param fields         Fields of the template part
	 * @param object         Value of the current field
	 * @return {@link String} representation of object field(s) value(s).
	 * @throws NoSuchFieldException if field not found
	 * @deprecated Use {@link TemplateProcessing#retrieveValue(TemplateConfiguration, int, String[], Object)}
	 */
	@Deprecated
	public static String retrieveValue(StepTemplateConfig templateConfig, int index, String[] fields, Object object)
			throws NoSuchFieldException {
		return TemplateProcessing.retrieveValue(new TemplateConfiguration(templateConfig), index, fields, object);
	}
}
