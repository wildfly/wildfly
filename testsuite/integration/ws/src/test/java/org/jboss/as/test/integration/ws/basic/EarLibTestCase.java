/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.basic;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="klape@redhat.com">Kyle Lape</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EarLibTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ws-app.ear");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-example.war");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "endpoint.jar");
        jar.addClasses(EndpointIface.class, PojoEndpoint.class, HelloObject.class);
        war.setWebXML(EarLibTestCase.class.getPackage(), "web.xml");
        ear.addAsDirectory("/lib");
        ear.add(jar, "/lib", ZipExporter.class);
        ear.add(war, "/", ZipExporter.class);
        return ear;
    }

    @Test
    public void testWSDL() throws Exception {
        String s = performCall("?wsdl");
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("wsdl:definitions"));
    }

    private String performCall(String params) throws Exception {
        URL url = new URL(this.url.toExternalForm() + "ws-example/" + params);
        return HttpRequest.get(url.toExternalForm(), 30, TimeUnit.SECONDS);
    }
}