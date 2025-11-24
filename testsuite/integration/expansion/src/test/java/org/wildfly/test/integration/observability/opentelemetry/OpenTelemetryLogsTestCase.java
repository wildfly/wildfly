/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;

@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
public class OpenTelemetryLogsTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_NAME = "otel-logs-test";

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return buildBaseArchive(DEPLOYMENT_NAME);
    }

    @Test
    public void testLogs() throws Exception {
        makeRequests(new URL(url, "logging/hello"), 1, Response.noContent().build().getStatus());

        String expectedLogEntry = "This is a test message: hello";
        otelCollector.assertOpenTelemetryLogs(logEntries ->
                Assert.assertTrue("Missing log entry: '" + expectedLogEntry + "'",
                        logEntries.stream().anyMatch(entry ->
                                entry.body().contains(expectedLogEntry))));
    }
}
