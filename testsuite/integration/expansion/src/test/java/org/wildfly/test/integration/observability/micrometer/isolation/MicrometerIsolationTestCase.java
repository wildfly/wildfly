/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.isolation;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.observability.collector.InMemoryCollector;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@RunWith(Arquillian.class)
@ServerSetup(MicrometerSetupTask.class)
@RunAsClient
public class MicrometerIsolationTestCase {
    private static final String SERVICE_ONE = IsolationResource1.DEPLOYMENT_NAME;
    private static final String SERVICE_TWO = IsolationResource2.DEPLOYMENT_NAME;
    private final InMemoryCollector collector = InMemoryCollector.getInstance();

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

    @Test
    @InSequence
    public void initializeApps() throws MalformedURLException, InterruptedException {
        makeRequests(getDeploymentUrl(SERVICE_ONE));
        makeRequests(getDeploymentUrl(SERVICE_TWO));

        collector.assertMetrics(metrics ->
                Arrays.asList("app1_counter", "app2_counter")
                        .forEach(metric -> assertTrue("Missing metric: " + metric,
                                metrics.stream().anyMatch(m -> m.name().contains(metric)))));

    }

    @Test
    @InSequence(1)
    public void testCounters() throws MalformedURLException {
        testService("counter");
    }

    @Test
    @InSequence(2)
    public void testTimers() throws MalformedURLException {
        testService("timer");
    }

    @Test
    @InSequence(3)
    public void testGauges() throws MalformedURLException {
        testService("gauge");
    }

    @Test
    @InSequence(4)
    public void testSummaries() throws MalformedURLException {
        testService("summary");
    }

    @Test
    @InSequence(5)
    public void testFind() throws MalformedURLException {
        testService("find");
    }

    @Test
    @InSequence(6)
    public void testGetMeters() throws MalformedURLException {
        testService("getMeters");
    }

    @Test
    @InSequence(7)
    public void testForEachMeter() throws MalformedURLException {
        testService("forEachMeter");
    }

    private void testService(String endpoint) throws MalformedURLException {
        List.of(SERVICE_ONE, SERVICE_TWO).forEach(serviceName -> {
            var url = getDeploymentUrl(serviceName) + "/" + endpoint;
            try (Client client = ClientBuilder.newClient()) {
                WebTarget target = client.target(url);
                Response response = target.request().get();
                String body = response.readEntity(String.class);
                Assert.assertEquals("The server returned an error, indicating that metrics are leaking between deployments.",
                        200, response.getStatus());
            }
        });
    }

    private void makeRequests(String url) throws MalformedURLException {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(url);
            for (int i = 0; i < 5; i++) {
                Assert.assertEquals(200, target.request().get().getStatus());
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
}
