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
package org.jboss.as.test.integration.ws.authentication;

import java.net.URL;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for authentication against POJO endpoint
 *
 * @author Rostislav Svoboda
 */
@ServerSetup({EjbSecurityDomainSetup.class})
@RunWith(Arquillian.class)
@RunAsClient
public class PojoEndpointAuthenticationTestCase {

    @ArquillianResource
    URL baseUrl;

    QName serviceName = new QName("http://jbossws.org/authentication", "POJOAuthService");

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxws-authentication-pojo.war")
                .addAsResource(PojoEndpointAuthenticationTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(PojoEndpointAuthenticationTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addClasses(PojoEndpointIface.class, PojoEndpoint.class)
                .addAsWebInfResource(PojoEndpointAuthenticationTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(PojoEndpointAuthenticationTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        return war;
    }

    @Test
    public void accessHelloWithoutUsernameAndPassord() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-pojo/POJOAuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        PojoEndpointIface proxy = service.getPort(PojoEndpointIface.class);

        try {
            proxy.hello("World");
            Assert.fail("Test should fail, HTTP response '401: Unauthorized' was expected");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertTrue("HTTPException '401: Unauthorized' was expected", e.getCause().getMessage().contains("401: Unauthorized"));
        }
    }

    @Test
    public void accessHelloWithBadPassword() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-pojo/POJOAuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        PojoEndpointIface proxy = service.getPort(PojoEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password-XYZ");

        try {
            proxy.hello("World");
            Assert.fail("Test should fail, HTTP response '401: Unauthorized' was expected");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertTrue("HTTPException '401: Unauthorized' was expected", e.getCause().getMessage().contains("401: Unauthorized"));
        }
    }

    @Test
    public void accessHelloWithValidUser1() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-pojo/POJOAuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        PojoEndpointIface proxy = service.getPort(PojoEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password1");

        final String result = proxy.hello("World");
        Assert.assertEquals("Hello World!", result);
    }

    @Test
    public void accessHelloWithValidUser2() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-pojo/POJOAuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        PojoEndpointIface proxy = service.getPort(PojoEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user2");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password2");

        final String result = proxy.hello("World");
        Assert.assertEquals("Hello World!", result);
    }

    @Test
    public void accessHelloWithUnauthorizedUser() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-pojo/POJOAuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        PojoEndpointIface proxy = service.getPort(PojoEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user3");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password3");

        try {
            proxy.hello("World");
            Assert.fail("Test should fail, HTTP response '403: Forbidden' was expected");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertTrue("String '403: Forbidden' was expected in " + e.getCause().getMessage(), e.getCause().getMessage().contains("403: Forbidden"));
        }
    }
}
