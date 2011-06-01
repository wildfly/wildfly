/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.integration.osgi.http;

import static org.jboss.osgi.http.HttpServiceCapability.DEFAULT_HTTP_SERVICE_PORT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.osgi.http.bundle.EndpointServlet;
import org.jboss.as.testsuite.integration.osgi.http.bundle.HttpExampleActivator;
import org.jboss.osgi.http.HttpServiceCapability;
import org.jboss.osgi.logging.LogServiceTracker;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A test that deployes a bundle that containes a HttpServlet which is registered through the OSGi HttpService
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Jan-2009
 */
@RunWith(Arquillian.class)
public class HttpServiceTestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-http");
        archive.addClasses(HttpExampleActivator.class, EndpointServlet.class);
        archive.addAsResource("osgi/http/message.txt", "res/message.txt");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(HttpExampleActivator.class);
                builder.addImportPackages(HttpService.class, LogService.class, BundleActivator.class, ServiceTracker.class);
                builder.addImportPackages(HttpServiceCapability.class, LogServiceTracker.class);
                builder.addImportPackages(HttpServlet.class, Servlet.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testServletAccess() throws Exception {
        bundle.start();
        String line = getHttpResponse("/servlet?test=plain", 5000);
        assertEquals("Hello from Servlet", line);
    }

    @Test
    public void testServletInitProps() throws Exception {
        bundle.start();
        String line = getHttpResponse("/servlet?test=initProp", 5000);
        assertEquals("initProp=SomeValue", line);
    }

    @Test
    public void testServletBundleContext() throws Exception {
        bundle.start();
        String line = getHttpResponse("/servlet?test=context", 5000);
        assertEquals("example-http", line);
    }

    @Test
    public void testResourceAccess() throws Exception {
        bundle.start();
        String line = getHttpResponse("/file/message.txt", 5000);
        assertEquals("Hello from Resource", line);
    }

    private String getHttpResponse(String reqPath, int timeout) throws IOException {
        return HttpServiceCapability.getHttpResponse("localhost", DEFAULT_HTTP_SERVICE_PORT, reqPath, timeout);
    }
}