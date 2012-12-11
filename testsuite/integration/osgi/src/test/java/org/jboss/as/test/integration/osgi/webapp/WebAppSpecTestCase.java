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
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.osgi.web.WebExtension;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.api.Echo;
import org.jboss.as.test.integration.osgi.webapp.bundle.SimpleAnnotatedServlet;
import org.jboss.as.test.integration.osgi.webapp.bundle.SimpleServlet;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
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

    static final Asset HOST_ASSET = new StringAsset("Hello from Host");
    static final Asset FRAGMENT_ASSET = new StringAsset("Hello from Fragment");

    static final String BUNDLE_A_WAB = "bundle-a.wab";
    static final String BUNDLE_B_WAB = "bundle-b.wab";
    static final String BUNDLE_C_WAB = "bundle-c.wab";
    static final String FRAGMENT_C = "fragment-c.jar";
    static final String BUNDLE_D_WAB = "bundle-d.wab";
    static final String BUNDLE_E_WAB = "bundle-e.wab";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    PackageAdmin packageAdmin;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-webapp-spec-tests");
        jar.addClasses(HttpRequest.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(PackageAdmin.class, ManagementClient.class);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Test
    public void testServletContextService() throws Exception {
        // The Web Extender must register the Servlet Context of the WAB as a service, using the Bundle Context of the WAB.
        deployer.deploy(BUNDLE_A_WAB);
        try {
            String result = performCall("/testcontext/testservletcontext");
            Assert.assertEquals("ServletContext: bundle-a.wab|/testcontext", result);
        } finally {
            deployer.undeploy(BUNDLE_A_WAB);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWebXMLInHostBundle() throws Exception {
        // The web.xml must be found with the Bundle findEntries method.
        deployer.deploy(BUNDLE_B_WAB);
        try {
            String result = performCall("/bundleB/servlet?input=Hello");
            Assert.assertEquals("Simple Servlet called with input=Hello", result);
            result = performCall("/bundleB/host-message.txt");
            Assert.assertEquals("Hello from Host", result);
            Bundle bundle = packageAdmin.getBundles(BUNDLE_B_WAB, null)[0];
            Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", true);
            Assert.assertNotNull("WEb-INF/web.xml entries found", entries);
        } finally {
            deployer.undeploy(BUNDLE_B_WAB);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWebXMLInFragment() throws Exception {
        // The findEntries method includes fragments, allowing the web.xml to be provided by a fragment.
        deployer.deploy(FRAGMENT_C);
        deployer.deploy(BUNDLE_C_WAB);
        try {
            String result = performCall("/bundleC/servlet?input=Hello");
            Assert.assertEquals("Simple Servlet called with input=Hello", result);
            Bundle bundle = packageAdmin.getBundles(BUNDLE_C_WAB, null)[0];
            Enumeration<URL> entries = bundle.findEntries("WEB-INF", "web.xml", true);
            Assert.assertNotNull("WEb-INF/web.xml entries found", entries);
        } finally {
            deployer.undeploy(BUNDLE_C_WAB);
            deployer.undeploy(FRAGMENT_C);
        }
    }

    @Test
    public void testLazyActivation() throws Exception {
        // The Web Extender should ensure that serving static content from the WAB
        // does not activate the WAB when it has a lazy activation policy.
        deployer.deploy(BUNDLE_D_WAB);
        try {
            Bundle bundle = packageAdmin.getBundles(BUNDLE_D_WAB, null)[0];
            //Assert.assertEquals(Bundle.STARTING, bundle.getState());
            String result = performCall("/bundleD/host-message.txt");
            Assert.assertEquals("Hello from Host", result);
            //Assert.assertEquals(Bundle.STARTING, bundle.getState());
            result = performCall("/bundleD/servlet?input=Hello");
            Assert.assertEquals("Simple Servlet called with input=Hello", result);
            Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
        } finally {
            deployer.undeploy(BUNDLE_D_WAB);
        }
    }

    @Test
    public void testForbiddenPaths() throws Exception {
        // For confidentiality reasons, a Web Runtime must not return any static content for paths that start with one of the following prefixes:
        // WEB-INF, OSGI-INF, META-INF, OSGI-OPT
        deployer.deploy(BUNDLE_E_WAB);
        try {
            String result = performCall("/bundleE/host-message.txt");
            Assert.assertEquals("Hello from Host", result);
            try {
                performCall("/bundleE/WEB-INF/forbidden.txt");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }
            try {
                performCall("/bundleE/META-INF/forbidden.txt");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }
            try {
                performCall("/bundleE/OSGI-INF/forbidden.txt");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }
            try {
                performCall("/bundleE/OSGI-OPT/forbidden.txt");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }
            result = performCall("/bundleE/servlet?input=Hello");
            Assert.assertEquals("Simple Servlet called with input=Hello", result);
        } finally {
            deployer.undeploy(BUNDLE_E_WAB);
        }
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 5, TimeUnit.SECONDS);
    }

    @Deployment(name = BUNDLE_A_WAB, managed = false, testable = false)
    public static Archive<?> getBundleA() {
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
                builder.addManifestHeader(WebExtension.WEB_CONTEXT_PATH,  "/testcontext");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_B_WAB, managed = false, testable = false)
    public static Archive<?> getBundleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B_WAB);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(SimpleServlet.class.getPackage(), "simple-web.xml", "WEB-INF/web.xml");
        archive.addAsResource(HOST_ASSET, "host-message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader(WebExtension.WEB_CONTEXT_PATH,  "/bundleB");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_C_WAB, managed = false, testable = false)
    public static Archive<?> getBundleC() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_C_WAB);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(HOST_ASSET, "host-message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader(WebExtension.WEB_CONTEXT_PATH,  "/bundleC");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = FRAGMENT_C, managed = false, testable = false)
    public static Archive<?> getFragmentC() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, FRAGMENT_C);
        archive.addAsResource(SimpleServlet.class.getPackage(), "simple-web.xml", "WEB-INF/web.xml");
        archive.addAsResource(FRAGMENT_ASSET, "fragment-message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addFragmentHost(BUNDLE_C_WAB);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_D_WAB, managed = false, testable = false)
    public static Archive<?> getBundleD() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_D_WAB);
        archive.addClasses(SimpleServlet.class, Echo.class);
        archive.addAsResource(SimpleServlet.class.getPackage(), "simple-web.xml", "WEB-INF/web.xml");
        archive.addAsResource(HOST_ASSET, "host-message.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivationPolicy(Constants.ACTIVATION_LAZY);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader(WebExtension.WEB_CONTEXT_PATH,  "/bundleD");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_E_WAB, managed = false, testable = false)
    public static Archive<?> getBundleE() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_E_WAB);
        archive.addClasses(SimpleAnnotatedServlet.class, Echo.class);
        archive.addAsResource(HOST_ASSET, "host-message.txt");
        archive.addAsResource(HOST_ASSET, "WEB-INF/forbidden.txt");
        archive.addAsResource(HOST_ASSET, "OSGI-INF/forbidden.txt");
        archive.addAsResource(HOST_ASSET, "META-INF/forbidden.txt");
        archive.addAsResource(HOST_ASSET, "OSGI-OPT/forbidden.txt");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivationPolicy(Constants.ACTIVATION_LAZY);
                builder.addImportPackages(Servlet.class, HttpServlet.class);
                builder.addManifestHeader(WebExtension.WEB_CONTEXT_PATH,  "/bundleE");
                return builder.openStream();
            }
        });
        return archive;
    }
}
