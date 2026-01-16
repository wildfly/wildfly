/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService2;

@RunAsClient
public class OpenTelemetryLogsTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_NAME = "otel-logs-test";

    private static final String DEPLOYMENT_SERVICE1 = "service1";
    private static final String DEPLOYMENT_SERVICE2 = "service2";

    private static final String EXPECTED_LOG_ENTRY = "This is a test message: hello";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_SERVICE1, managed = false, testable = false)
    public static WebArchive getDeployment1() {
        return buildBaseArchive(DEPLOYMENT_SERVICE1);
    }

    @Deployment(name = DEPLOYMENT_SERVICE2, managed = false, testable = false)
    public static WebArchive getDeployment2() {
        return buildBaseArchive(DEPLOYMENT_SERVICE2).addClass(OtelService2.class);
    }

    @Test
    @InSequence(1)
    public void deploy() {
        deployer.deploy(DEPLOYMENT_SERVICE1);
        deployer.deploy(DEPLOYMENT_SERVICE2);
    }

    @Test
    @InSequence(2)
    public void testFormattedLogMessage() throws Exception {
        makeRequests(new URL(getDeploymentUrl(DEPLOYMENT_SERVICE1) + "logging/hello"), 1, Response.noContent().build().getStatus());

        server.assertLogs(logEntries ->
                Assert.assertTrue("Missing log entry: '" + EXPECTED_LOG_ENTRY + "'",
                        logEntries.stream().anyMatch(entry ->
                                entry.body().contains(EXPECTED_LOG_ENTRY))));
    }

    @Test
    @InSequence(3)
    public void testDuplicateLogs() {
        var logMessages = server.fetchLogs().stream()
                .filter(log -> log.body().contains(EXPECTED_LOG_ENTRY)).toList();

        Assert.assertEquals("Duplicated log entry found", 1, logMessages.size());
    }
}
