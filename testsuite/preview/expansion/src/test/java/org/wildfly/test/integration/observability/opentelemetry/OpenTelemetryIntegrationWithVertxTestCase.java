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
 * A test on opentelemetry to use the Vertx instance defined in the vertx subsystem.
 * There will be a server log to indicate the usage.
 */
@RunWith(Arquillian.class)
@ServerSetup({OpenTelemetryWithCollectorSetupTask.class, OpenTelemetryIntegrationWithVertxTestCase.LoggingWithVertxServerSetupTask.class, VertxSubsystemSetupTask.class})
@RunAsClient
@TestcontainersRequired
public class OpenTelemetryIntegrationWithVertxTestCase extends AbstractOpenTelemetryIntegrationTest {

    private static final String WITH_VERTX_SMALLRYE_OPENTELEMETRY_LOG_FILE = "smallrye-opentelemetry-with-vertx.log";
    private static final String WITH_VERTX_VERTX_FEATURE_PACK_LOG_FILE = "vertx-feature-pack-with-vertx.log";
    public static class LoggingWithVertxServerSetupTask extends LoggingServerSetupTask {
        public LoggingWithVertxServerSetupTask() {
            super(WITH_VERTX_SMALLRYE_OPENTELEMETRY_LOG_FILE, WITH_VERTX_VERTX_FEATURE_PACK_LOG_FILE);
        }
    }

    @Deployment(testable = false)
    public static WebArchive getDeployment() {
        return buildBaseArchive("otelinteg-with-vertx");
    }

    @Test
    public void testVertxUsageInLog() throws Exception {
        requestOpenTelemetryTrace("otelinteg-with-vertx.war");
        String logsInSmalleRyeOpentelemetry = retrieveServerLog(managementClient, WITH_VERTX_SMALLRYE_OPENTELEMETRY_LOG_FILE);
        Assert.assertFalse("It won't create Vertx when vertx subsystem is available", logsInSmalleRyeOpentelemetry.contains("Create a new Vertx instance"));
        String logsInVertxSubsystem = retrieveServerLog(managementClient, WITH_VERTX_VERTX_FEATURE_PACK_LOG_FILE);
        Assert.assertTrue("Should use Vertx instance from vertx subsystem", logsInVertxSubsystem.contains("WFLYVTX0008"));
    }
}
