/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
