/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.http;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.junit.Test;

/**
 * Abstract parent for password-based Elytron HTTP mechanisms tests.
 *
 * @author Jan Kalina
 */
abstract class PasswordMechTestBase extends AbstractMechTestBase {

    @Test
    public void testSuccess() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", SimpleServlet.RESPONSE_BODY, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testInsufficientRole() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role2"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_FORBIDDEN, statusCode);
            }
        }
    }

    @Test
    public void testInvalidPrincipal() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1wrong", "password1");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
            }
        }
    }

    @Test
    public void testInvalidCredential() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user1", "password1wrong");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build()) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
            }
        }
    }
}
