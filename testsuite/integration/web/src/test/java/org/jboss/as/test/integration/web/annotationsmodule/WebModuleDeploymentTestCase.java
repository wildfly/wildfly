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

package org.jboss.as.test.integration.web.annotationsmodule;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Tests that servlets defined by annodations in a static module are picked up
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebModuleDeploymentTestCase {

    public static void doSetup() throws Exception {
        File testModuleRoot = new File(getModulePath(), "org/jboss/test/webModule");
        File file = testModuleRoot;
        while (!getModulePath().equals(file.getParentFile()))
            file = file.getParentFile();
        deleteRecursively(file);
        createTestModule(testModuleRoot);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        File testModuleRoot = new File(getModulePath(), "org/jboss/test/webModule");
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

        URL url = WebModuleDeploymentTestCase.class.getResource("module.xml");
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }
        copyFile(new File(file, "module.xml"), url.openStream());

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "webTest.jar");
        jar.addClasses(ModuleServlet.class);


        Indexer indexer = new Indexer();
        try (InputStream resource = ModuleServlet.class.getResourceAsStream(ModuleServlet.class.getSimpleName() + ".class")) {
            indexer.index(resource);
        }
        Index index = indexer.complete();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        IndexWriter writer = new IndexWriter(data);
        writer.write(index);
        jar.addAsManifestResource(new ByteArrayAsset(data.toByteArray()), "jandex.idx");
        FileOutputStream jarFile = new FileOutputStream(new File(file, "webTest.jar"));
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

    @ArquillianResource
    protected URL url;

    @Deployment
    public static Archive<?> deploy2() throws Exception {
        doSetup();
        WebArchive jar = ShrinkWrap.create(WebArchive.class, "webAnnotation.war");
        jar.addClasses(WebModuleDeploymentTestCase.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.test.webModule meta-inf annotations\n"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void testSimpleBeanInjected() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "/servlet");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            Assert.assertEquals(ModuleServlet.MODULE_SERVLET, result);
        }
    }
}
