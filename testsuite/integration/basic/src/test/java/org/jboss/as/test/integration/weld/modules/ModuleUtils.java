/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.modules;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.Extension;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.xnio.IoUtils;

public class ModuleUtils {

    public static void createSimpleTestModule(String moduleName, Class<?>... classes) throws IOException {
        createTestModule(moduleName, createSimpleModuleDescriptor(moduleName).openStream(), classes);
    }

    public static void createTestModule(String moduleName, String moduleXml, Class<?>... classes) throws IOException {
        URL url = classes[0].getResource(moduleXml);
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml: " + moduleXml);
        }
        createTestModule(moduleName, url.openStream(), classes);
    }

    private static void createTestModule(String moduleName, InputStream moduleXml, Class<?>... classes) throws IOException {
        File testModuleRoot = new File(getModulePath(), "test" + File.separatorChar + moduleName);
        if (testModuleRoot.exists()) {
            throw new IllegalArgumentException(testModuleRoot + " already exists");
        }
        File file = new File(testModuleRoot, "main");
        if (!file.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + file);
        }

        copyFile(new File(file, "module.xml"), moduleXml);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, moduleName + ".jar");
        jar.addClasses(classes);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        addExtensionsIfAvailable(jar, classes);

        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (final InputStream resource = clazz.getResourceAsStream(clazz.getSimpleName() + ".class")) {
                indexer.index(resource);
            }
        }

        Index index = indexer.complete();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        IndexWriter writer = new IndexWriter(data);
        writer.write(index);
        jar.addAsManifestResource(new ByteArrayAsset(data.toByteArray()), "jandex.idx");
        FileOutputStream jarFile = new FileOutputStream(new File(file, moduleName + ".jar"));
        try {
            jar.as(ZipExporter.class).exportTo(jarFile);
        } finally {
            jarFile.flush();
            jarFile.close();
        }

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

    public static void deleteRecursively(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    deleteRecursively(new File(file, name));
                }
            }
            file.delete();
        }
    }

    private static Asset createSimpleModuleDescriptor(String moduleName) {
        return new StringAsset(
                "<module xmlns=\"urn:jboss:module:1.1\" name=\"test." + moduleName + "\">" +
                "<resources>" +
                "<resource-root path=\"" + moduleName + ".jar\"/>" +
                "</resources>" +
                "<dependencies>" +
                "<module name=\"javax.enterprise.api\"/>" +
                "<module name=\"javax.inject.api\"/>" +
                "</dependencies>" +
                "</module>");
    }

    /**
     * Adds extensions to the specified archive if any available.
     *
     * @param jar to add extensions to
     * @param classes to be evaluated
     */
    @SuppressWarnings("unchecked")
    private static void addExtensionsIfAvailable(JavaArchive jar, final Class<?>... classes) {
        List<Class<Extension>> extensions = new ArrayList<>(1);
        for (Class<?> clazz : classes) {
            if (Extension.class.isAssignableFrom(clazz)) {
                extensions.add((Class<Extension>) clazz);
            }
        }

        if (!extensions.isEmpty()) {
            Class<Extension>[] a = (Class<Extension>[]) Array.newInstance(Extension.class.getClass(), 0);
            jar.addAsServiceProvider(Extension.class, extensions.toArray(a));
        }
    }
}
