/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.plugin.tools.OperationExecutionException;
import org.wildfly.plugin.tools.server.ServerManager;

@RunWith(Arquillian.class)
@RunAsClient
public class MicrometerRemovalTestCase {
    private static final ModelNode micrometerExtension = Operations.createAddress("extension", "org.wildfly.extension.micrometer");
    private static final ModelNode micrometerSubsystem = Operations.createAddress("subsystem", "micrometer");
    private static final ModelNode ADDRESS_SERVER_LOG_DIR = Operations.createAddress("path", "jboss.server.log.dir");
    private static final String ERROR_MESSAGE = "Failed to publish metrics to OTLP receiver";

    @ContainerResource
    private ServerManager serverManager;
    private String logFilePath;

    @Before
    public void getLogLocation() throws IOException {
        ModelNode op = Operations.createReadAttributeOperation(ADDRESS_SERVER_LOG_DIR, "path");
        logFilePath = serverManager.executeOperation(op).asString();
    }

    @Test
    public void testRemoval() throws Exception {
        ServerLogTailerListener listener = new ServerLogTailerListener();
        try (Tailer ignored = Tailer.builder()
            .setFile(new File(logFilePath, "server.log"))
            .setTailerListener(listener)
            .setDelayDuration(Duration.ofMillis(500))
            .get()) {
            enableMicrometer();
            Thread.sleep(TimeoutUtil.adjust(1000));
            disableMicrometer();
            // Micrometer will push one last time while the registry is shutting down. Sleep long enough to allow that to
            // happen, then clear the log, wait, then check again. The server is configured to push every millisecond, so
            // 500 should be sufficient to give that time without slowing down the test suite more than necessary.
            Thread.sleep(TimeoutUtil.adjust(1000));
            listener.logs.clear();
            Thread.sleep(TimeoutUtil.adjust(1000));
            Assert.assertTrue("Micrometer has been removed, but errors are still being logged.",
                listener.logs.stream().noneMatch(l -> l.contains(ERROR_MESSAGE)));
        } finally {
            disableMicrometer();
        }
    }

    protected void enableMicrometer() throws IOException {
        try {
            if (!resourceExists(micrometerExtension)) {
                serverManager.executeOperation(Operations.createAddOperation(micrometerExtension));
            }
            if (!resourceExists(micrometerSubsystem)) {
                ModelNode addOp = Operations.createAddOperation(micrometerSubsystem);
                addOp.get("endpoint").set("http://localhost:4318/v1/metrics/v1/metrics");
                addOp.get("step").set("1");
                serverManager.executeOperation(addOp);
            }
        } finally {
            serverManager.reloadIfRequired();
        }
    }

    protected void disableMicrometer() throws IOException {
        try {
            if (resourceExists(micrometerSubsystem)) {
                serverManager.executeOperation(Operations.createRemoveOperation(micrometerSubsystem));
            }
            if (resourceExists(micrometerExtension)) {
                serverManager.executeOperation(Operations.createRemoveOperation(micrometerExtension));
            }
        } finally {
            serverManager.reloadIfRequired();
        }
    }

    private boolean resourceExists(ModelNode resourceAddress) throws IOException {
        try {
            serverManager.executeOperation(Operations.createReadResourceOperation(resourceAddress));
            return true;
        } catch (OperationExecutionException e) {
            return false;
        }
    }

    private static class ServerLogTailerListener implements TailerListener {
        List<String> logs = new ArrayList<>();

        @Override
        public void fileNotFound() {
            throw new RuntimeException("Log file not found.");
        }

        @Override
        public void fileRotated() {
            logs.clear();
        }

        @Override
        public void handle(Exception ex) {
            throw new RuntimeException(ex);
        }

        @Override
        public void handle(String line) {
            logs.add(line);
        }

        @Override
        public void init(Tailer tailer) {

        }
    }
}
