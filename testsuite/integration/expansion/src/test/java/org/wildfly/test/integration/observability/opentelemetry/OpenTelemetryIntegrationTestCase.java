/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.util.Objects;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.setuptask.ServiceNameSetupTask;

@ServerSetup({OpenTelemetrySetupTask.class, ServiceNameSetupTask.class})
@RunAsClient
public class OpenTelemetryIntegrationTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_NAME = "otelinteg";

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    @Test
    public void testServiceNameOverride() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(getDeploymentUrl(DEPLOYMENT_NAME)).request().get();
            Assert.assertEquals(200, response.getStatus());
        }

        server.assertSpans(spans -> {
            var list = spans.stream().filter(s ->
                            Objects.equals(s.resourceAttributes().get("service.name"), ServiceNameSetupTask.SERVICE_NAME))
                    .toList();
            Assert.assertFalse("Spans not found for service", list.isEmpty());
        });
    }
}
