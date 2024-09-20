/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClients;

/**
 * Helper class to start and stop container including a deployment.
 *
 * @author Radoslav Husar
 */
public final class ClusterHttpClientUtil {

    public static void establishTopology(URL baseURL, String container, String cache, Set<String> topology) throws URISyntaxException, IOException {
        establishTopology(baseURL, container, cache, topology, TopologyChangeListener.DEFAULT_TIMEOUT);
    }

    public static void establishTopology(URL baseURL, String container, String cache, Set<String> topology, Duration timeout) throws URISyntaxException, IOException {
        HttpClient client = HttpClients.createDefault();
        try {
            establishTopology(client, baseURL, container, cache, topology, timeout);
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    public static void establishTopology(HttpClient client, URL baseURL, String container, String cache, Set<String> topology, Duration timeout) throws URISyntaxException, IOException {
        URI uri = TopologyChangeListenerServlet.createURI(baseURL, container, cache, topology, timeout);
        HttpResponse response = client.execute(new HttpGet(uri));
        try {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    /**
     * Tries a get on the provided client with default GRACE_TIME_TO_MEMBERSHIP_CHANGE.
     */
    public static HttpResponse tryGet(final HttpClient client, final String url) throws IOException {
        return tryGet(client, url, GRACE_TIME_TO_REPLICATE);
    }

    public static HttpResponse tryGet(final HttpClient client, final HttpUriRequest r) throws IOException {
        final long startTime;
        HttpResponse response = client.execute(r);
        startTime = System.currentTimeMillis();
        while(response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK && startTime + GRACE_TIME_TO_REPLICATE > System.currentTimeMillis()) {
            response = client.execute(r);
        }
        return response;
    }

    /**
     * Tries a get on the provided client with specified graceTime in milliseconds.
     */
    public static HttpResponse tryGet(final HttpClient client, final String url, final long graceTime) throws IOException {
        return tryGet(client, new HttpGet(url));
    }

    /**
     * Tries a get on the provided client consuming the request body.
     */
    public static String tryGetAndConsume(final HttpClient client, final String url) throws IOException {
        // Get the response
        HttpResponse response = tryGet(client, url);

        // Consume it
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8), 4096)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Utility class.
     */
    private ClusterHttpClientUtil() {
    }
}
