/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import static org.wildfly.test.integration.observability.setuptask.ServiceNameSetupTask.SERVICE_NAME;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.setuptask.ServiceNameSetupTask;

/** Verifies subsystem and deployment service-name configuration for exported traces. */
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, ServiceNameSetupTask.class})
@RunAsClient
@TestcontainersRequired
public class OpenTelemetryIntegrationTestCase extends BaseOpenTelemetryTest {
    private static final String SERVER_CONFIG_DEPLOYMENT = "otelinteg-server-config";
    private static final String DEPLOYMENT_CONFIG_DEPLOYMENT = "otelinteg-deployment-config";
    private static final String DEPLOYMENT_SERVICE_NAME = "deployment-service-name";

    /** Creates the deployment that inherits the subsystem service name. */
    @Deployment(name = SERVER_CONFIG_DEPLOYMENT, order = 1, testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive(SERVER_CONFIG_DEPLOYMENT);
    }

    /** Creates the deployment that overrides the subsystem service name. */
    @Deployment(name = DEPLOYMENT_CONFIG_DEPLOYMENT, order = 2, testable = false)
    public static WebArchive getDeploymentWithServiceName() {
        return buildBaseArchive(DEPLOYMENT_CONFIG_DEPLOYMENT, DEPLOYMENT_SERVICE_NAME);
    }

    /** Verifies traces use the subsystem-configured service name by default. */
    @Test
    @InSequence(1)
    @OperateOnDeployment(SERVER_CONFIG_DEPLOYMENT)
    public void testSubsystemServiceNameOverride() throws Exception {
        try (Client client = ClientBuilder.newClient();
                Response response = client.target(getDeploymentUrl(SERVER_CONFIG_DEPLOYMENT)).request().get()) {
            Assert.assertEquals("Request to the server-config deployment should return HTTP 200", 200,
                    response.getStatus());
        }

        otelCollector.assertTraces(SERVICE_NAME, traces -> Assert.assertFalse("Traces not found for service", traces.isEmpty()));
    }

    /** Verifies deployment configuration overrides the subsystem service name without disabling exports. */
    @Test
    @InSequence(2)
    @OperateOnDeployment(DEPLOYMENT_CONFIG_DEPLOYMENT)
    public void testDeploymentServiceNameOverride() throws Exception {
        try (Client client = ClientBuilder.newClient();
                Response response = client.target(getDeploymentUrl(DEPLOYMENT_CONFIG_DEPLOYMENT)).request().get()) {
            Assert.assertEquals("Request to the deployment-config deployment should return HTTP 200", 200,
                    response.getStatus());
        }

        otelCollector.assertTraces(DEPLOYMENT_SERVICE_NAME,
                traces -> Assert.assertFalse("Traces not found for deployment service", traces.isEmpty()));
    }
}
