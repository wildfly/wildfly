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

import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.xnio.IoUtils;

import javax.enterprise.inject.spi.Extension;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ModuleUtils {

    public static TestModule createSimpleTestModule(String moduleName, Class<?>... classes) throws IOException {
        return createTestModule(moduleName, createSimpleModuleDescriptor(moduleName).openStream(), classes);
    }

    public static TestModule createTestModule(String moduleName, String moduleXml, Class<?>... classes) throws IOException {
        URL url = classes[0].getResource(moduleXml);

        if (url == null) {
            throw new IllegalStateException("Could not find module.xml: " + moduleXml);
        }

        return createTestModule(moduleName, url.openStream(), classes);
    }

    private static TestModule createTestModule(String moduleName, InputStream moduleXml, Class<?>... classes) throws IOException {
        File tempFile = File.createTempFile("test_module_tmp", ".tmp");

        copyFile(tempFile, moduleXml);

        TestModule testModule = new TestModule("test." + moduleName, tempFile);
        JavaArchive moduleJar = testModule
            .addResource(moduleName + ".jar")
            .addClasses(classes)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        addExtensionsIfAvailable(moduleJar, classes);

        testModule.create();

        return testModule;
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
