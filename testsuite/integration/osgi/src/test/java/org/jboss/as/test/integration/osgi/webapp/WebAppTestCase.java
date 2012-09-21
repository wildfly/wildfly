/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.api.Echo;
import org.jboss.as.test.integration.osgi.webapp.bundle.SimpleServlet;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test webapp deployemnts as OSGi bundles
 *
 * @author thomas.diesler@jboss.com
 *
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
public class WebAppTestCase {

    static final String SIMPLE_WAR = "simple.war";
    static final String BUNDLE_A_WAR = "bundle-a.war";
    static final String BUNDLE_B_WAR = "bundle-b.war";
    static final String BUNDLE_C_WAB = "bundle-c.wab";
    static final String BUNDLE_D_WAB = "bundle-d.wab";
    static final String BUNDLE_E_JAR = "bundle-e.jar";

    static final Asset STRING_ASSET = new StringAsset("Hello from Resource");

    @Inject
    public PackageAdmin packageAdmin;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-webapp-test");
        jar.addClasses(HttpRequest.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Deployment(name = SIMPLE_WAR, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsWebResource(STRING_ASSET, "message.txt");
        return archive;
    }

    @Deployment(name = BUNDLE_A_WAR, testable = false)
    public static Archive<?> getSimpleWarAsBundle() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, BUNDLE_A_WAR);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsWebResource(STRING_ASSET, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addBundleClasspath("WEB-INF/classes");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_B_WAR, testable = false)
    public static Archive<?> getWebAppBundleDeploymentA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B_WAR);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(STRING_ASSET, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_C_WAB, testable = false)
    public static Archive<?> getBundleWithWabExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_C_WAB);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(STRING_ASSET, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_D_WAB, testable = false)
    public static Archive<?> getBundleWithWebContextPath() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_D_WAB);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(STRING_ASSET, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/bundle-d");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_E_JAR, testable = false)
    public static Archive<?> getBundleWithJarExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_E_JAR);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(STRING_ASSET, "message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/bundle-e");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testWarDeployment() throws Exception {
        String result = performCall("/simple/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("/simple/message.txt");
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    public void testWarStructureDeployment() throws Exception {
        String result = performCall("/bundle-a/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("/bundle-a/message.txt");
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    public void testOSGiStructureDeployment() throws Exception {
        String result = performCall("/bundle-b/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("/bundle-b/message.txt");
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    public void testSimpleBundleWithWabExtension() throws Exception {
        String result = performCall("/bundle-c/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("/bundle-c/message.txt");
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    public void testBundleWithWebContextPath() throws Exception {

        Bundle bundle = packageAdmin.getBundles(BUNDLE_D_WAB, null)[0];

        String result = performCall("/bundle-d/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);

        result = performCall("/bundle-d/message.txt");
        Assert.assertEquals("Hello from Resource", result);

        bundle.stop();
        Assert.assertEquals("RESOLVED", Bundle.RESOLVED, bundle.getState());

        try {
            performCall("/bundle-d/servlet?input=Hello");
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }

        try {
            performCall("/bundle-d/message.txt");
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }

        bundle.start();
        Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

        result = performCall("/bundle-d/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);

        result = performCall("/bundle-d/message.txt");
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    public void testSimpleBundleWithJarExtension() throws Exception {
        String result = performCall("/bundle-e/servlet?input=Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        result = performCall("/bundle-e/message.txt");
        Assert.assertEquals("Hello from Resource", result);
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 5, TimeUnit.SECONDS);
    }
}
