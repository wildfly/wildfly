/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.authentication;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.iteration.ByteIterator;

/**
 * Tests for secured access to WSDL for EJB endpoint
 *
 * @author Rostislav Svoboda
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EJBEndpointSecuredWSDLAccessTestCase {

    @ArquillianResource
    URL baseUrl;

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jaxws-authentication-ejb3-for-wsdl.jar")
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
    public void accessWSDLWithValidUsernameAndPassword() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");
        String encoding = base64Encode("user1:password1");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(wsdlURL.toString());
            httpget.setHeader("Authorization", "Basic " + encoding);
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                String text = Utils.getContent(response);
                Assert.assertTrue("Response doesn't contain wsdl file", text.contains("wsdl:binding"));
            }
        }
    }

    @Test
    public void accessWSDLWithValidUsernameAndPasswordButInvalidRole() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");
        String encoding = base64Encode("user2:password2");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(wsdlURL.toString());
            httpget.setHeader("Authorization", "Basic " + encoding);
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                Assert.assertEquals(403, response.getStatusLine().getStatusCode());

                Utils.getContent(response);
                //Assert.assertTrue("Response doesn't contain access denied message", text.contains("Access to the requested resource has been denied"));
            }
        }
    }

    @Test
    public void accessWSDLWithInvalidUsernameAndPassword() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");
        String encoding = base64Encode("user1:password-XZY");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(wsdlURL.toString());
            httpget.setHeader("Authorization", "Basic " + encoding);
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                Assert.assertEquals(401, response.getStatusLine().getStatusCode());

                Utils.getContent(response);
                //Assert.assertTrue("Response doesn't contain expected message.", text.contains("This request requires HTTP authentication"));
            }
        }
    }

    @Test
    public void accessWSDLWithoutUsernameAndPassword() throws Exception {
        URL wsdlURL = new URL(baseUrl, "/jaxws-authentication-ejb3-for-wsdl/EJB3ServiceForWSDL?wsdl");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(wsdlURL.toString());
            try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                Assert.assertEquals(401, response.getStatusLine().getStatusCode());

                Utils.getContent(response);
                //Assert.assertTrue("Response doesn't contain expected message.", text.contains("This request requires HTTP authentication"));
            }
        }
    }

    private static String base64Encode(final String original) {
        return ByteIterator.ofBytes(original.getBytes(StandardCharsets.UTF_8)).base64Encode().drainToString();
    }


}
