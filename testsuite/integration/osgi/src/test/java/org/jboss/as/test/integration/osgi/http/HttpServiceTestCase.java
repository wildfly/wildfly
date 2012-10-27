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
package org.jboss.as.test.integration.osgi.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.resolver.XBundle;
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
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test the {@link HttpService} on JBossWeb
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 19-Jul-2011
 */
@RunWith(Arquillian.class)
public class HttpServiceTestCase {

    static StringAsset STRING_ASSET = new StringAsset("Hello from Resource");

    @ArquillianResource
    Bundle bundle;

    @ArquillianResource
    ManagementClient managementClient;

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
                builder.addImportPackages(XBundle.class, ManagementClient.class);
                builder.addImportPackages(ServiceTracker.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @InSequence(0)
    public void testServletAccess() throws Exception {

        // The first test in sequence tracks the HttpService
        // instead of assuming that it is already available.

        class TestHandler {
            private final CountDownLatch latch = new CountDownLatch(1);
            Exception lastException;

            void performTest(HttpService httpService) {
                String reqspec = "/httpservice/servlet?test=param&param=Kermit";
                try {
                    // Verify that the alias is not yet available
                    assertNotAvailable(reqspec);

                    // Register the test servlet and make a call
                    httpService.registerServlet("/servlet", new HttpServiceServlet(bundle), null, null);
                    Assert.assertEquals("Hello: Kermit", performCall(reqspec));

                    // Unregister the servlet alias
                    httpService.unregister("/servlet");
                    assertNotAvailable(reqspec);

                } catch (Exception ex) {
                    lastException = ex;
                } finally {
                    complete();
                }
            }

            void complete() {
                latch.countDown();
            }

            void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
                if (!latch.await(timeout, unit))
                    throw new TimeoutException();
            }
        }

        final TestHandler handler = new TestHandler();
        final BundleContext context = bundle.getBundleContext();
        final ServiceReference[] srefholder = new ServiceReference[1];
        ServiceTracker tracker = new ServiceTracker(context, HttpService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference sref) {
                srefholder[0] = sref;
                HttpService httpService = (HttpService) super.addingService(sref);
                handler.performTest(httpService);
                return httpService;
            }
        };
        tracker.open();

        handler.awaitCompletion(30, TimeUnit.SECONDS);

        if (handler.lastException != null)
            throw handler.lastException;

        ServiceReference sref = srefholder[0];
        if (sref != null) {
            context.ungetService(sref);
        }
    }

    @Test
    @InSequence(1)
    public void testResourceAccess() throws Exception {
        BundleContext context = bundle.getBundleContext();
        final ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        Assert.assertNotNull("ServiceReference was not found for " + HttpService.class.getName(), sref);
        String reqspec = "/httpservice/resource/message.txt";
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
    @InSequence(1)
    public void testServletInitProps() throws Exception {
        BundleContext context = bundle.getBundleContext();
        final ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        Assert.assertNotNull("ServiceReference was not found for " + HttpService.class.getName(), sref);
        String reqspec = "/httpservice/servlet?test=init&init=someKey";
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
    @InSequence(1)
    public void testServletInstance() throws Exception {
        BundleContext context = bundle.getBundleContext();
        final ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        Assert.assertNotNull("ServiceReference was not found for " + HttpService.class.getName(), sref);
        String reqspec = "/httpservice/servlet?test=instance";
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

    @Test
    public void testServletContext() throws Exception {
        BundleContext context = bundle.getBundleContext();
        final ServiceReference sref = context.getServiceReference(HttpService.class.getName());
        Assert.assertNotNull("ServiceReference was not found for " + HttpService.class.getName(), sref);
        String reqspec = "/httpservice/servlet2?test=param&param=Kermit";
        try {
            HttpService httpService = (HttpService) context.getService(sref);

            // Verify that the alias is not yet available
            assertNotAvailable(reqspec);

            // Register the test servlet
            HttpServiceServlet servlet = new HttpServiceServlet(bundle);
            Dictionary<String, String> parms = new Hashtable<String, String>();
            parms.put("foo", "bar");
            httpService.registerServlet("/servlet2", servlet, parms, null);

            // Check that the ServletContext is available
            Assert.assertEquals("/httpservice", servlet.getServletContext().getContextPath());

            // Check that the ServletConfig is available
            Assert.assertEquals("bar", servlet.getServletConfig().getInitParameter("foo"));

            // Unregister the servlet alias
            httpService.unregister("/servlet2");
            assertNotAvailable(reqspec);
        } finally {
            context.ungetService(sref);
        }
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
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 10, TimeUnit.SECONDS);
    }

    @SuppressWarnings("serial")
    static final class HttpServiceServlet extends HttpServlet {

        private final XBundle bundle;

        // This hides the default ctor and verifies that this instance is used
        HttpServiceServlet(Bundle bundle) {
            this.bundle = (XBundle) bundle;
        }



        @Override
        public void init(ServletConfig config) throws ServletException {
            System.err.println("*** init() : " + config);
            super.init(config);
        }



        @Override
        public void init() throws ServletException {
            System.err.println("### init()");
            super.init();
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