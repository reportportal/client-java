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

package com.epam.reportportal.annotations.attribute;

import java.lang.annotation.*;

/**
 * Annotation used per se or in {@link Attributes} as field, to provide multiple
 * {@link com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ} with both 'key' and 'value' fields specified.
 * Used to prevent duplication of {@link Attribute} annotation with the same value and different keys.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MultiKeyAttributeGroup.class)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.CONSTRUCTOR })
public @interface MultiKeyAttribute {

	String[] keys();

	String value();
}
