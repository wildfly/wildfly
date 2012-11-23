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
package org.jboss.as.test.integration.osgi.jaxws;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.osgi.deployment.bundle.DeferredFailActivator;
import org.jboss.as.test.integration.osgi.jaxws.bundle.Endpoint;
import org.jboss.as.test.integration.osgi.jaxws.bundle.EndpointImpl;
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
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test web service endpoint functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Aug-2011
 */
@RunWith(Arquillian.class)
public class WebServiceEndpointTestCase {

    static final String SIMPLE_WAR = "simple.war";
    static final String BUNDLE_A_WAR = "bundle-a.war";
    static final String BUNDLE_B_WAR = "bundle-b.war";
    static final String BUNDLE_C_WAB = "bundle-c.wab";
    static final String BUNDLE_D_WAB = "bundle-d.wab";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    PackageAdmin packageAdmin;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static Archive<?> testDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "osgi-ws-test");
        archive.addClasses(Endpoint.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebService.class, SOAPBinding.class, QName.class, Service.class);
                builder.addImportPackages(PackageAdmin.class, ManagementClient.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSimpleWar() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
            Service service = Service.create(getWsdl("/simple"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    @Test
    public void testSimpleWarAsBundle() throws Exception {
        deployer.deploy(BUNDLE_A_WAR);
        try {
            QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
            Service service = Service.create(getWsdl("/bundle-a"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
        } finally {
            deployer.undeploy(BUNDLE_A_WAR);
        }
    }

    @Test
    public void testBundleWithWarExtension() throws Exception {
        deployer.deploy(BUNDLE_B_WAR);
        try {
            QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
            Service service = Service.create(getWsdl("/bundle-b"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
        } finally {
            deployer.undeploy(BUNDLE_B_WAR);
        }
    }

    @Test
    public void testBundleWithWabExtension() throws Exception {
        deployer.deploy(BUNDLE_C_WAB);
        try {
            QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
            Service service = Service.create(getWsdl("/bundle-c"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
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
                QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
                Service service = Service.create(getWsdl("/bundle-c"), serviceName);
                Endpoint port = service.getPort(Endpoint.class);
                port.echo("Foo");
                Assert.fail("WebServiceException expected");
            } catch (WebServiceException ex) {
                // expected
            }

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
            Service service = Service.create(getWsdl("/bundle-c"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
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
                QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
                Service service = Service.create(getWsdl("/bundle-d"), serviceName);
                Endpoint port = service.getPort(Endpoint.class);
                port.echo("Foo");
                Assert.fail("WebServiceException expected");
            } catch (WebServiceException ex) {
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
                QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
                Service service = Service.create(getWsdl("/bundle-d"), serviceName);
                Endpoint port = service.getPort(Endpoint.class);
                port.echo("Foo");
                Assert.fail("WebServiceException expected");
            } catch (WebServiceException ex) {
                // expected
            }

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
            Service service = Service.create(getWsdl("/bundle-d"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
        } finally {
            bundle.uninstall();
        }
    }

    private URL getWsdl(String contextPath) throws MalformedURLException {
        return new URL(managementClient.getWebUri() + contextPath + "/EndpointService?wsdl");
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        return archive;
    }

    @Deployment(name = BUNDLE_A_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWarAsBundle() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, BUNDLE_A_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebService.class, SOAPBinding.class);
                builder.addBundleClasspath("WEB-INF/classes");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_B_WAR, managed = false, testable = false)
    public static Archive<?> getBundleWithWarExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_B_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebService.class, SOAPBinding.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_C_WAB, managed = false, testable = false)
    public static Archive<?> getBundleWithWabExtension() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_C_WAB);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebService.class, SOAPBinding.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = BUNDLE_D_WAB, managed = false, testable = false)
    public static Archive<?> getBundleD() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_D_WAB);
        archive.addClasses(Endpoint.class, EndpointImpl.class, DeferredFailActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(DeferredFailActivator.class);
                builder.addImportPackages(WebService.class, SOAPBinding.class);
                builder.addImportPackages(BundleActivator.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}

