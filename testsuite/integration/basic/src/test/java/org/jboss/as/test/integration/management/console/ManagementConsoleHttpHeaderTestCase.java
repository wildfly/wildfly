/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.console;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.http.Authentication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests presence of X-Frame-Options header in response from management console
 *
 * @author Jan Kasik <jkasik@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class ManagementConsoleHttpHeaderTestCase {

    private static final int MGMT_PORT = 9990;

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void checkManagementConsoleForXFrameOptionsHeader() throws IOException, URISyntaxException {
        URL url = new URL("http", managementClient.getMgmtAddress(), MGMT_PORT, "/console/index.html");
        checkURLForHeader(url, "X-Frame-Options", "SAMEORIGIN");
    }

    @Test
    public void checkManagementConsoleForXContentTypeOptionsHeader() throws IOException, URISyntaxException {
        URL url = new URL("http", managementClient.getMgmtAddress(), MGMT_PORT, "/console/index.html");
        checkURLForHeader(url, "X-Content-Type-Options", "nosniff");
    }

    private void checkURLForHeader(URL url, String headerName, String expectedHeaderValue) throws URISyntaxException, IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(createCredentialsProvider(url))
                .build()) {
            HttpContext httpContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(httpGet, httpContext);

            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals(HttpURLConnection.HTTP_OK,
                    statusCode, "Wrong response code: " + statusCode + " for url '" + url.toString() + "'.");

            Header[] headers = response.getHeaders(headerName);
            assertNotNull(headers, "Unexpected behaviour of HttpResponse#getHeaders() returned null!");
            assertTrue(headers.length > 0,
                    "There is no '" + headerName + "' header present! Headers present: " +
                            Arrays.toString(response.getAllHeaders()));
            for (Header header : headers) {
                if (header.getValue().equals(expectedHeaderValue)) {
                    return;
                }
            }
            fail("No header '" + headerName + "' with value '" + expectedHeaderValue + "' found!");
        }
    }

    private CredentialsProvider createCredentialsProvider(URL url) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(Authentication.USERNAME, Authentication.PASSWORD);
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort(), "ManagementRealm"), credentials);
        return credentialsProvider;
    }

}
