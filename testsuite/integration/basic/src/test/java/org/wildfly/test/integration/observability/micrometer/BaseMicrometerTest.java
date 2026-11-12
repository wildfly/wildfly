/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import static org.wildfly.test.integration.observability.setuptask.PrometheusSetupTask.PROMETHEUS_CONTEXT;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;

/**
 * Common scaffolding for the client-side Micrometer tests: a deployment URL, a management client, the OpenTelemetry
 * collector container, and the helpers used to drive traffic and scrape the Prometheus endpoint. Subclasses supply their
 * own {@code @ServerSetup} since the required setup tasks differ per test.
 */
@RunWith(Arquillian.class)
@TestcontainersRequired
@RunAsClient
public abstract class BaseMicrometerTest {
    protected static final int REQUEST_COUNT = 5;

    @ArquillianResource
    protected URL url;

    @ContainerResource
    protected ManagementClient managementClient;

    @Testcontainer
    protected OpenTelemetryCollectorContainer otelCollector;

    protected void executeOperation(final ModelNode op) throws IOException {
        final ModelNode result = managementClient.getControllerClient().execute(Operation.Factory.create(op));
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to execute operation: " + Operations.getFailureDescription(result)
                    .asString());
        }
    }

    protected void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI());
            for (int i = 0; i < REQUEST_COUNT; i++) {
                target.request().get();
            }
        }
    }

    protected List<PrometheusMetric> fetchPrometheusMetrics(boolean authenticate) throws IOException {
        return authenticate
                ? fetchPrometheusMetrics("testSuite", "testSuitePassword")
                : fetchPrometheusMetrics(null, null);
    }

    /**
     * Scrapes the management Prometheus endpoint. When {@code username} is non-null the request is authenticated with the
     * supplied credentials; otherwise it is sent unauthenticated.
     */
    protected List<PrometheusMetric> fetchPrometheusMetrics(String username, String password) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse resp = scrapeManagement(client, PROMETHEUS_CONTEXT, username, password);
            return PrometheusMetric.buildPrometheusMetrics(EntityUtils.toString(resp.getEntity()));
        }
    }

    /**
     * Scrapes the management Prometheus endpoint and returns only the HTTP status code. Used by negative tests that
     * assert an unauthenticated scrape is rejected before any metric body is produced.
     */
    protected int fetchPrometheusStatus(String username, String password) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            return scrapeManagement(client, PROMETHEUS_CONTEXT, username, password).getStatusLine().getStatusCode();
        }
    }

    /**
     * Issues a GET against {@code http://<mgmt-host>:<mgmt-port><context>}. When {@code username} is non-null the request
     * carries the given credentials and is forced to the DIGEST scheme - the one the management HTTP interface challenges
     * with - rather than falling back to (and leaking) Basic.
     */
    private CloseableHttpResponse scrapeManagement(CloseableHttpClient client, String context, String username,
                                                   String password) throws IOException {
        HttpClientContext hcContext = HttpClientContext.create();
        HttpGet get = new HttpGet(String.format("http://%s:%d/%s", managementClient.getMgmtAddress(),
                managementClient.getMgmtPort(), context));

        if (username != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            hcContext.setCredentialsProvider(credentialsProvider);
            get.setConfig(RequestConfig.custom()
                    .setTargetPreferredAuthSchemes(List.of(AuthSchemes.DIGEST))
                    .build());
        }

        return client.execute(get, hcContext);
    }
}
