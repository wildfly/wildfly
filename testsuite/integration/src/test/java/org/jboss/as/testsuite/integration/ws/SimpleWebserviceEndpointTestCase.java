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

package org.jboss.as.testsuite.integration.ws;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.jaxrs.servletintegration.WebXml;
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

    private static final Logger log = Logger.getLogger(SimpleWebserviceEndpointTestCase.class.getName());

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ws-endpoint-example.war");
        war.addPackage(SimpleWebserviceEndpointImpl.class.getPackage());
        war.addClass(SimpleWebserviceEndpointImpl.class);
        war.addAsWebResource(WebXml.get("<servlet>" +
            "    <servlet-name>TestService</servlet-name>" +
            "    <servlet-class>org.jboss.as.testsuite.integration.ws.SimpleWebserviceEndpointImpl</servlet-class>" +
            "  </servlet>" +
            "  <servlet-mapping>" +
            "    <servlet-name>TestService</servlet-name>" +
            "    <url-pattern>/SimpleService</url-pattern>" +
            "  </servlet-mapping>"),"web.xml");
        log.info(war.toString(true));
        return war;
    }

    @Test
    public void testSimpleStatelessWebserviceEndpoint() throws Exception {
        final QName serviceName = new QName("org.jboss.as.testsuite.integration.ws", "SimpleService");
        final URL wsdlURL = new URL("http://localhost:8080/ws-endpoint-example/SimpleService?wsdl");
        final Service service = Service.create(wsdlURL, serviceName);
        final SimpleWebserviceEndpointIface port = service.getPort(SimpleWebserviceEndpointIface.class);
        final String result = port.echo("hello");
        Assert.assertEquals("hello", result);
    }

}
