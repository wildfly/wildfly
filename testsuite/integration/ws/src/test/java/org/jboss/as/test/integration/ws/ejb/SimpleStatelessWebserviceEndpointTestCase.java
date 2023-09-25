/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.ejb;

import java.net.URL;

import javax.naming.Context;
import javax.xml.namespace.QName;
import jakarta.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests simple stateless web service endpoint invocation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleStatelessWebserviceEndpointTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stateless-ws-endpoint-example.jar");
        jar.addClasses(SimpleStatelessWebserviceEndpointIface.class, SimpleStatelessWebserviceEndpointImpl.class);
        return jar;
    }

    @Test
    public void testSimpleStatelessWebserviceEndpoint() throws Exception {
        final QName serviceName = new QName("org.jboss.as.test.integration.ws.ejb", "SimpleService");
        final URL wsdlURL = new URL(baseUrl, "/stateless-ws-endpoint-example/SimpleService/SimpleStatelessWebserviceEndpointImpl?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final SimpleStatelessWebserviceEndpointIface port = service.getPort(SimpleStatelessWebserviceEndpointIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("hello", result);
    }

    /*
     * Test for jakarta.ejb.Remote annotation in WS interface
     */
    @Test
    public void testRemoteAccess() throws Exception {

        final Context context = Util.createNamingContext();
        SimpleStatelessWebserviceEndpointIface ejb3Remote = (SimpleStatelessWebserviceEndpointIface)
                context.lookup("ejb:/stateless-ws-endpoint-example/SimpleStatelessWebserviceEndpointImpl!" + SimpleStatelessWebserviceEndpointIface.class.getName());

        String helloWorld = "Hello world!";
        Object retObj = ejb3Remote.echo(helloWorld);
        Assert.assertEquals(helloWorld, retObj);
    }

}
