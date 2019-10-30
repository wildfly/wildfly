/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.http.Authentication;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests presence of X-Frame-Options header in response from management console
 *
 * @author Jan Kasik <jkasik@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XFrameOptionsHeaderTestCase {

    private static final int MGMT_PORT = 9990;

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void checkManagementConsoleForXFrameOptionsHeader() throws IOException, URISyntaxException {
        URL url = new URL("http", managementClient.getMgmtAddress(), MGMT_PORT, "/console/index.html");
        checkURLForHeader(url, "X-Frame-Options", "SAMEORIGIN");
    }

    private void checkURLForHeader(URL url, String headerName, String expectedHeaderValue) throws URISyntaxException, IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(createCredentialsProvider(url))
                .build()) {
            HttpContext httpContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(httpGet, httpContext);

            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Wrong response code: " + statusCode + " for url '" + url.toString() + "'.",
                    HttpURLConnection.HTTP_OK, statusCode);

            Header[] headers = response.getHeaders(headerName);
            assertNotNull("Unexpected behaviour of HttpResponse#getHeaders() returned null!", headers);
            assertTrue("There is no '" + headerName + "' header present! Headers present: " +
                            Arrays.toString(response.getAllHeaders()),
                    headers.length > 0);
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
