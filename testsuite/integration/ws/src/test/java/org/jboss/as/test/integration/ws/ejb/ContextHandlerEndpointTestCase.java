/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
 * Tests web service context injection into JAX-WS handler.
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
