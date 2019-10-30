/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.cdi.interceptor;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [JBWS-3441] Support CDI interceptors for POJO JAX-WS services
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CdiInterceptorTestCase {
    @ArquillianResource
    URL baseUrl;

    public static final String ARCHIVE_NAME = "jaxws-cdi-interceptors";

    @Deployment
    public static Archive<?> archive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(CdiInterceptorTestCase.class.getPackage());
        war.addAsWebInfResource(new StringAsset(BEANS_CONFIG), "beans.xml");
        return war;

    }

    private EndpointIface getPojo() throws Exception {
        final URL wsdlURL = new URL(baseUrl + "POJOEndpointService?wsdl");
        final QName serviceName = new QName("http://org.jboss.test.ws/jbws3441", "POJOEndpointService");
        final Service service = Service.create(wsdlURL, serviceName);
        return service.getPort(EndpointIface.class);
    }

    private EndpointIface getEjb3() throws Exception {
        final URL wsdlURL = new URL(baseUrl + "EJB3EndpointService/EJB3Endpoint?wsdl");
        final QName serviceName = new QName("http://org.jboss.test.ws/jbws3441", "EJB3EndpointService");
        final Service service = Service.create(wsdlURL, serviceName);
        return service.getPort(EndpointIface.class);
    }

    @Test
    public void testPojoCall() throws Exception {
        String message = "Hi";
        String response = getPojo().echo(message);
        Assert.assertEquals("Hi (including POJO interceptor)", response);
    }

    @Test
    public void testEjb3Call() throws Exception {
        String message = "Hi";
        String response = getEjb3().echo(message);
        Assert.assertEquals("Hi (including EJB interceptor)", response);
    }

    private static final String BEANS_CONFIG = "<beans><interceptors>"
            + "<class>org.jboss.as.test.integration.ws.cdi.interceptor.POJOInterceptorImpl</class>"
            + "<class>org.jboss.as.test.integration.ws.cdi.interceptor.EJBInterceptorImpl</class>" + "</interceptors></beans>";
}
