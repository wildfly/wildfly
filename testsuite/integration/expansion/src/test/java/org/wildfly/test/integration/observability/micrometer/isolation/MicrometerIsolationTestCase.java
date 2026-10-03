/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.isolation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.arquillian.testcontainers.api.Testcontainer;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.containers.OpenTelemetryCollectorContainer;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.observability.JaxRsActivator;

/**
 * This test verifies that application metrics are isolated per-deployment, meaning metrics from SERVICE_ONE should not
 * be visible in SERVICE_TWO. This is tested by:
 * - Deploy both services
 * - Make some requests to each to make sure metrics are registered
 * - Make a request to test endpoints in each application
 *   - Endpoints attempts one of the following:
 *     - Read the metric from the other deployment via <code>MeterRegistry.</code> search method
 *       - If the metric is _not_ visible, a <code>MeterNotFoundException</code> will be thrown, resulting in a 500 error
 *     - Create a metric with name colliding with other deployment
 *       - The created metric is verified to have the correct marker tag
 *   - The client-side test verifies that the endpoint returns the 200. This is the expected result.
 *   - A 500 response indicates that one app can see the other's meters, which should not be allowed.
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(MicrometerSetupTask.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestcontainersRequired
@RunAsClient
public class MicrometerIsolationTestCase {
    protected static final String SERVICE_ONE = IsolationResource1.DEPLOYMENT_NAME;
    protected static final String SERVICE_TWO = IsolationResource2.DEPLOYMENT_NAME;

    @Deployment(name = SERVICE_ONE, order = 1, testable = false)
    public static WebArchive createDeployment1() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_ONE + ".war")
                .addClasses(JaxRsActivator.class, AbstractIsolationResource.class, IsolationResource1.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Deployment(name = SERVICE_TWO, order = 2, testable = false)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, SERVICE_TWO + ".war")
                .addClasses(JaxRsActivator.class, AbstractIsolationResource.class, IsolationResource2.class)
                .addAsWebInfResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @Test
    @Order(0)
    void initializeApps() throws Exception {
        makeRequests(getDeploymentUrl(SERVICE_ONE));
        makeRequests(getDeploymentUrl(SERVICE_TWO));

        otelCollector.assertMetrics(metrics ->
                Arrays.asList("app1_counter", "app2_counter")
                        .forEach(metric -> assertTrue(metrics.stream().anyMatch(m -> m.getKey().contains(metric)),
                                "Missing metric: " + metric)));

    }

    private void makeRequests(String url) throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url);
            for (int i = 0; i < 5; i++) {
                assertEquals(200, target.request().get().getStatus());
            }
        }
    }

    protected String getDeploymentUrl(String deploymentName)  {
        try {
            return TestSuiteEnvironment.getHttpUrl() + "/" + deploymentName;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    void counters() throws Exception {
        testService("counter");
    }

    @Test
    @Order(2)
    void timers() throws Exception {
        testService("timer");
    }

    @Test
    @Order(3)
    void gauges() throws Exception {
        testService("gauge");
    }

    @Test
    @Order(4)
    void summaries() throws Exception {
        testService("summary");
    }

    @Test
    @Order(5)
    void find() throws Exception {
        testService("find");
    }

    @Test
    @Order(6)
    void getMeters() throws Exception {
        testService("getMeters");
    }

    @Test
    @Order(7)
    void forEachMeter() throws Exception {
        testService("forEachMeter");
    }

    private void testService(String endpoint) throws MalformedURLException {
        List.of(SERVICE_ONE, SERVICE_TWO).forEach(serviceName -> {
            var url = getDeploymentUrl(serviceName) + "/" + endpoint;
            try (Client client = ClientBuilder.newClient()) {
                WebTarget target = client.target(url);
                Response response = target.request().get();
                String body = response.readEntity(String.class);
                assertEquals(200,
                        response.getStatus(), "The server returned an error, indicating that metrics are leaking between deployments.");
            }
        });
    }
}
