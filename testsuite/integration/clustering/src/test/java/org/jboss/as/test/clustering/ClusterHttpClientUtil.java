/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.GRACE_TIME_TO_REPLICATE;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

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
 * @version April 2012
 */
public final class ClusterHttpClientUtil {

    public static void establishTopology(URL baseURL, String container, String cache, String... nodes) throws URISyntaxException, IOException {
        establishTopology(baseURL, container, cache, TopologyChangeListener.DEFAULT_TIMEOUT, nodes);
    }

    public static void establishTopology(URL baseURL, String container, String cache, long timeout, String... nodes) throws URISyntaxException, IOException {
        HttpClient client = HttpClients.createDefault();
        try {
            establishTopology(client, baseURL, container, cache, timeout, nodes);
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }

    public static void establishTopology(HttpClient client, URL baseURL, String container, String cache, long timeout, String... nodes)
            throws URISyntaxException, IOException {
        URI uri = TopologyChangeListenerServlet.createURI(baseURL, container, cache, timeout, nodes);
        HttpResponse response = client.execute(new HttpGet(uri));
        try {
            assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    /**
     * Tries a get on the provided client with default GRACE_TIME_TO_MEMBERSHIP_CHANGE.
     *
     * @param client
     * @param url
     * @return HTTP response
     * @throws IOException
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
     *
     * @param client
     * @param url
     * @param graceTime
     * @return
     * @throws IOException
     */
    public static HttpResponse tryGet(final HttpClient client, final String url, final long graceTime) throws IOException {
        return tryGet(client, new HttpGet(url));
    }

    /**
     * Tries a get on the provided client consuming the request body.
     *
     * @param client
     * @param url
     * @return response body as string
     * @throws IOException
     */
    public static String tryGetAndConsume(final HttpClient client, final String url) throws IOException {
        // Get the response
        HttpResponse response = tryGet(client, url);

        // Consume it
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 4096)) {
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
