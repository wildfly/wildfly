/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.jaeger.JaegerSpan;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.span.AppScopedBean;

@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
@TestcontainersRequired
public class WithSpanTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_NAME = "with-span-test";

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME)
            .addPackage(AppScopedBean.class.getPackage());
    }

    @Test
    public void testWithSpan() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            WebTarget target = client.target(getDeploymentUrl(DEPLOYMENT_NAME) + "/span");
            Response response = target.request().get();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            otelCollector.assertTraces(DEPLOYMENT_NAME + ".war", traces -> {
                Assert.assertFalse(traces.isEmpty());

                Assert.assertTrue(traces.get(0).getSpans().stream()
                    .map(JaegerSpan::getOperationName)
                    .anyMatch("AppScopedBean.getString"::equals)
                );
            });
        }
    }

}
