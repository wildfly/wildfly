/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import java.net.URL;
import java.util.List;

import jakarta.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.as.test.shared.observability.signals.logs.OpenTelemetryLogRecord;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.test.integration.observability.opentelemetry.application.OtelService2;

/** Verifies deployment log routing, resource identity, and duplicate suppression. */
@RunAsClient
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class})
public class OpenTelemetryLogsTestCase extends BaseOpenTelemetryTest {
    private static final String DEPLOYMENT_SERVICE1 = "service1";
    private static final String DEPLOYMENT_SERVICE2 = "service2";
    private static final String DEPLOYMENT_SERVICE_NAME = "service1-log-service";

    private static final String EXPECTED_LOG_ENTRY = "This is a test message: hello";

    @ArquillianResource
    private Deployer deployer;

    /** Creates the deployment with a deployment-specific service name. */
    @Deployment(name = DEPLOYMENT_SERVICE1, managed = false, testable = false)
    public static WebArchive getDeployment1() {
        return buildBaseArchive(DEPLOYMENT_SERVICE1, DEPLOYMENT_SERVICE_NAME);
    }

    /** Creates the deployment that uses the subsystem service name. */
    @Deployment(name = DEPLOYMENT_SERVICE2, managed = false, testable = false)
    public static WebArchive getDeployment2() {
        return buildBaseArchive(DEPLOYMENT_SERVICE2).addClass(OtelService2.class);
    }

    /** Deploys both applications used by the log-routing assertions. */
    @Test
    @InSequence(1)
    public void deploy() {
        deployer.deploy(DEPLOYMENT_SERVICE1);
        deployer.deploy(DEPLOYMENT_SERVICE2);
    }

    /** Verifies a formatted application log carries its deployment resource attributes. */
    @Test
    @InSequence(2)
    public void testFormattedLogMessage() throws Exception {
        makeRequests(new URL(getDeploymentUrl(DEPLOYMENT_SERVICE1) + "logging/hello"), 1,
                Response.Status.NO_CONTENT.getStatusCode());

        otelCollector.assertOpenTelemetryLogs(logEntries ->
                Assert.assertTrue("Missing deployment log entry: '" + EXPECTED_LOG_ENTRY + "'",
                        logEntries.stream().anyMatch(entry ->
                                entry.body().contains(EXPECTED_LOG_ENTRY)
                                        && DEPLOYMENT_SERVICE_NAME.equals(
                                                entry.resourceAttributes().get("service.name")))));
    }

    /** Verifies the routing handler exports one copy of each deployment log record. */
    @Test
    @InSequence(3)
    public void testDuplicateLogs() {
        List<OpenTelemetryLogRecord> logMessages = otelCollector.getOpenTelemetryLogs().stream()
                .filter(log -> log.body().contains(EXPECTED_LOG_ENTRY)).toList();

        Assert.assertEquals("Duplicated log entry found", 1, logMessages.size());
    }
}
