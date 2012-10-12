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
package org.jboss.as.test.integration.osgi.jaxrs;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.osgi.deployment.bundle.DeferredFailActivator;
import org.jboss.as.test.integration.osgi.jaxrs.bundle.SimpleResource;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Test simple OSGi REST deployment
 *
 * @author thomas.diesler@jboss.com
 * @since 30-Aug-2012
 */
@RunWith(Arquillian.class)
public class RestEndpointTestCase {

    private static final String SIMPLE_WAR = "simple.war";
    static final String BUNDLE_A_WAR = "bundle-a.war";
    static final String BUNDLE_B_WAR = "bundle-b.war";
    static final String BUNDLE_C_WAB = "bundle-c.wab";
    static final String BUNDLE_D_WAB = "bundle-d.wab";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @Inject
    public BundleContext context;

    @Deployment
    public static Archive<?> testDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-rest-test");
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

    @Test
    public void testSimpleWar() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            Assert.assertEquals("Hello World!", performCall("/simple/helloworld"));
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    @Test
    public void testSimpleWarAsBundle() throws Exception {
        deployer.deploy(BUNDLE_A_WAR);
        try {
            Assert.assertEquals("Hello World!", performCall("/bundle-a/helloworld"));
        } finally {
            deployer.undeploy(BUNDLE_A_WAR);
        }
    }

    @Test
    public void testBundleWithWarExtension() throws Exception {
        deployer.deploy(BUNDLE_B_WAR);
        try {
            Assert.assertEquals("Hello World!", performCall("/bundle-b/helloworld"));
        } finally {
            deployer.undeploy(BUNDLE_B_WAR);
        }
    }

    @Test
    public void testBundleWithWabExtension() throws Exception {
        deployer.deploy(BUNDLE_C_WAB);
        try {
            Assert.assertEquals("Hello World!", performCall("/bundle-c/helloworld"));
        } finally {
            deployer.undeploy(BUNDLE_C_WAB);
        }
    }

    @Test
    public void testDeferredBundleWithWabExtension() throws Exception {
        InputStream input = deployer.getDeployment(BUNDLE_C_WAB);
        Bundle bundle = context.installBundle(BUNDLE_C_WAB, input);
        try {
            Assert.assertEquals("INSTALLED", Bundle.INSTALLED, bundle.getState());
            try {
                performCall("/bundle-c/helloworld");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            Assert.assertEquals("Hello World!", performCall("/bundle-c/helloworld"));
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    @Ignore("[AS7-5653] Cannot restart webapp bundle after activation failure")
    public void testDeferredBundleWithFailure() throws Exception {
        InputStream input = deployer.getDeployment(BUNDLE_D_WAB);
        Bundle bundle = context.installBundle(BUNDLE_D_WAB, input);
        try {
            Assert.assertEquals("INSTALLED", Bundle.INSTALLED, bundle.getState());
            try {
                performCall("/bundle-d/helloworld");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }

            try {
                bundle.start();
                Assert.fail("BundleException expected");
            } catch (BundleException ex) {
                // expected
            }
            Assert.assertEquals("RESOLVED", Bundle.RESOLVED, bundle.getState());
            try {
                performCall("/bundle-d/helloworld");
                Assert.fail("IOException expected");
            } catch (IOException ex) {
                // expected
            }

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            Assert.assertEquals("Hello World!", performCall("/bundle-d/helloworld"));
        } finally {
            bundle.uninstall();
        }
    }

    private String performCall(String path) throws Exception {
        String urlspec = managementClient.getWebUri() + path;
        return HttpRequest.get(urlspec, 5, TimeUnit.SECONDS);
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar(){
        final WebArchive archive = create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(SimpleResource.class);
        archive.setWebXML(SimpleResource.class.getPackage(), "rest-web.xml");
        return archive;
    }

    @Deployment(name = BUNDLE_A_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWarAsBundle() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, BUNDLE_A_WAR);
        archive.addClasses(SimpleResource.class);
        archive.setWebXML(SimpleResource.class.getPackage(), "rest-web.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Produces.class, Application.class);
                builder.addBundleClasspath("WEB-INF/classes");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_B_WAR, managed = false, testable = false)
    public static Archive<?> getBundleWithWarExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B_WAR);
        archive.addClasses(SimpleResource.class);
        archive.addAsResource(SimpleResource.class.getPackage(), "rest-web.xml", "WEB-INF/web.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Produces.class, Application.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_C_WAB, managed = false, testable = false)
    public static Archive<?> getBundleWithWabExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_C_WAB);
        archive.addClasses(SimpleResource.class);
        archive.addAsResource(SimpleResource.class.getPackage(), "rest-web.xml", "WEB-INF/web.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Produces.class, Application.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_D_WAB, managed = false, testable = false)
    public static Archive<?> getBundleD() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_D_WAB);
        archive.addClasses(SimpleResource.class, DeferredFailActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(DeferredFailActivator.class);
                builder.addImportPackages(Produces.class, Application.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
