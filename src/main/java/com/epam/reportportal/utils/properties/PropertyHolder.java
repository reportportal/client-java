/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.utils.properties;

/**
 * Interface for containers with meta information that should be sent to the Report Portal instance via {@link com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ}
 * using {@link SystemAttributesExtractor}
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public interface PropertyHolder {

	String getName();

	String[] getPropertyKeys();

	boolean isInternal();
}
