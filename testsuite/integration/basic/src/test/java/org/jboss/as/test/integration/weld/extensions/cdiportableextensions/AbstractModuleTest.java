/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
