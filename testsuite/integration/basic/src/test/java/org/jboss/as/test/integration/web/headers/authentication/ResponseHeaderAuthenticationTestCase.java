/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.headers.authentication;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Test configures elytron with the key-store and security domain for undertow.
 * Deploys application with the user/password form and fills the form with the predefined application user credentials.
 * Test checks the "Set-Cookie" response header if there is not undefined domain value.
 * Test for [ WFLY-11071 ].
 *
 * @author Daniel Cihak
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(ResponseHeaderAuthenticationServerSetupTask.class)
@RunAsClient
public class ResponseHeaderAuthenticationTestCase {

    public static final String PASSWORD = "password1";
    private static final String DEPLOYMENT = "test";
    private static final String DOMAIN_ATTRIBUTE = "domain=";

    @Deployment(name=DEPLOYMENT)
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addAsWebInfResource(ResponseHeaderAuthenticationTestCase.class.getPackage(), "web.xml", "/web.xml");
        war.addAsWebInfResource(ResponseHeaderAuthenticationTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebResource(ResponseHeaderAuthenticationTestCase.class.getPackage(), "login.html", "login.html");
        war.addAsWebResource(ResponseHeaderAuthenticationTestCase.class.getPackage(), "index.jsp", "index.jsp");
        war.addAsWebResource(ResponseHeaderAuthenticationTestCase.class.getPackage(), "error.jsp", "error.jsp");
        return war;
    }

    /**
     * Tests if the SSO response header has not domain value undefined.
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void testResponseHeaderAuthentication(@ArquillianResource URL url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String appUrl = url.toExternalForm() + "j_security_check";

            HttpPost httpPost = new HttpPost(appUrl);
            httpPost.addHeader("Referer", url.toExternalForm() + "login.html");
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("j_username", "user1"));
            formParams.add(new BasicNameValuePair("j_password", PASSWORD));
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
            HttpResponse response = httpClient.execute(httpPost);

            Header[] setCookieHeaders = response.getHeaders("Set-Cookie");
            for (Header header: setCookieHeaders) {
                String headerValue = header.getValue();
                if (headerValue.startsWith("JSESSIONIDSSO=") && headerValue.contains(DOMAIN_ATTRIBUTE)) {
                    String domainValue = headerValue.split(DOMAIN_ATTRIBUTE)[1];
                    assertNotEquals(domainValue, "undefined");
                }
            }
        }
    }

}
