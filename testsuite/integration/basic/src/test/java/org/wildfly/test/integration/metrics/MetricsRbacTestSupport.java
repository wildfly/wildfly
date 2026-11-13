/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.management.api.expression.Utils.executeOp;

import java.io.IOException;
import java.util.List;

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
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.RbacRealmSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.dmr.ModelNode;

/** Shared management endpoint and authorization helpers for WildFly Metrics RBAC tests. */
final class MetricsRbacTestSupport {
    private static final String METRICS_CONTEXT = "/metrics";

    /** Prevents instantiation of this test support class. */
    private MetricsRbacTestSupport() {
    }

    /**
     * Fetches and parses the management metrics endpoint, using DIGEST when credentials are supplied.
     *
     * @param managementClient the client that provides the management endpoint address
     * @param username the management-realm user, or {@code null} for an unauthenticated request
     * @param password the user's password, ignored when {@code username} is {@code null}
     * @return parsed Prometheus metrics
     * @throws IOException if the request fails or the endpoint does not return HTTP 200
     */
    static List<PrometheusMetric> fetchMetrics(ManagementClient managementClient, String username, String password)
            throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = scrapeManagement(client, managementClient, username, password)) {
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                throw new IOException("Metrics endpoint returned an unexpected status: " + status);
            }
            return PrometheusMetric.buildPrometheusMetrics(EntityUtils.toString(response.getEntity()));
        }
    }

    /**
     * Fetches the management metrics endpoint's HTTP status without parsing its response body.
     *
     * @param managementClient the client that provides the management endpoint address
     * @param username the management-realm user, or {@code null} for an unauthenticated request
     * @param password the user's password, ignored when {@code username} is {@code null}
     * @return the response status code
     * @throws IOException if the request fails
     */
    static int fetchMetricsStatus(ManagementClient managementClient, String username, String password)
            throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = scrapeManagement(client, managementClient, username, password)) {
            return response.getStatusLine().getStatusCode();
        }
    }

    /**
     * Enables or disables RBAC and Metrics endpoint authentication together, then waits for any required reload.
     *
     * @param managementClient the client used to change and reload the server
     * @param enabled {@code true} to enable RBAC and endpoint authentication; {@code false} to restore simple
     *                authorization and disable endpoint authentication
     * @throws Exception if the management operation or reload fails
     */
    static void setAuthenticationEnabled(ManagementClient managementClient, boolean enabled) throws Exception {
        ModelNode operation = Util.createCompositeOperation(List.of(
                Util.getWriteAttributeOperation(RbacRealmSetupTask.authorization, PROVIDER,
                        enabled ? "rbac" : "simple"),
                Util.getWriteAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, "metrics"), "security-enabled",
                        enabled)));
        executeOp(operation, managementClient.getControllerClient());
        ServerReload.reloadIfRequired(managementClient);
    }

    /**
     * Issues a GET to the management metrics endpoint, adding DIGEST credentials when a username is supplied.
     *
     * @param client the HTTP client used for the request
     * @param managementClient the client that provides the management endpoint address
     * @param username the management-realm user, or {@code null} for an unauthenticated request
     * @param password the user's password, ignored when {@code username} is {@code null}
     * @return the response, which the caller must close
     * @throws IOException if the request fails
     */
    private static CloseableHttpResponse scrapeManagement(CloseableHttpClient client,
            ManagementClient managementClient, String username, String password) throws IOException {
        HttpClientContext context = HttpClientContext.create();
        HttpGet request = new HttpGet(String.format("http://%s:%d%s", managementClient.getMgmtAddress(),
                managementClient.getMgmtPort(), METRICS_CONTEXT));
        if (username != null) {
            CredentialsProvider credentials = new BasicCredentialsProvider();
            credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            context.setCredentialsProvider(credentials);
            request.setConfig(RequestConfig.custom()
                    .setTargetPreferredAuthSchemes(List.of(AuthSchemes.DIGEST))
                    .build());
        }
        return client.execute(request, context);
    }
}
