/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Utility class for establishing cluster topology via {@link TopologyChangeListenerServlet}.
 *
 * @author Radoslav Husar
 */
public final class TopologyChangeListenerUtil {

    public static void establishTopology(URL baseURL, String container, String cache, Set<String> topology) throws URISyntaxException, IOException {
        establishTopology(baseURL, container, cache, topology, TopologyChangeListener.DEFAULT_TIMEOUT);
    }

    public static void establishTopology(URL baseURL, String container, String cache, Set<String> topology, Duration timeout) throws URISyntaxException, IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            establishTopology(client, baseURL, container, cache, topology, timeout);
        }
    }

    public static void establishTopology(HttpClient client, URL baseURL, String container, String cache, Set<String> topology, Duration timeout) throws URISyntaxException, IOException {
        URI uri = TopologyChangeListenerServlet.createURI(baseURL, container, cache, topology, timeout);
        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(new HttpGet(uri))) {
            assertEquals(String.format("Failed to establish topology %s for container=%s andcache=%s within %s", topology, container, cache, timeout), HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        }
    }

    private TopologyChangeListenerUtil() {
        // Utility class.
    }
}
