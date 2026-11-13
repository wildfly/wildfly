/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.metrics;

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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
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
        MetricsRbacTestSupport.setAuthenticationEnabled(managementClient, true);

        Assert.assertEquals("Unauthenticated scrape of the secured metrics endpoint should be rejected",
                401, fetchMetricsStatus(null, null));
    }

    /**
     * Negative test: an authenticated management user without at least the Monitor role must not see the
     * RBAC-protected model metrics, including the metric series labels. The scrape authenticates, but the
     * Undertow model metric is read under the caller's identity and, lacking read access, the whole series is
     * omitted rather than exported with a zero value and address-derived labels.
     */
    @Test
    public void testWildFlyMetricsWithoutMonitorRole() throws Exception {
        MetricsRbacTestSupport.setAuthenticationEnabled(managementClient, true);
        makeRequests();

        List<PrometheusMetric> metrics =
                fetchMetrics(RbacRealmSetupTask.USER_NOROLE, RbacRealmSetupTask.TEST_RBAC_PASSWORD);

        Assert.assertFalse("The Undertow model metric series '" + UNDERTOW_MODEL_METRIC +
                        "' must be omitted for a user without the Monitor role",
                metrics.stream().anyMatch(metric -> metric.getKey().startsWith(UNDERTOW_MODEL_METRIC)));
    }

    private void verifyMetricsAccess(boolean authenticationEnabled) throws Exception {
        MetricsRbacTestSupport.setAuthenticationEnabled(managementClient, authenticationEnabled);
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
        return MetricsRbacTestSupport.fetchMetrics(managementClient, username, password);
    }

    /**
     * Scrapes the management metrics endpoint and returns only the HTTP status code. Used by negative tests that
     * assert an unauthenticated scrape is rejected before any metric body is produced.
     */
    protected int fetchMetricsStatus(String username, String password) throws IOException {
        return MetricsRbacTestSupport.fetchMetricsStatus(managementClient, username, password);
    }

}
