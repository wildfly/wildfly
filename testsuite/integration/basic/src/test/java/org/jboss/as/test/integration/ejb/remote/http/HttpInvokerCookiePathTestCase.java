/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.http;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

import java.net.URL;

/**
 * Tests that the HTTP invoker session cookie has the correct path attribute.
 * This test verifies the fix for [WFLY-21930] where the cookie path was missing
 * the leading slash, causing load balancers and HTTP clients to reject the cookie.
 *
 * @author Rafael Rosa
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpInvokerCookiePathTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "http-cookie-path-test.war");
        war.addClasses(EchoBean.class, EchoRemote.class);
        return war;
    }

     /**
     * Verifies that the JSESSIONID cookie set by the HTTP invoker endpoint
     * has a path attribute that starts with a forward slash.
     */
    @Test
    public void testHttpInvokerCookiePathHasLeadingSlash() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Access the wildfly-services endpoint to trigger HTTP invoker session cookie
            String wildflyServicesUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/wildfly-services";
            HttpGet request = new HttpGet(wildflyServicesUrl);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                // Inspect Set-Cookie headers
                Header[] setCookieHeaders = response.getHeaders("Set-Cookie");

                boolean foundJSessionId = false;
                String actualPath = null;

                // Look for JSESSIONID cookie from HTTP invoker
                for (Header header : setCookieHeaders) {
                    String cookieValue = header.getValue();

                    if (cookieValue.contains("JSESSIONID=")) {
                        foundJSessionId = true;

                        // Extract path attribute
                        String[] parts = cookieValue.split(";");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (trimmed.toLowerCase().startsWith("path=")) {
                                actualPath = trimmed.substring(5);
                                break;
                            }
                        }
                        break;
                    }
                }

                Assert.assertTrue(
                    "HTTP invoker endpoint should set JSESSIONID cookie",
                    foundJSessionId
                );
                Assert.assertNotNull(
                    "JSESSIONID cookie should have a path attribute",
                    actualPath
                );
                Assert.assertTrue(
                    "HTTP invoker cookie path must start with '/', but was: " + actualPath,
                    actualPath.startsWith("/")
                );
                Assert.assertEquals(
                    "HTTP invoker cookie path should be '/wildfly-services'",
                    "/wildfly-services",
                    actualPath
                );
            }
        }
    }
}
