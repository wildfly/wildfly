/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URISyntaxException;

import io.opentelemetry.api.common.AttributeKey;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(OpenTelemetrySetupTask.class)
@Ignore
public class OpenTelemetryIntegrationTestCase extends BaseOpenTelemetryTest {
    @Deployment
    public static Archive getDeployment() {
        return buildBaseArchive(OpenTelemetryIntegrationTestCase.class.getSimpleName());
    }

    @Test
    public void testServiceNameOverride() throws URISyntaxException {
        try (Client client = ClientBuilder.newClient()) {
            client.target(url.toURI())
                    .request().get();
        }

        AttributeKey<String> key = AttributeKey.stringKey("service.name");
        spanExporter.getFinishedSpanItems(3).forEach(spanData -> {
            Assert.assertEquals(SERVICE_NAME, spanData.getResource().getAttribute(key));
        });
    }
}
