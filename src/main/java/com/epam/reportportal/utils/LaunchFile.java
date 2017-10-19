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
package com.epam.reportportal.utils;

import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.Lists;
import io.reactivex.Maybe;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Add Launch file
 *
 * @author Andrei Varabyeu
 */
public class LaunchFile {

	public static final String FILE_PREFIX = "rplaunch";

	private static final Pattern FILENAME_PATTERN = Pattern.compile("rplaunch-(.*)-#(\\d)+-(.*)\\.tmp");

	private static final Logger LOGGER = LoggerFactory.getLogger(LaunchFile.class);

	private final File file;

	private LaunchFile(File file) {
		this.file = file;
	}

	public static Maybe<String> find(final String name) {
		File tempDir = getTempDir();
		final String prefix = FILE_PREFIX + "-" + name;
		List<String> files = Arrays.asList(tempDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix) && FILENAME_PATTERN.matcher(name).matches();
			}
		}));

		List<StartLaunchRS> fileRSs = new ArrayList(Lists.transform(files, new com.google.common.base.Function<String, StartLaunchRS>() {
			@Override
			public StartLaunchRS apply(String input) {
				Matcher m = FILENAME_PATTERN.matcher(input);
				if (m.find()) {
					return new StartLaunchRS(m.group(3), Long.parseLong(m.group(2)));
				}
				throw new RuntimeException("Does not match:" + input);
			}
		}));
		Collections.sort(fileRSs, new Comparator<StartLaunchRS>() {
			@Override
			public int compare(StartLaunchRS o1, StartLaunchRS o2) {
				return -1 * o1.getNumber().compareTo(o2.getNumber());
			}
		});
		return Maybe.just(fileRSs.get(0).getId());
	}

	public static Maybe<LaunchFile> create(final String name, Maybe<StartLaunchRS> id) {
		final Maybe<LaunchFile> lfPromise = id.map(new Function<StartLaunchRS, LaunchFile>() {
			@Override
			public LaunchFile apply(StartLaunchRS launchId) throws Exception {
				try {
					final File file = new File(getTempDir(),
							String.format("%s-%s-#%d-%s.tmp", FILE_PREFIX, name, launchId.getNumber(), launchId.getId())
					);
					if (file.createNewFile()) {
						LOGGER.info("ReportPortal's temp file '{}' is created", file.getAbsolutePath());
					}

					return new LaunchFile(file);
				} catch (Exception e) {
					LOGGER.error("Cannot create ReportPortal launch file", e);
					throw e;
				}
			}
		}).cache().onErrorReturnItem(new LaunchFile(null));
		lfPromise.subscribe();
		return lfPromise;
	}

	public File getFile() {
		return file;
	}

	public void remove() {
		if (null != file && file.exists() && file.delete()) {
			LOGGER.info("ReportPortal's temp file '{}' has been removed", file.getAbsolutePath());
		}
	}

	public static File getTempDir() {
		File tempDir = new File(StandardSystemProperty.JAVA_IO_TMPDIR.value(), "reportportal");
		if (tempDir.mkdirs()) {
			LOGGER.info("Temp directory for ReportPortal launch files is created: '{}'", tempDir.getAbsolutePath());
		}

		return tempDir;
	}

	private static class RemoveFileHook implements Runnable {
		private final LaunchFile file;

		private RemoveFileHook(LaunchFile file) {
			this.file = file;
		}

		@Override
		public void run() {
			if (null != file) {
				file.remove();
			}
		}
	}
}
