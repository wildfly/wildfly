/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.ejb;

import java.net.URL;
import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests web service context injection into Jakarta XML Web Services handler.
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ContextHandlerEndpointTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        JavaArchive ejb3Jar = ShrinkWrap.create(JavaArchive.class, "context-handler-endpoint.jar");
        ejb3Jar.addClass(ContextHandler.class);
        ejb3Jar.addClass(ContextHandlerEndpointIface.class);
        ejb3Jar.addClass(ContextHandlerEndpointImpl.class);
        ejb3Jar.addAsResource(ContextHandler.class.getPackage(), "handler.xml", "org/jboss/as/test/integration/ws/ejb/handler.xml");

        return ejb3Jar;
    }

    @Test
    public void testHandlerContext() throws Exception {
        QName serviceName = new QName("org.jboss.as.test.integration.ws.ejb", "ContextHandlerService");
        URL wsdlURL = new URL(baseUrl, "/context-handler-endpoint/ContextHandlerService/ContextHandlerEndpointImpl?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        ContextHandlerEndpointIface port = service.getPort(ContextHandlerEndpointIface.class);

        String response = port.doSomething("hello");
        Assert.assertNotNull("Response is null.", response);
    }
}
