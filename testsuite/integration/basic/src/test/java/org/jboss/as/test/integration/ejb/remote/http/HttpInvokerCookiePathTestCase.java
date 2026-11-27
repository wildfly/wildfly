/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.http;

import java.net.URL;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the HTTP invoker session cookie has the correct path attribute.
 * This test verifies the fix for [WFLY-21930] where the cookie path was missing
 * the leading slash, causing load balancers and HTTP clients to reject the cookie.
 *
 * @author Rafael Rosa
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class HttpInvokerCookiePathTestCase {

    private static final String PATH_PREFIX = "path=";

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, HttpInvokerCookiePathTestCase.class.getSimpleName() + ".war");
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
                // The HTTP invoker endpoint requires authentication, so it returns 401 Unauthorized.
                // The JSESSIONID cookie is only set on 401 responses to avoid sticky session issues.
                Assertions.assertEquals(
                    401,
                    response.getStatusLine().getStatusCode(), "Expected 401 Unauthorized from the HTTP invoker endpoint"
                );

                // Inspect Set-Cookie headers
                Header[] setCookieHeaders = response.getHeaders("Set-Cookie");

                boolean foundJSessionId = false;
                String actualPath = null;

                // Look for JSESSIONID cookie from HTTP invoker
                for (Header header : setCookieHeaders) {
                    String cookieValue = header.getValue();

                    if (cookieValue.contains("JSESSIONID=")) {
                        foundJSessionId = true;

                        // Extract path attribute — lower-case the segment for case-insensitive matching
                        String[] parts = cookieValue.split(";");
                        for (String part : parts) {
                            String lower = part.trim().toLowerCase();
                            if (lower.startsWith(PATH_PREFIX)) {
                                actualPath = lower.substring(PATH_PREFIX.length());
                                break;
                            }
                        }
                        break;
                    }
                }

                Assertions.assertTrue(
                    foundJSessionId,
                    "HTTP invoker endpoint should set JSESSIONID cookie"
                );
                Assertions.assertNotNull(
                    actualPath,
                    "JSESSIONID cookie should have a path attribute"
                );
                Assertions.assertTrue(
                    actualPath.startsWith("/"),
                    "HTTP invoker cookie path must start with '/', but was: " + actualPath
                );
                Assertions.assertEquals(
                    "/wildfly-services",
                    actualPath,
                    "HTTP invoker cookie path should be '/wildfly-services'"
                );
            }
        }
    }
}
