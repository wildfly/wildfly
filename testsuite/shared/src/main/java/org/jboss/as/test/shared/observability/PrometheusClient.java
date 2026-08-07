/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;

public class PrometheusClient {
    private final String prometheusUrl;
    private final String userName;
    private final String password;

    public PrometheusClient(String prometheusUrl) {
        this(prometheusUrl, null, null);
    }

    public PrometheusClient(String prometheusUrl, String userName, String password) {
        this.prometheusUrl = prometheusUrl;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Fetches a current snapshot of the metrics from the Prometheus endpoint.
     *
     * @return list of prometheus metrics
     */
    public List<PrometheusMetric> fetchMetrics() throws HttpResponseException {
        return fetchMetrics(false);
    }

    public List<PrometheusMetric> fetchMetrics(boolean authenticate) throws HttpResponseException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpClientContext hcContext = HttpClientContext.create();

            if (authenticate) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
                hcContext.setCredentialsProvider(credentialsProvider);
            }

            try (CloseableHttpResponse resp = client.execute(new HttpGet(prometheusUrl), hcContext)) {
                int statusCode = resp.getStatusLine().getStatusCode();

                if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
                    throw new HttpResponseException(statusCode, resp.getStatusLine().getReasonPhrase());
                }

                return buildPrometheusMetrics(EntityUtils.toString(resp.getEntity()));
            }
        } catch (HttpResponseException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<PrometheusMetric> buildPrometheusMetrics(String body) {
        if (body.isEmpty()) {
            return Collections.emptyList();
        }
        String[] entries = body.split("\n");
        Map<String, String> help = new HashMap<>();
        Map<String, String> type = new HashMap<>();
        List<PrometheusMetric> metrics = new ArrayList<>();
        Arrays.stream(entries).forEach(e -> {
            if (e.startsWith("# HELP")) {
                extractMetadata(help, e);
            } else if (e.startsWith("# TYPE")) {
                extractMetadata(type, e);
            } else {
                if (e.matches(".+\\{.*\\}.+")) {
                    String[] parts = e.split("[{}]");
                    String key = parts[0];
                    Map<String, String> tags = Arrays.stream(parts[1].split(","))
                                    .map(t -> t.split("="))
                                    .collect(Collectors.toMap(i -> i[0],
                                            i -> i[1]
                                                    .replaceAll("^\"", "")
                                                    .replaceAll("\"$", "")
                                    ));
                    metrics.add(new PrometheusMetric(key, tags, parts[2].trim(), type.get(key), help.get(key)));
                } else {
                    String[] parts = e.split(" ");
                    String key = parts[0];
                    metrics.add(new PrometheusMetric(key, Map.of(), parts[1].trim(), type.get(key), help.get(key)));
                }
            }
        });

        return metrics;
    }

    // This method extracts metadata from the HELP and TYPE entries in the Prometheus response. For example, the inputs
    // # HELP buffer_pool_used_memory_bytes The memory used by the NIO pool
    // # TYPE buffer_pool_used_memory_bytes gauge
    // create the map entries "buffer_pool_used_memory_bytes"->"The memory used by the NIO pool" and
    // "buffer_pool_used_memory_bytes"->"gauge". The target map is determined by the calling code, keeping HELP and TYPE
    // data separate, to be used in building the PrometheusMetric objects later
    private void extractMetadata(Map<String, String> target, String source) {
        String[] parts = source.split(" ");
        target.put(parts[2],
                Arrays.stream(Arrays.copyOfRange(parts, 3, parts.length))
                        .reduce("", (total, element) -> total + " " + element));
    }
}
