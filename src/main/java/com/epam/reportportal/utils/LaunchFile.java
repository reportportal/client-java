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

import com.google.common.base.StandardSystemProperty;
import io.reactivex.Maybe;
import io.reactivex.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Add Launch file
 *
 * @author Andrei Varabyeu
 */
public class LaunchFile {

    public static final String FILE_PREFIX = "rplaunch";

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchFile.class);

    private final File file;

    private LaunchFile(File file) {
        this.file = file;
    }

    public static Maybe<LaunchFile> create(Maybe<String> id) {
        final Maybe<LaunchFile> lfPromise = id.map(new Function<String, LaunchFile>() {
            @Override
            public LaunchFile apply(String launchId) throws Exception {
                LaunchFile lf = null;
                try {
                    final File file = new File(getTempDir(), String.format("%s-%s.tmp", FILE_PREFIX, launchId));
                    if (file.createNewFile()) {
                        file.deleteOnExit();
                        LOGGER.info("ReportPortal's temp file '{}' is created", file.getAbsolutePath());
                    }

                    lf = new LaunchFile(file);
                    return lf;
                } catch (Exception e) {
                    LOGGER.error("Cannot create ReportPortal launch file", e);
                    throw e;
                } finally {
                    Runtime.getRuntime().addShutdownHook(new Thread(new RemoveFileHook(lf)));
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
