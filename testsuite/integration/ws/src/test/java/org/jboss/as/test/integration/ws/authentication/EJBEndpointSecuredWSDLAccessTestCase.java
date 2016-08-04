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
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.security.EjbSecurityDomainSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.security.Base64Encoder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for secured access to WSDL for EJB endpoint
 *
 * @author Rostislav Svoboda
 */
@ServerSetup({EjbSecurityDomainSetup.class})
@RunWith(Arquillian.class)
@RunAsClient
public class EJBEndpointSecuredWSDLAccessTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jaxws-authentication-ejb3-for-wsdl.jar")
                .addAsResource(EJBEndpointSecuredWSDLAccessTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(EJBEndpointSecuredWSDLAccessTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addClasses(EJBEndpointIface.class, EJBEndpointSecuredWSDLAccess.class);

        return jar;
    }

    @Test
    public void createService() throws Exception {
        QName serviceName = new QName("http://jbossws.org/authenticationForWSDL", "EJB3ServiceForWSDL");
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");


        try {
            Service service = Service.create(wsdlURL, serviceName);
            EJBEndpointIface proxy = service.getPort(EJBEndpointIface.class);
            Assert.fail("Proxy shouldn't be created because WSDL access should be secured");
        } catch (WebServiceException e) {
            // failure is expected
        }
    }


    @Test
    public void accessWSDLWithValidUsernameAndPassord() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");
        String encoding = Base64Encoder.encode("user1:password1");

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(wsdlURL.toString());
        httpget.setHeader("Authorization", "Basic " + encoding);
        HttpResponse response = httpclient.execute(httpget);

        String text = Utils.getContent(response);
        Assert.assertTrue("Response doesn't contain wsdl file", text.contains("wsdl:binding"));

    }

    @Test
    public void accessWSDLWithValidUsernameAndPassordButInvalidRole() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");
        String encoding = Base64Encoder.encode("user2:password2");

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(wsdlURL.toString());
        httpget.setHeader("Authorization", "Basic " + encoding);
        HttpResponse response = httpclient.execute(httpget);
        Assert.assertEquals(403, response.getStatusLine().getStatusCode());

        Utils.getContent(response);
        //Assert.assertTrue("Response doesn't contain access denied message", text.contains("Access to the requested resource has been denied"));

    }

    @Test
    public void accessWSDLWithInvalidUsernameAndPassord() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");
        String encoding = Base64Encoder.encode("user1:password-XZY");

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(wsdlURL.toString());
        httpget.setHeader("Authorization", "Basic " + encoding);
        HttpResponse response = httpclient.execute(httpget);
        Assert.assertEquals(401, response.getStatusLine().getStatusCode());

        Utils.getContent(response);
        //Assert.assertTrue("Response doesn't contain expected message.", text.contains("This request requires HTTP authentication"));
    }

    @Test
    public void accessWSDLWithoutUsernameAndPassord() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");

        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(wsdlURL.toString());
        HttpResponse response = httpclient.execute(httpget);
        Assert.assertEquals(401, response.getStatusLine().getStatusCode());

        Utils.getContent(response);
        //Assert.assertTrue("Response doesn't contain expected message.", text.contains("This request requires HTTP authentication"));
    }


}
