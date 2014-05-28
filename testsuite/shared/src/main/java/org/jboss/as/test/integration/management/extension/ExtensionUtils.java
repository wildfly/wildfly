/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.extension;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.as.controller.Extension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.xnio.IoUtils;

/**
 * Utilities for manipulating extensions in integration tests.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ExtensionUtils {

    public static final String JAR_NAME = "test-extension.jar";

    public static void createExtensionModule(String extensionName, Class<? extends Extension> extension) throws IOException {
        createExtensionModule(getExtensionModuleRoot(extensionName), extension);
    }

    public static void createExtensionModule(String extensionName, Class<? extends Extension> extension, Package... additionalPackages) throws IOException {
        createExtensionModule(getExtensionModuleRoot(extensionName), extension, additionalPackages);
    }

    public static void createExtensionModule(File extensionModuleRoot, Class<? extends Extension> extension) throws IOException {
        createExtensionModule(extensionModuleRoot, extension, new Package[0]);
    }

    public static void createExtensionModule(File extensionModuleRoot, Class<? extends Extension> extension, Package... additionalPackages) throws IOException {

        deleteRecursively(extensionModuleRoot);

        if (extensionModuleRoot.exists()) {
            throw new IllegalArgumentException(extensionModuleRoot + " already exists");
        }
        File file = new File(extensionModuleRoot, "main");
        if (!file.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + file);
        }
        final InputStream is = createResourceRoot(extension, additionalPackages).exportAsInputStream();
        try {
            copyFile(new File(file, JAR_NAME), is);
        } finally {
            IoUtils.safeClose(is);
        }

        URL url = extension.getResource("module.xml");
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }
        copyFile(new File(file, "module.xml"), url.openStream());
    }

    public static void deleteExtensionModule(String moduleName) {
        deleteRecursively(getExtensionModuleRoot(moduleName));
    }

    public static void deleteExtensionModule(File extensionModuleRoot) {
        deleteRecursively(extensionModuleRoot);
    }

    private static void copyFile(File target, InputStream src) throws IOException {
        final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
        try {
            int i = src.read();
            while (i != -1) {
                out.write(i);
                i = src.read();
            }
        } finally {
            IoUtils.safeClose(out);
        }
    }

    public static File getModulePath() {
        String modulePath = System.getProperty("module.path", null);
        if (modulePath == null) {
            String jbossHome = System.getProperty("jboss.home", null);
            if (jbossHome == null) {
                throw new IllegalStateException("Neither -Dmodule.path nor -Djboss.home were set");
            }
            modulePath = jbossHome + File.separatorChar + "modules";
        }else{
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

    private static File getExtensionModuleRoot(String extensionName) {
        File file = getModulePath();
        for (String element : extensionName.split("\\.")) {
            file = new File(file, element);
        }
        return file;
    }

    private static StreamExporter createResourceRoot(Class<? extends Extension> extension, Package... additionalPackages) throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addPackage(extension.getPackage());
        if (additionalPackages != null) {
            for (Package pkg : additionalPackages) {
                archive.addPackage(pkg);
            }
        }
        archive.addAsServiceProvider(Extension.class, extension);
        return archive.as(ZipExporter.class);
    }

    private static void deleteRecursively(File file) {
        if (file == null) {
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRecursively(f);
            }
        }
        if (!file.delete()) {
            System.out.println("Could not delete file: " + file);
        }
    }
}
