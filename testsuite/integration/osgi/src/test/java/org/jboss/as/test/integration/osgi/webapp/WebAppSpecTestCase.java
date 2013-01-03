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

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.webapp.bundle.TestServletContext;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test OSGi webapp functionality
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 *
 * @since 07-Jun-2011
 */
@RunWith(Arquillian.class)
public class WebAppSpecTestCase {

    static final String BUNDLE_A_WAB = "bundle-a.wab";

    static final Asset STRING_ASSET = new StringAsset("Hello from Resource");

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment(name = BUNDLE_A_WAB, testable = false)
    public static Archive<?> getWebAppBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A_WAB);
        archive.addClass(TestServletContext.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebServlet.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addImportPackages(BundleContext.class, ServiceTracker.class);
                builder.addManifestHeader("Web-ContextPath",  "/testcontext");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testServletContextService() throws Exception {
        String result = performCall("/testcontext/testservletcontext");
        Assert.assertEquals("ServletContext: bundle-a.wab|/testcontext", result);
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 5, TimeUnit.SECONDS);
    }
}
