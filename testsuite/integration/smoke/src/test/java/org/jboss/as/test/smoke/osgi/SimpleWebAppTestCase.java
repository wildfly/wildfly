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
package org.jboss.as.test.smoke.osgi;

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
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.smoke.osgi.bundle.SimpleServlet;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test webapp deployemnts as OSGi bundles
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SimpleWebAppTestCase {

    private static final String SIMPLE_WAR = "war-example.war";
    private static final String WAR_STRUCTURE_BUNDLE = "war-structure-bundle.war";
    private static final String OSGI_STRUCTURE_BUNDLE = "osgi-structure-bundle.war";
    private static final String WEB_APPLICATION_BUNDLE_A = "osgi-webapp-a.wab";
    private static final String WEB_APPLICATION_BUNDLE_B = "osgi-webapp-b.wab";

    @ArquillianResource
    private URL urlres;

    @Deployment(name = SIMPLE_WAR, testable = false)
    public static Archive<?> getWarDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(SimpleServlet.class);
        return archive;
    }

    @Deployment(name = WAR_STRUCTURE_BUNDLE, testable = false)
    public static Archive<?> getWarStructureDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, WAR_STRUCTURE_BUNDLE);
        archive.addClasses(SimpleServlet.class);
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

    @Deployment(name = OSGI_STRUCTURE_BUNDLE, testable = false)
    public static Archive<?> getOSGiStructureDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, OSGI_STRUCTURE_BUNDLE);
        archive.addClasses(SimpleServlet.class);
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

    @Deployment(name = WEB_APPLICATION_BUNDLE_A, testable = false)
    public static Archive<?> getWebAppBundleDeploymentA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, WEB_APPLICATION_BUNDLE_A);
        archive.addClasses(SimpleServlet.class);
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

    @Deployment(name = WEB_APPLICATION_BUNDLE_B, testable = false)
    public static Archive<?> getWebAppBundleDeploymentB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, WEB_APPLICATION_BUNDLE_B);
        archive.addClasses(SimpleServlet.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PostConstruct.class, WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader("Web-ContextPath", "/osgi-webapp");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @OperateOnDeployment(SIMPLE_WAR)
    public void testSimpleWar() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    @OperateOnDeployment(WAR_STRUCTURE_BUNDLE)
    public void testWarStructureBundle() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    @OperateOnDeployment(OSGI_STRUCTURE_BUNDLE)
    public void testOSGiStructureBundle() throws Exception {
        String result = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    @OperateOnDeployment(WEB_APPLICATION_BUNDLE_A)
    public void testWebApplicationBundleA() throws Exception {
        String result = performCall("osgi-webapp-a", "simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    @Test
    @OperateOnDeployment(WEB_APPLICATION_BUNDLE_B)
    public void testWebApplicationBundleB() throws Exception {
        String result = performCall("osgi-webapp", "simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", result);
    }

    private String performCall(String pattern, String param) throws Exception {
        return performCall(null,  pattern, param);
    }

    private String performCall(String context, String pattern, String param) throws Exception {
        String urlspec = urlres.toExternalForm();
        if (urlres.getPath().isEmpty() && context != null) {
            urlspec += "/" + context + "/";
        }
        URL url = new URL(urlspec + pattern + "?input=" + param);
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
