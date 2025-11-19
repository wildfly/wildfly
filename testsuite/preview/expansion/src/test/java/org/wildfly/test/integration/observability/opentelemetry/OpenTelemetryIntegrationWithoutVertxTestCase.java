/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.opentelemetry;

import org.arquillian.testcontainers.api.TestcontainersRequired;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetryWithCollectorSetupTask;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test on opentelemetry that is not using the Vertx instance defined in the vertx subsystem.
 * There will be a server log to indicate a Vertx instance is created by smallerye-opentelemetry.
 */
@RunWith(Arquillian.class)
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, LoggingServerSetupTask.class})
@RunAsClient
@TestcontainersRequired
public class OpenTelemetryIntegrationWithoutVertxTestCase extends AbstractOpenTelemetryIntegrationTest {

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive("otelinteg-without-vertx");
    }

    @Test
    public void testVertxUsageInLog() throws Exception {
        requestOpenTelemetryTrace("otelinteg-without-vertx.war");
        String logsInSmalleRyeOpentelemetry = retrieveServerLog(managementClient, LoggingServerSetupTask.SMALLRYE_OPENTELEMETRY_LOG_FILE);
        Assert.assertTrue("It should create Vertx when vertx subsystem is not available", logsInSmalleRyeOpentelemetry.contains("Create a new Vertx instance"));
        String logsInVertxSubsystem = retrieveServerLog(managementClient, LoggingServerSetupTask.VERTX_FEATURE_PACK_LOG_FILE);
        Assert.assertFalse("Should not use Vertx instance from vertx subsystem", logsInVertxSubsystem.contains("WFLYVTX0008"));
    }

}
