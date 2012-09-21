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

import javax.inject.Inject;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.osgi.jaxws.bundle.Endpoint;
import org.jboss.as.test.integration.osgi.jaxws.bundle.EndpointImpl;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test web service endpoint functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Aug-2011
 */
@RunAsClient
@RunWith(Arquillian.class)
public class WebServiceEndpointTestCase {

    static final String SIMPLE_WAR = "simple.war";
    static final String BUNDLE_A_WAR = "bundle-a.war";
    static final String BUNDLE_B_WAR = "bundle-b.war";
    static final String BUNDLE_C_WAB = "bundle-c.wab";

    @Inject
    public PackageAdmin packageAdmin;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static Archive<?> testDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-ws-test");
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
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        return archive;
    }

    @Deployment(name = BUNDLE_A_WAR, testable = false)
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

    @Deployment(name = BUNDLE_B_WAR, testable = false)
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

    @Deployment(name = BUNDLE_C_WAB, testable = false)
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

    @Test
    @OperateOnDeployment(SIMPLE_WAR)
    public void testSimpleWar() throws Exception {
        QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
        Service service = Service.create(getWsdl("/simple"), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }

    @Test
    @OperateOnDeployment(BUNDLE_A_WAR)
    public void testSimpleWarAsBundle() throws Exception {
        QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
        Service service = Service.create(getWsdl("/bundle-a"), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }

    @Test
    @OperateOnDeployment(BUNDLE_B_WAR)
    public void testBundleWithWarExtension() throws Exception {
        QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
        Service service = Service.create(getWsdl("/bundle-b"), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }

    @Test
    @OperateOnDeployment(BUNDLE_C_WAB)
    public void testBundleWithWabExtension() throws Exception {
        QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
        Service service = Service.create(getWsdl("/bundle-c"), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }

    private URL getWsdl(String contextPath) throws MalformedURLException {
        return new URL(managementClient.getWebUri() + contextPath + "/EndpointService?wsdl");
    }
}
