/*
 * Copyright (C) 2018 EPAM Systems
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
package com.epam.reportportal.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

public class LaunchFileTest {
	@Test
	public void find() throws Exception {
		File tmpDir = LaunchFile.getTempDir();
		File f1 = new File(tmpDir, "rplaunch-xxxx-#1-11.tmp");
		f1.createNewFile();
		f1.deleteOnExit();

		File f2 = new File(tmpDir, "rplaunch-xxxx-#3-13.tmp");
		f2.createNewFile();
		f2.deleteOnExit();

		Assert.assertEquals("13", LaunchFile.find("xxxx").blockingGet());
	}

}