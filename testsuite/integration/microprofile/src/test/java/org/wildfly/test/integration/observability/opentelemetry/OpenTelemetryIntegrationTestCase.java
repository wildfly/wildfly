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
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.observability.opentelemetry.jaeger.JaegerTrace;
import org.wildfly.test.integration.observability.setuptask.OpenTelemetrySetupTask;
import org.wildfly.test.integration.observability.setuptask.ServiceNameSetupTask;

@RunWith(Arquillian.class)
@ServerSetup({OpenTelemetrySetupTask.class, ServiceNameSetupTask.class})
@RunAsClient
public class OpenTelemetryIntegrationTestCase extends BaseOpenTelemetryTest {
    @Deployment
    public static WebArchive getDeployment() {
        return buildBaseArchive(OpenTelemetryIntegrationTestCase.class.getSimpleName());
    }

    @Test
    public void testServiceNameOverride() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            Response response = client.target(url.toURI()).request().get();
            Assert.assertEquals(200, response.getStatus());
        }

        List<JaegerTrace> traces = otelContainer.getTraces(SERVICE_NAME);
        Assert.assertFalse("Traces not found for service", traces.isEmpty());
    }
}
