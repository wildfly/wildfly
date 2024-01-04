/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.vdx.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileUtils {

    public static void copyFileFromResourcesToServer(String resourceFile, Path targetDirectory, boolean override) throws Exception {
        if (resourceFile == null || "".equals(resourceFile)) {
            return;
        }

        Path sourcePath = Paths.get(ClassLoader.getSystemResource(resourceFile).toURI());
        Path targetPath = Paths.get(targetDirectory.toString(), sourcePath.getFileName().toString());

        if (override || Files.notExists(targetPath)) {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }


}
