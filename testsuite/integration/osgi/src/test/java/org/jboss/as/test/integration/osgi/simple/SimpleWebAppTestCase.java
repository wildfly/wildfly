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
package org.jboss.as.test.integration.osgi.simple;

import static org.jboss.as.test.osgi.OSGiManagementOperations.bundleStart;
import static org.jboss.as.test.osgi.OSGiManagementOperations.bundleStop;
import static org.jboss.as.test.osgi.OSGiManagementOperations.getBundleInfo;
import static org.jboss.as.test.osgi.OSGiManagementOperations.getBundleState;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.osgi.parser.ModelConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.simple.bundleA.SimpleServlet;
import org.jboss.as.test.integration.osgi.simple.bundleB.Echo;
import org.jboss.dmr.ModelNode;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test webapp deployemnts as OSGi bundles
 *
 * @author thomas.diesler@jboss.com
 *
 * @since 07-Jun-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SimpleWebAppTestCase {

    static final String SIMPLE_WAR = "simple.war";
    static final String BUNDLE_A_WAR = "bundle-a.war";
    static final String BUNDLE_B_WAR = "bundle-b.war";
    static final String BUNDLE_C_WAB = "bundle-c.wab";
    static final String BUNDLE_D_WAB = "bundle-d.wab";

    static final Asset STRING_ASSET = new StringAsset("Hello from Resource");

    @ArquillianResource
    URL targetURL;

    @ArquillianResource
    ManagementClient managementClient;

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
                builder.addManifestHeader("Web-ContextPath",  "/osgi-webapp");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @OperateOnDeployment(SIMPLE_WAR)
    public void testWarDeployment() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("message.txt", null);
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    @OperateOnDeployment(BUNDLE_A_WAR)
    public void testWarStructureDeployment() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("message.txt", null);
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    @OperateOnDeployment(BUNDLE_B_WAR)
    public void testOSGiStructureDeployment() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("message.txt", null);
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    @OperateOnDeployment(BUNDLE_C_WAB)
    public void testSimpleBundleWithWabExtension() throws Exception {
        String result = performCall("bundle-c", "simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
        // Test resource access
        result = performCall("bundle-c", "message.txt", null);
        Assert.assertEquals("Hello from Resource", result);
    }

    @Test
    @OperateOnDeployment(BUNDLE_D_WAB)
    public void testBundleWithWebContextPath() throws Exception {

        ModelNode info = getBundleInfo(getControllerClient(), BUNDLE_D_WAB);
        Assert.assertEquals("ACTIVE", info.get(ModelConstants.STATE).asString());
        Assert.assertEquals(BUNDLE_D_WAB, info.get(ModelConstants.SYMBOLIC_NAME).asString());

        String result = performCall("osgi-webapp", "simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);

        result = performCall("osgi-webapp", "message.txt", null);
        Assert.assertEquals("Hello from Resource", result);

        Assert.assertTrue("Bundle stopped", bundleStop(getControllerClient(), BUNDLE_D_WAB));
        Assert.assertEquals("RESOLVED", getBundleState(getControllerClient(), BUNDLE_D_WAB));

        try {
            performCall("osgi-webapp", "simple", "Hello");
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }

        try {
            performCall("osgi-webapp", "message.txt", null);
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }

        Assert.assertTrue("Bundle started", bundleStart(getControllerClient(), BUNDLE_D_WAB));
        Assert.assertEquals("ACTIVE", getBundleState(getControllerClient(), BUNDLE_D_WAB));

        result = performCall("osgi-webapp", "simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);

        result = performCall("osgi-webapp", "message.txt", null);
        Assert.assertEquals("Hello from Resource", result);
    }

    private String performCall(String pattern, String param) throws Exception {
        return performCall(null,  pattern, param);
    }

    private String performCall(String context, String pattern, String param) throws Exception {
        String urlspec = targetURL.toExternalForm();
        if (targetURL.getPath().isEmpty() && context != null) {
            urlspec += "/" + context + "/";
        }
        URL url = new URL(urlspec + pattern + (param != null ? "?input=" + param : ""));
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }

    private ModelControllerClient getControllerClient() {
        return managementClient.getControllerClient();
    }
}
