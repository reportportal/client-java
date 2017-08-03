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
package com.epam.reportportal.utils.files;

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.utils.MimeTypeDetector;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Andrei Varabyeu
 */
public class ImageConverterTest {
    @Test
    public void isImage() throws Exception {
        final String resourceName = "defaultUserPhoto.jpg";
        final ByteSource byteSource = Resources.asByteSource(Resources.getResource(resourceName));
        boolean r = ImageConverter
                .isImage(new TypeAwareByteSource(byteSource, MimeTypeDetector.detect(byteSource, resourceName)));
        Assert.assertTrue("Incorrect image type detection", r);
    }

}
