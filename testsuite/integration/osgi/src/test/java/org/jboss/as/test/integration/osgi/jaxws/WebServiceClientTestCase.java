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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.osgi.jaxws.bundle.Endpoint;
import org.jboss.as.test.integration.osgi.jaxws.bundle.EndpointImpl;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test web service client functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 28-Aug-2011
 */
@RunWith(Arquillian.class)
public class WebServiceClientTestCase {

    static final String SIMPLE_ENDPOINT_WAR = "simple-endpoint.war";
    static final String CLIENT_BUNDLE = "simple-client.jar";

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment(name = SIMPLE_ENDPOINT_WAR, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_ENDPOINT_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        return archive;
    }

    @Deployment(name = CLIENT_BUNDLE)
    public static Archive<?> getSimpleWarAsBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CLIENT_BUNDLE);
        archive.addClasses(Endpoint.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(WebService.class, SOAPBinding.class, QName.class, Service.class);
                builder.addImportPackages(ManagementClient.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @OperateOnDeployment(CLIENT_BUNDLE)
    public void testSimpleWarAsBundle() throws Exception {
        QName serviceName = new QName("http://osgi.smoke.test.as.jboss.org", "EndpointService");
        Service service = Service.create(getWsdl(), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        Assert.assertEquals("Foo", port.echo("Foo"));
    }

    private URL getWsdl() throws MalformedURLException {
        return new URL(managementClient.getWebUri() + "/simple-endpoint/EndpointService?wsdl");
    }
}
