/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ws.ejb;

import java.net.URL;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

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
     * Test for javax.ejb.Remote annotation in WS interface
     */
    @Test
    public void testRemoteAccess() throws Exception {

        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(props);
        SimpleStatelessWebserviceEndpointIface ejb3Remote = (SimpleStatelessWebserviceEndpointIface)
                context.lookup("ejb:/stateless-ws-endpoint-example/SimpleStatelessWebserviceEndpointImpl!" + SimpleStatelessWebserviceEndpointIface.class.getName());

        String helloWorld = "Hello world!";
        Object retObj = ejb3Remote.echo(helloWorld);
        Assert.assertEquals(helloWorld, retObj);
    }

}
