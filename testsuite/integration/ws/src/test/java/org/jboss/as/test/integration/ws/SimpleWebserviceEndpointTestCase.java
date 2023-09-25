/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws;

import java.net.URL;
import org.jboss.logging.Logger;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.spi.Provider;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests simple stateless web service endpoint invocation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
public class SimpleWebserviceEndpointTestCase {

    @ArquillianResource
    URL baseUrl;

    private static final Logger log = Logger.getLogger(SimpleWebserviceEndpointTestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-endpoint-example.war");
        war.addPackage(SimpleWebserviceEndpointImpl.class.getPackage());
        war.addClass(SimpleWebserviceEndpointImpl.class);
        war.addAsWebInfResource(SimpleWebserviceEndpointTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testJBossWSIntegrationIsInPlace() {
        String p = Provider.provider().getClass().getName();
        Assert.assertTrue(p + " is not a JBossWS implementation of jakarta.xml.ws.spi.Provider", p.startsWith("org.jboss."));
    }


    @Test
    @RunAsClient
    public void testSimpleStatelessWebserviceEndpoint() throws Exception {

        final QName serviceName = new QName("org.jboss.as.test.integration.ws", "SimpleService");
        final URL wsdlURL = new URL(baseUrl, "/ws-endpoint-example/SimpleService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final SimpleWebserviceEndpointIface port = service.getPort(SimpleWebserviceEndpointIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("hello", result);
    }

}
