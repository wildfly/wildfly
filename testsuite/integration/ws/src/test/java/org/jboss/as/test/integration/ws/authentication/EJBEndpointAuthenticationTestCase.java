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
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for authentication against EJB endpoint
 *
 * @author Rostislav Svoboda
 */
@ServerSetup({EjbSecurityDomainSetup.class})
@RunWith(Arquillian.class)
@RunAsClient
public class EJBEndpointAuthenticationTestCase {

    @ArquillianResource
    URL baseUrl;

    QName serviceName = new QName("http://jbossws.org/authentication", "EJB3AuthService");

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jaxws-authentication-ejb.jar")
                .addAsResource(EJBEndpointAuthenticationTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(EJBEndpointAuthenticationTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addClasses(EJBEndpointIface.class, EJBEndpoint.class);
        return jar;
    }

    // ------------------------------------------------------------------------------
    //
    // Tests for hello method
    //

    @Test
    public void accessHelloWithoutUsernameAndPassord() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

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
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

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
    public void accessHelloWithUnauthorizedUser() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user2");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password2");

        try {
            proxy.hello("World");
            Assert.fail("Test should fail, user shouldn't be allowed to invoke that method");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertEquals(getNotAllowedExceptionMessage("hello"), e.getCause().getMessage());
        }
    }

    @Test
    public void accessHelloWithValidUser() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password1");

        final String result = proxy.hello("World");
        Assert.assertEquals("Hello World!", result);
    }


    // ------------------------------------------------------------------------------
    //
    // Tests for helloForRole method
    //

    @Test
    public void accessHelloForRoleWithInvalidRole() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password1");

        try {
            proxy.helloForRole("World");
            Assert.fail("Test should fail, user shouldn't be allowed to invoke that method");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertEquals(getNotAllowedExceptionMessage("helloForRole"), e.getCause().getMessage());
        }
    }

    @Test
    public void accessHelloForRoleWithValidRole() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user2");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password2");

        final String result = proxy.helloForRole("World");
        Assert.assertEquals("Hello World!", result);
    }


    // ------------------------------------------------------------------------------
    //
    // Tests for helloForRoles method
    //

    @Test
    public void accessHelloForRolesWithValidRole1() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password1");

        final String result = proxy.helloForRoles("World");
        Assert.assertEquals("Hello World!", result);
    }

    @Test
    public void accessHelloForRolesWithValidRole2() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user2");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password2");

        final String result = proxy.helloForRoles("World");
        Assert.assertEquals("Hello World!", result);
    }

    @Test
    public void accessHelloForRolesWithInvalidRole() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user3");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password3");

        try {
            proxy.helloForRoles("World");
            Assert.fail("Test should fail, user shouldn't be allowed to invoke that method");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertEquals(getNotAllowedExceptionMessage("helloForRoles"), e.getCause().getMessage());
        }
    }


    // ------------------------------------------------------------------------------
    //
    // Tests for helloForAll method
    //

    @Test
    public void accessHelloForAllWithValidRole1() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password1");

        final String result = proxy.helloForAll("World");
        Assert.assertEquals("Hello World!", result);
    }

    @Test
    public void accessHelloForAllWithValidRole2() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user2");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password2");

        final String result = proxy.helloForAll("World");
        Assert.assertEquals("Hello World!", result);
    }

    @Test
    public void accessHelloForAllWithValidRole3() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user3");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password3");

        final String result = proxy.helloForAll("World");
        Assert.assertEquals("Hello World!", result);
    }

    // ------------------------------------------------------------------------------
    //
    // Tests for helloForNone method
    //

    @Test
    public void accessHelloForNoneWithValidRole1() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user1");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password1");

        try {
            proxy.helloForNone("World");
            Assert.fail("Test should fail, user shouldn't be allowed to invoke that method");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertEquals(getNotAllowedExceptionMessage("helloForNone"), e.getCause().getMessage());
        }
    }

    @Test
    public void accessHelloForNoneWithValidRole2() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user2");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password2");

        try {
            proxy.helloForNone("World");
            Assert.fail("Test should fail, user shouldn't be allowed to invoke that method");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertEquals(getNotAllowedExceptionMessage("helloForNone"), e.getCause().getMessage());
        }
    }

    @Test
    public void accessHelloForNoneWithValidRole3() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3/EJB3AuthService?wsdl");

        Service service = Service.create(wsdlURL, serviceName);
        EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);

        Map<String, Object> reqContext = ((BindingProvider) proxy).getRequestContext();
        reqContext.put(BindingProvider.USERNAME_PROPERTY, "user3");
        reqContext.put(BindingProvider.PASSWORD_PROPERTY, "password3");

        try {
            proxy.helloForNone("World");
            Assert.fail("Test should fail, user shouldn't be allowed to invoke that method");
        } catch (WebServiceException e) {
            // failure is expected
            Assert.assertEquals(getNotAllowedExceptionMessage("helloForNone"), e.getCause().getMessage());
        }
    }

    /**
     * @deprecated This is a really bad way to do this - we cannot unexport this non-API class until this is removed
     */
    @Deprecated
    private String getNotAllowedExceptionMessage(String methodName) throws NoSuchMethodException {
        return EjbLogger.ROOT_LOGGER.invocationOfMethodNotAllowed(EJBEndpoint.class.getMethod(methodName, String.class), "EJBEndpoint").getMessage();
    }
}
