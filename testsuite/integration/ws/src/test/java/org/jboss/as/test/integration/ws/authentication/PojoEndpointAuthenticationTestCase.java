/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.authentication;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for authentication against POJO endpoint
 *
 * @author Rostislav Svoboda
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PojoEndpointAuthenticationTestCase {

    @ArquillianResource
    URL baseUrl;

    QName serviceName = new QName("http://jbossws.org/authentication", "POJOAuthService");

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxws-authentication-pojo.war")
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
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "guest");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "guest");

        try {
            proxy.hello("World");
            Assert.fail("Test should fail, HTTP response '403: Forbidden' was expected");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertTrue("String '403: Forbidden' was expected in " + e.getCause().getMessage(), e.getCause().getMessage().contains("403: Forbidden"));
        }
    }
}
