/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.management.api.expression.Utils.executeOp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.observability.setuptasks.RbacRealmSetupTask;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.metrics.application.TestApplication;
import org.wildfly.test.integration.metrics.application.TestResource;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
        StabilityServerSetupSnapshotRestoreTasks.Community.class,
        RbacRealmSetupTask.class
})
public class MetricsRbacTestCase {
    // Model-sourced (RBAC-affected) Undertow metric that must be non-zero once the deployment has served requests.
    private static final String UNDERTOW_MODEL_METRIC = "wildfly_undertow_bytes_sent";
    private static final int REQUEST_COUNT = 10;
    private static Boolean isMetricsEnabled;

    @ArquillianResource
    protected URL url;
    @ContainerResource
    protected ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class).addClasses(TestApplication.class, TestResource.class);
    }

    // In some CI scenarios, the WildFly Metrics subsystem may not be enabled. In those scenarios, we need to ignore
    // this test rather than failing the build.
    @Before
    public void before() throws Exception {
        if (isMetricsEnabled == null) {
            try {
                executeOp(Util.getReadResourceDescriptionOperation(PathAddress.pathAddress(SUBSYSTEM, "metrics")),
                        managementClient.getControllerClient());
                isMetricsEnabled = true;
            } catch (Exception e) {
                isMetricsEnabled = false;
            }
        }
        Assume.assumeTrue("WildFly Metrics must be enabled for this test to run.", isMetricsEnabled);
    }

    @Test
    public void testWildFlyMetricsWithAuthentication() throws Exception {
        verifyMetricsAccess(true);
    }

    @Test
    public void testWildFlyMetricsWithoutAuthentication() throws Exception {
        verifyMetricsAccess(false);
    }

    /**
     * Negative test: with RBAC and endpoint security enabled, an unauthenticated scrape of the metrics endpoint
     * must be rejected outright rather than served zeroed-out (anonymous) metrics.
     */
    @Test
    public void testWildFlyMetricsUnauthenticatedRejected() throws Exception {
        setAuthenticationEnabled(true);

        Assert.assertEquals("Unauthenticated scrape of the secured metrics endpoint should be rejected",
                401, fetchMetricsStatus(null, null));
    }

    /**
     * Negative test: an authenticated management user without at least the Monitor role must not see the
     * RBAC-protected model metrics. The scrape authenticates, but the Undertow model metric is read under the
     * caller's identity and, lacking read access, comes back absent or zeroed rather than carrying a real value.
     */
    @Test
    public void testWildFlyMetricsWithoutMonitorRole() throws Exception {
        setAuthenticationEnabled(true);
        makeRequests();

        List<PrometheusMetric> metrics =
                fetchMetrics(RbacRealmSetupTask.USER_NOROLE, RbacRealmSetupTask.TEST_RBAC_PASSWORD);

        double value = metrics.stream()
                .filter(metric -> metric.getKey().startsWith(UNDERTOW_MODEL_METRIC))
                .findFirst()
                .map(metric -> Double.parseDouble(metric.getValue()))
                .orElse(0.0);

        Assert.assertEquals("The Undertow model metric '" + UNDERTOW_MODEL_METRIC +
                "' must not be visible to a user without the Monitor role, but was " + value, 0.0, value, 0.0);
    }

    private void verifyMetricsAccess(boolean authenticationEnabled) throws Exception {
        setAuthenticationEnabled(authenticationEnabled);
        makeRequests();

        List<PrometheusMetric> metrics = authenticationEnabled
                ? fetchMetrics(RbacRealmSetupTask.USER_MONITOR, RbacRealmSetupTask.TEST_RBAC_PASSWORD)
                : fetchMetrics(null, null);

        PrometheusMetric undertowMetric = metrics.stream().filter(metric ->
                        metric.getKey().startsWith(UNDERTOW_MODEL_METRIC)).findFirst()
                .orElseThrow(() -> new AssertionError("Undertow model metric not found"));

        double value = Double.parseDouble(undertowMetric.getValue());

        Assert.assertTrue("The Undertow model metric '" + UNDERTOW_MODEL_METRIC +
                "' should be non-zero (authentication " + (authenticationEnabled ? "enabled" : "disabled") +
                "), but was " + value, value > 0);
    }

    protected void makeRequests() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url.toURI() + "metrics-app/hello");
            for (int i = 0; i < REQUEST_COUNT; i++) {
                Response response = target.request().get();
                Assert.assertEquals(200, response.getStatus());
            }
        }
    }

    /**
     * Scrapes the management metrics endpoint.
     */
    protected List<PrometheusMetric> fetchMetrics(String username, String password) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse resp = scrapeManagement(client, username, password);
            return PrometheusMetric.buildPrometheusMetrics(EntityUtils.toString(resp.getEntity()));
        }
    }

    /**
     * Scrapes the management metrics endpoint and returns only the HTTP status code. Used by negative tests that
     * assert an unauthenticated scrape is rejected before any metric body is produced.
     */
    protected int fetchMetricsStatus(String username, String password) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            return scrapeManagement(client, username, password).getStatusLine().getStatusCode();
        }
    }

    /**
     * Issues a GET against {@code http://<mgmt-host>:<mgmt-port>/metrics}. When {@code username} is non-null the request
     * carries the given credentials and is authenticated via DIGEST.
     */
    private CloseableHttpResponse scrapeManagement(CloseableHttpClient client, String username,
                                                   String password) throws IOException {
        HttpClientContext hcContext = HttpClientContext.create();
        HttpGet get = new HttpGet(String.format("http://%s:%d/metrics", managementClient.getMgmtAddress(),
                managementClient.getMgmtPort()));

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

    /**
     * Enables or disables RBAC and, in lockstep, the security of the WildFly Metrics endpoints, then reloads.
     * With authentication enabled, scrapes must present a management-realm identity with at least Monitor access.
     * With it disabled, the endpoints are open.
     */
    private void setAuthenticationEnabled(boolean enabled) throws Exception {
        executeOp(Util.createCompositeOperation(
                        List.of(
                                // /core-service=management/access=authorization:write-attribute(name=provider,value=rbac|simple)
                                Util.getWriteAttributeOperation(RbacRealmSetupTask.authorization, PROVIDER, enabled ? "rbac" : "simple"),
                                // /subsystem=metrics:write-attribute(name=security-enabled,value=...)
                                Util.getWriteAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, "metrics"), "security-enabled", enabled)
                        )),
                managementClient.getControllerClient());

        ServerReload.reloadIfRequired(managementClient);
    }

}
