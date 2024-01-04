/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.extensions.cdiportableextensions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Tests that beans defined in a static can be used by a deployment
 *
 * @author Stuart Douglas
 * @author Marek Schmidt
 */
public abstract class AbstractModuleTest {

    public static void doSetup(String modulePath, InputStream moduleXml, JavaArchive moduleArchive) throws Exception {
        File testModuleRoot = new File(getModulePath(), modulePath);
        deleteRecursively(testModuleRoot);
        createTestModule(testModuleRoot, moduleXml, moduleArchive);
    }

    public static void doCleanup(String modulePath) {
        File testModuleRoot = new File(getModulePath(), modulePath);
        deleteRecursively(testModuleRoot);
    }

    private static void deleteRecursively(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    deleteRecursively(new File(file, name));
                }
            }
            file.delete(); //this will always fail on windows, as module is still loaded and in use.
        }
    }

    private static void createTestModule(File testModuleRoot, InputStream moduleXml, JavaArchive moduleArchive) throws IOException {
        if (testModuleRoot.exists()) {
            throw new IllegalArgumentException(testModuleRoot + " already exists");
        }
        File file = new File(testModuleRoot, "main");
        if (!file.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + file);
        }

        copyFile(new File(file, "module.xml"), moduleXml);

        try (FileOutputStream jarFile = new FileOutputStream(new File(file, moduleArchive.getName()))){
            moduleArchive.as(ZipExporter.class).exportTo(jarFile);
        }

    }

    private static void copyFile(File target, InputStream src) throws IOException {
        Files.copy(src, target.toPath());
    }

    private static File getModulePath() {
        String modulePath = System.getProperty("module.path", null);
        if (modulePath == null) {
            String jbossHome = System.getProperty("jboss.home", null);
            if (jbossHome == null) {
                throw new IllegalStateException("Neither -Dmodule.path nor -Djboss.home were set");
            }
            modulePath = jbossHome + File.separatorChar + "modules";
        } else {
            modulePath = modulePath.split(File.pathSeparator)[0];
        }
        File moduleDir = new File(modulePath);
        if (!moduleDir.exists()) {
            throw new IllegalStateException("Determined module path does not exist");
        }
        if (!moduleDir.isDirectory()) {
            throw new IllegalStateException("Determined module path is not a dir");
        }
        return moduleDir;
    }
}
