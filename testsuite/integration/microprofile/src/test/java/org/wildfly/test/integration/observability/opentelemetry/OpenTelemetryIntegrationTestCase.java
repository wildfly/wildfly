/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.wildfly.test.integration.observability.setuptask.ServiceNameSetupTask.SERVICE_NAME;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.arquillian.testcontainers.api.Testcontainer;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.container.OpenTelemetryCollectorContainer;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;
import org.wildfly.test.integration.observability.setuptask.OpenTelemetrySetupTask;
import org.wildfly.test.integration.observability.setuptask.ServiceNameSetupTask;

@RunWith(Arquillian.class)
@ServerSetup({OpenTelemetrySetupTask.class})
@RunAsClient
@DockerRequired(AssumptionViolatedException.class)
public class OpenTelemetryIntegrationTestCase extends BaseOpenTelemetryTest {
    @Testcontainer
    private OpenTelemetryCollectorContainer otelCollector;

    @ContainerResource
    ManagementClient managementClient;

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive("otelinteg");
    }

    @Test
    @InSequence(1)
    public void setup() throws Exception {
        new ServiceNameSetupTask().setup(managementClient, null);
    }

    @Test
    @InSequence(2)
    public void testServiceNameOverride() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(getDeploymentUrl("otelinteg")).request().get();
            Assert.assertEquals(200, response.getStatus());
        }

        List<JaegerTrace> traces = otelCollector.getTraces(SERVICE_NAME);
        Assert.assertFalse("Traces not found for service", traces.isEmpty());
    }

    @Test
    @InSequence(3)
    public void tearDown() throws Exception {
        new ServiceNameSetupTask().tearDown(managementClient, null);
    }
}
