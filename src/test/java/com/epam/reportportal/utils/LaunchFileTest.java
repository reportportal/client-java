package com.epam.reportportal.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

public class LaunchFileTest {
	@Test
	public void find() throws Exception {
		File tmpDir = LaunchFile.getTempDir();
		File f1 = new File(tmpDir, "rplaunch-xxxx-#1-id11.tmp");
		f1.createNewFile();
		f1.deleteOnExit();

		File f2 = new File(tmpDir, "rplaunch-xxxx-#3-id13.tmp");
		f2.createNewFile();
		f2.deleteOnExit();

		Assert.assertEquals("id13", LaunchFile.find("xxxx").blockingGet());
	}

}