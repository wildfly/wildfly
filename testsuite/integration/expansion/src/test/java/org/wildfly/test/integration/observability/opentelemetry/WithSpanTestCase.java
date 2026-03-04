/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.test.shared.observability.signals.trace.SimpleSpan;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.span.AppScopedBean;

@RunAsClient
public class WithSpanTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_NAME = "with-span-test";

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

            server.assertSpans(spans ->
                    Assert.assertTrue(spans.stream().map(SimpleSpan::name)
                            .anyMatch("AppScopedBean.getString"::equals)
                    ));
        }
    }
}
