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

package org.jboss.as.test.integration.weld.multideployment;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * Tests that beans definied in a static can be used by a deployment
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class WeldModuleDeploymentTestCase {

    public static void doSetup() throws Exception {
        File testModuleRoot = new File(getModulePath(), "org/jboss/test/weldModule");
        File file = testModuleRoot;
        while (!getModulePath().equals(file.getParentFile()))
            file = file.getParentFile();
        deleteRecursively(file);
        createTestModule(testModuleRoot);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        File testModuleRoot = new File(getModulePath(), "org/jboss/test/weldModule");
        File file = testModuleRoot;
        while (!getModulePath().equals(file.getParentFile()))
            file = file.getParentFile();
        deleteRecursively(file);
    }

    private static void deleteRecursively(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (String name : file.list()) {
                    deleteRecursively(new File(file, name));
                }
            }
            file.delete();
        }
    }

    private static void createTestModule(File testModuleRoot) throws IOException {
        if (testModuleRoot.exists()) {
            throw new IllegalArgumentException(testModuleRoot + " already exists");
        }
        File file = new File(testModuleRoot, "main");
        if (!file.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + file);
        }

        URL url = WeldModuleDeploymentTestCase.class.getResource("module.xml");
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }
        copyFile(new File(file, "module.xml"), url.openStream());

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "weldTest.jar");
        jar.addClasses(SimpleBean.class, ModuleEjb.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");


        Indexer indexer = new Indexer();
        try (InputStream resource = ModuleEjb.class.getResourceAsStream(ModuleEjb.class.getSimpleName() + ".class")) {
            indexer.index(resource);
        }
        Index index = indexer.complete();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        IndexWriter writer = new IndexWriter(data);
        writer.write(index);
        jar.addAsManifestResource(new ByteArrayAsset(data.toByteArray()), "jandex.idx");
        FileOutputStream jarFile = new FileOutputStream(new File(file, "weldTest.jar"));
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


    @Deployment
    public static Archive<?> deploy2() throws Exception {
        doSetup();
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "d2.jar");
        jar.addClasses(WeldModuleDeploymentTestCase.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.test.weldModule meta-inf annotations\n"), "MANIFEST.MF");
        return jar;
    }

    @Inject
    private SimpleBean bean;

    @Inject
    private ModuleEjb moduleEjb;

    @Test
    public void testSimpleBeanInjected() throws Exception {
        Assert.assertNotNull(bean);
        Assert.assertNotNull(moduleEjb);
    }
}
