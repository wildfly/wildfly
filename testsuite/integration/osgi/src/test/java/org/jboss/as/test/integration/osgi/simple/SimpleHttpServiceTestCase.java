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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.logging.Logger;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Test the {@link HttpService} on JBossWeb
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Jul-2011
 */
@RunWith(Arquillian.class)
public class SimpleHttpServiceTestCase {

    static StringAsset STRING_ASSET = new StringAsset("Hello from Resource");

    @Inject
    public Bundle bundle;

    @Deployment
    public static Archive<?> getDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "http-service-example");
        archive.addClasses(HttpRequest.class);
        archive.addAsResource(STRING_ASSET, "res/message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(HttpService.class);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testServletAccess() throws Exception {
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        String reqspec = "/servlet?test=param&param=Kermit";
        try {
            HttpService httpService = (HttpService) context.getService(sref);

            // Verify that the alias is not yet available
            assertNotAvailable(reqspec);

            // Register the test servlet and make a call
            httpService.registerServlet("/servlet", new HttpServiceServlet(bundle), null, null);
            Assert.assertEquals("Hello: Kermit", performCall(reqspec));

            // Unregister the servlet alias
            httpService.unregister("/servlet");

            assertNotAvailable(reqspec);

        } finally {
            context.ungetService(sref);
        }
    }

    @Test
    public void testResourceAccess() throws Exception {
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        String reqspec = "/resource/message.txt";
        try {
            HttpService httpService = (HttpService) context.getService(sref);

            // Verify that the alias is not yet available
            assertNotAvailable(reqspec);

            // Register the test servlet and make a call
            httpService.registerResources("/resource", "/res", null);
            Assert.assertEquals("Hello from Resource", performCall(reqspec));

            // Unregister the servlet alias
            httpService.unregister("/resource");

            // Verify that the alias is not available any more
            assertNotAvailable(reqspec);
        } finally {
            context.ungetService(sref);
        }
    }

    @Test
    public void testServletInitProps() throws Exception {
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        String reqspec = "/servlet?test=init&init=someKey";
        try {
            HttpService httpService = (HttpService) context.getService(sref);

            // Verify that the alias is not yet available
            assertNotAvailable(reqspec);

            Properties initParams = new Properties();
            initParams.setProperty("someKey", "someValue");

            // Register the test servlet and make a call
            httpService.registerServlet("/servlet", new HttpServiceServlet(bundle), initParams, null);
            Assert.assertEquals("someKey=someValue", performCall(reqspec));
        } finally {
            context.ungetService(sref);
        }

        assertNotAvailable(reqspec);
    }

    @Test
    public void testServletInstance() throws Exception {
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        String reqspec = "/servlet?test=instance";
        try {
            HttpService httpService = (HttpService) context.getService(sref);

            // Verify that the alias is not yet available
            assertNotAvailable(reqspec);

            // Register the test servlet and make a call
            httpService.registerServlet("/servlet", new HttpServiceServlet(bundle), null, null);
            Assert.assertEquals("http-service-example:0.0.0", performCall(reqspec));
        } finally {
            context.ungetService(sref);
        }

        assertNotAvailable(reqspec);
    }

    private void assertNotAvailable(String reqspec) throws Exception {
        try {
            performCall(reqspec);
            Assert.fail("IOException expected");
        } catch (IOException ex) {
            // expected
        }
    }

    private String performCall(String path) throws Exception {
        String urlspec = "http://localhost:8080/httpservice" + path;
        return HttpRequest.get(urlspec, 10, TimeUnit.SECONDS);
    }

    @SuppressWarnings("serial")
    static final class HttpServiceServlet extends HttpServlet {

        static Logger log = Logger.getLogger(HttpServiceServlet.class);

        private final XBundle bundle;

        // This hides the default ctor and verifies that this instance is used
        HttpServiceServlet(Bundle bundle) {
            this.bundle = (XBundle) bundle;
        }

        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            PrintWriter out = res.getWriter();
            String type = req.getParameter("test");
            if ("param".equals(type)) {
                String value = req.getParameter("param");
                out.print("Hello: " + value);
            } else if ("init".equals(type)) {
                String key = req.getParameter("init");
                String value = getInitParameter(key);
                out.print(key + "=" + value);
            } else if ("instance".equals(type)) {
                out.print(bundle.getCanonicalName());
            } else {
                throw new IllegalArgumentException("Invalid 'test' parameter: " + type);
            }
            out.close();
        }
    }
}