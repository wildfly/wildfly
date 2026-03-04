/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.nio.file.Files;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.logging.LoggingUtil;
import org.jboss.as.test.shared.observability.setuptasks.MicrometerSetupTask;
import org.jboss.as.test.shared.observability.setuptasks.OpenTelemetrySetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test examines the logging produced when multiple metrics systems are enabled. The first subsystem will log nothing,
 * but each subsequent system should log an INFO message alerting the user of the duplication. The number of messages logged
 * is the number of metrics subsystems enabled minus 1.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleMetricsTestCase {
    private static final String MESSAGE_PREAMBLE = "Additional metrics systems discovered";

    private static final ModelNode ADDRESS_ROOT_LOGGER = Operations.createAddress("subsystem", "logging", "root-logger", "ROOT");

    private static final ModelNode ADDRESS_METRICS_EXTENSION = Operations.createAddress("extension", "org.wildfly.extension.metrics");
    private static final ModelNode ADDRESS_METRICS_SUBSYSTEM = Operations.createAddress("subsystem", "metrics");

    private static final ModelNode ADDRESS_MICROMETER_EXTENSION = MicrometerSetupTask.MICROMETER_EXTENSION;
    private static final ModelNode ADDRESS_MICROMETER_SUBSYSTEM = MicrometerSetupTask.MICROMETER_SUBSYSTEM;

    private static final ModelNode ADDRESS_OPENTELEMETRY_EXTENSION = OpenTelemetrySetupTask.OPENTELEMETRY_EXTENSION;
    private static final ModelNode ADDRESS_OPENTELEMETRY_SUBSYSTEM = OpenTelemetrySetupTask.OPENTELEMETRY_ADDRESS;

    private static final ModelNode ADDRESS_MPTELEMETRY_EXTENSION = Operations.createAddress("extension", "org.wildfly.extension.microprofile.telemetry");
    private static final ModelNode ADDRESS_MPTELEMETRY_SUBSYSTEM = Operations.createAddress("subsystem", "microprofile-telemetry");

    @ContainerResource
    private ManagementClient managementClient;

    // WildFly Metrics is not enabled for bootable jars, so that changes the number of messages logged.
    private boolean wildflyMetricsAvailable = true;
    private boolean micrometerAvailable = true;
    private boolean opentelemetryAvailable = true;
    private boolean mptelemetryAvailable = true;

    @Test
    @InSequence
    public void setup() throws Exception {
        wildflyMetricsAvailable = canBeRead(ADDRESS_METRICS_SUBSYSTEM);
        micrometerAvailable = canBeRead(ADDRESS_MICROMETER_SUBSYSTEM);
        opentelemetryAvailable = canBeRead(ADDRESS_OPENTELEMETRY_SUBSYSTEM);
        mptelemetryAvailable = canBeRead(ADDRESS_MPTELEMETRY_SUBSYSTEM);
    }

    @Test
    @InSequence(1)
    public void testWildFlyMetrics() throws Exception {
        String loggerName = "wildfly-metrics";
        try {
            disableMicrometer();
            disableOpenTelemetry();
            addLogHandler(loggerName);
            reloadServer();

            assertExpectedCount(loggerName, 0);
        } finally {
            removeLogHandler(loggerName);
        }
    }

    @Test
    @InSequence(2)
    public void testMicrometer() throws Exception {
        String loggerName = "micrometer";
        try {
            disableOpenTelemetry();
            enableMicrometer();
            addLogHandler(loggerName);
            reloadServer();

            assertExpectedCount(loggerName, wildflyMetricsAvailable ? 1 : 0);
        } finally {
            removeLogHandler(loggerName);
        }
    }

    @Test
    @InSequence(3)
    public void testOpenTelemetry() throws Exception {
        String loggerName = "opentelemetry";
        try {
            addLogHandler(loggerName);
            disableMicrometer();
            enableOpenTelemetry();
            reloadServer();

            assertExpectedCount(loggerName, wildflyMetricsAvailable ? 1 : 0);
        } finally {
            removeLogHandler(loggerName);
        }
    }

    @Test
    @InSequence(4)
    public void testAll() throws Exception {
        String loggerName = "all-metrics";
        try {
            addLogHandler(loggerName);
            enableMicrometer();
            enableOpenTelemetry();
            reloadServer();

            assertExpectedCount(loggerName, wildflyMetricsAvailable ? 2 : 1);
        } finally {
            removeLogHandler(loggerName);
        }
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    public void shutdown() {
        restoreSubsystemState (micrometerAvailable, ADDRESS_MICROMETER_EXTENSION, ADDRESS_MICROMETER_SUBSYSTEM);
        restoreSubsystemState (opentelemetryAvailable, ADDRESS_OPENTELEMETRY_EXTENSION, ADDRESS_OPENTELEMETRY_SUBSYSTEM);
        restoreSubsystemState (mptelemetryAvailable, ADDRESS_MPTELEMETRY_EXTENSION, ADDRESS_MPTELEMETRY_SUBSYSTEM);
        restoreSubsystemState(wildflyMetricsAvailable, ADDRESS_METRICS_EXTENSION, ADDRESS_METRICS_SUBSYSTEM);
        reloadServer();
    }

    private void restoreSubsystemState(boolean presentAtStart, ModelNode extensionAddress, ModelNode subsystemAddress) {
        if (presentAtStart) {
            safeAdd(extensionAddress);
            safeAdd(subsystemAddress);
        } else {
            safeRemove(subsystemAddress);
            safeRemove(extensionAddress);
        }
    }

    private void assertExpectedCount(String loggerName, int expected) throws Exception {
        try (var lines = Files.lines(LoggingUtil.getLogPath(managementClient.getControllerClient(),
                "file-handler", loggerName))) {

            Assert.assertEquals("The list was expected not to be empty", expected,
                    lines.filter(line -> line.contains(MESSAGE_PREAMBLE)).count());
        }
    }

    private void reloadServer() {
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private void addLogHandler(String loggerName) {
        final Operations.CompositeOperationBuilder builder = Operations.CompositeOperationBuilder.create();

        //add handler
        final ModelNode handlerAddress = Operations.createAddress(SUBSYSTEM, "logging", "file-handler", loggerName);
        ModelNode addTestLogOp = Operations.createAddOperation(handlerAddress);
        addTestLogOp.get("append").set("false");
        ModelNode file = new ModelNode();
        file.get("relative-to").set("jboss.server.log.dir");
        file.get("path").set(loggerName + ".log");
        addTestLogOp.get("file").set(file);
        addTestLogOp.get("autoflush").set("true");
        addTestLogOp.get("suffix").set(".yyyy-MM-dd");
        addTestLogOp.get("formatter").set("%-5p [%c] (%t) %s%e%n");
        builder.addStep(addTestLogOp);

        executeOp(managementClient, builder.build());

        final ModelNode addHandlerOp = Operations.createOperation("add-handler", ADDRESS_ROOT_LOGGER);
        addHandlerOp.get("name").set(loggerName);
        executeOp(managementClient, addHandlerOp);
    }

    private void removeLogHandler(String loggerName) {
        final ModelNode removeHandlerOp = Operations.createOperation("remove-handler", ADDRESS_ROOT_LOGGER);
        removeHandlerOp.get("name").set(loggerName);
        executeOp(managementClient, removeHandlerOp);

        final ModelNode handlerAddress = Operations.createAddress(SUBSYSTEM, "logging", "file-handler", loggerName);
        executeOp(managementClient, Operations.createRemoveOperation(handlerAddress));
    }

    private void enableMicrometer() {
        safeAdd(ADDRESS_MICROMETER_EXTENSION);
        safeAdd(ADDRESS_MICROMETER_SUBSYSTEM);
    }

    private void disableMicrometer() {
        safeRemove(ADDRESS_MICROMETER_SUBSYSTEM);
        safeRemove(ADDRESS_MICROMETER_EXTENSION);
    }

    private void enableOpenTelemetry() {
        safeAdd(ADDRESS_OPENTELEMETRY_EXTENSION);
        safeAdd(ADDRESS_OPENTELEMETRY_SUBSYSTEM);
    }

    private void disableOpenTelemetry() {
        safeRemove(ADDRESS_MPTELEMETRY_SUBSYSTEM);
        safeRemove(ADDRESS_MPTELEMETRY_EXTENSION);
        safeRemove(ADDRESS_OPENTELEMETRY_SUBSYSTEM);
        safeRemove(ADDRESS_OPENTELEMETRY_EXTENSION);
    }

    private void safeAdd(ModelNode address) {
        if (!canBeRead(address)) {
            executeOp(managementClient, Operations.createAddOperation(address));
        }
    }

    private void safeRemove(ModelNode address) {
        if (canBeRead(address)) {
            executeOp(managementClient, Operations.createRemoveOperation(address));
        }
    }

    private boolean canBeRead(ModelNode address) {
        return Operations.isSuccessfulOutcome(executeRead(managementClient, address));
    }

    private void executeOp(final ManagementClient client, final ModelNode op) {
        executeOp(client, Operation.Factory.create(op));
    }

    private void executeOp(final ManagementClient client, final Operation op) {
        final ModelNode result;
        try {
            result = client.getControllerClient().execute(op);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException("Failed to execute operation: " + Operations.getFailureDescription(result)
                    .asString());
        }
    }

    public ModelNode executeRead(final ManagementClient managementClient, ModelNode address) {
        try {
            return managementClient.getControllerClient().execute(Operations.createReadResourceOperation(address));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
