/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.stabilitylevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD_ENHANCED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STABILITY;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;

/**
 * For tests that need to run under a specific server stability level,
 * the server setup tasks from the inner classes can be used to change the stability level of the server to the desired level.
 * Once the test is done, the original stability level is restored.
 * <p/>
 * In order to not pollute the configuration with XML from a different stability level following the run of the test,
 * it takes a snapshot of the server configuration in the {@code setup()} method, and then restores to that snapshot in
 * the {@code teardown()} method
 */
public class StabilityServerSetupSnapshotRestoreTasks implements ServerSetupTask {
    private static final String SERVER_ENVIRONMENT = "server-environment";

    private final Stability desiredStability;
    private volatile Stability originalStability;

    private AutoCloseable snapshot;


    public StabilityServerSetupSnapshotRestoreTasks(Stability desiredStability) {
        this.desiredStability = desiredStability;
    }

    @Override
    public final void setup(ManagementClient managementClient, String containerId) throws Exception {
        // Make sure the desired stability level is one of the ones supported by the server
        Set<Stability> supportedStabilityLevels = getSupportedStabilityLevels(managementClient);
        Assumptions.assumeThat(supportedStabilityLevels.contains(desiredStability))
                .as("%s is not a supported stability level. Supported levels: %s", desiredStability, supportedStabilityLevels)
                .isTrue();

        // Check the reload-enhanced operation exists in the current stability level
        Assumptions.assumeThat(checkReloadEnhancedOperationIsAvailable(managementClient))
                .as("The reload-enhanced operation is not registered at this stability level")
                .isTrue();

        // Check the reload-enhanced operation exists in the stability level we want to load to so that
        // we can reload back to the current one
        Stability reloadOpStability = getReloadEnhancedOperationStabilityLevel(managementClient);
        Assumptions.assumeThat(desiredStability.enables(reloadOpStability))
                .as("The reload-enhanced operation is not available at the desired stability level")
                .isTrue();

        originalStability = readCurrentStability(managementClient.getControllerClient());

        // Take a snapshot, indicating that when reloading we want to go back to the original stability
        snapshot = takeSnapshot(managementClient, originalStability);

        // We only want to reload to lower stability levels if necessary (e.g. when running the ts.preview tests,
        // container that contains any configuration at 'preview' level the reload to 'community' would fail)
        if (!originalStability.enables(desiredStability)) {
            reloadToDesiredStability(managementClient, desiredStability);
        }

        // Do any additional setup from the subclasses
        doSetup(managementClient);
    }

    protected void doSetup(ManagementClient managementClient) throws Exception {

    }

    @Override
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        tearDown(managementClient);
        snapshot.close();
    }

    public final void tearDown(ManagementClient managementClient) throws Exception {

    }

    private boolean checkReloadEnhancedOperationIsAvailable(ManagementClient managementClient) throws Exception {
        ModelNode op = Util.createOperation(READ_OPERATION_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        for (ModelNode name : result.asList()) {
            if (name.asString().equals(RELOAD_ENHANCED)) {
                return true;
            }
        }
        return false;
    }

    private Stability getReloadEnhancedOperationStabilityLevel(ManagementClient managementClient) throws Exception {
        ModelNode op = Util.createOperation(READ_OPERATION_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        op.get(NAME).set(RELOAD_ENHANCED);

        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        String stability = result.get(STABILITY).asString();
        return Stability.fromString(stability);

    }

    private Set<Stability> getSupportedStabilityLevels(ManagementClient managementClient) throws Exception {
        ModelNode op = Util.getReadAttributeOperation(PathAddress.pathAddress(CORE_SERVICE, SERVER_ENVIRONMENT), "permissible-stability-levels");
        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        Set<Stability> set = new HashSet<>();
        for (ModelNode mn : result.asList()) {
            set.add(Stability.fromString(mn.asString()));
        }
        return set;
    }

    private Stability reloadToDesiredStability(ManagementClient managementClient, Stability stability) throws Exception {
        // Check the stability
        Stability currentStability = readCurrentStability(managementClient.getControllerClient());
        if (currentStability == stability) {
            return originalStability;
        }

        //Reload the server to the desired stability level
        reloadEnhancedAndWaitForCompletion(managementClient, stability, null);

        Stability reloadedStability = readCurrentStability(managementClient.getControllerClient());
        assertThat(reloadedStability).isEqualTo(stability);
        return originalStability;
    }

    private Stability readCurrentStability(ModelControllerClient client) throws Exception {
        ModelNode op = Util.getReadAttributeOperation(PathAddress.pathAddress(CORE_SERVICE, SERVER_ENVIRONMENT), STABILITY);
        ModelNode result = ManagementOperations.executeOperation(client, op);
        return Stability.fromString(result.asString());
    }

    /**
     * A server setup task that sets the server stability to the default level.
     */
    public static class Default extends StabilityServerSetupSnapshotRestoreTasks {
        public Default() {
            super(Stability.DEFAULT);
        }
    }

    /**
     * A server setup task that sets the server stability to the community level.
     */
    public static class Community extends StabilityServerSetupSnapshotRestoreTasks {
        public Community() {
            super(Stability.COMMUNITY);
        }
    }

    /**
     * A server setup task that sets the server stability to the preview level.
     */
    public static class Preview extends StabilityServerSetupSnapshotRestoreTasks {
        public Preview() {
            super(Stability.PREVIEW);
        }
    }

    /**
     * A server setup task that sets the server stability to the experimental level.
     */
    public static class Experimental extends StabilityServerSetupSnapshotRestoreTasks {
        public Experimental() {
            super(Stability.EXPERIMENTAL);
        }
    }

    // Temporary duplication until we have the needed hooks in SnapshotServerSetupTasks to reload to different stability levels
    private static AutoCloseable takeSnapshot(ManagementClient client, Stability reloadToStability) {
        try {
            ModelNode node = new ModelNode();
            node.get(ModelDescriptionConstants.OP).set("take-snapshot");
            ModelNode result = client.getControllerClient().execute(node);
            if (!"success".equals(result.get(ClientConstants.OUTCOME).asString())) {
                Assertions.fail("take-snapshot operation didn't finish successfully: " + result.asString());
            }
            String snapshot = result.get(ModelDescriptionConstants.RESULT).asString();
            final String fileName = snapshot.contains(File.separator) ? snapshot.substring(snapshot.lastIndexOf(File.separator) + 1) : snapshot;
            return new AutoCloseable() {
                @Override
                public void close() throws Exception {
                    if (reloadToStability != null) {
                        reloadEnhancedAndWaitForCompletion(client, reloadToStability, fileName);
                    } else {
                        ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient(), ServerReload.TIMEOUT, false, client.getMgmtAddress(), client.getMgmtPort(), fileName);
                    }

                    ModelNode node1 = new ModelNode();
                    node1.get(ModelDescriptionConstants.OP).set("write-config");
                    ModelNode result1 = client.getControllerClient().execute(node1);
                    if (!"success".equals(result1.get(ClientConstants.OUTCOME).asString())) {
                        Assertions.fail("Failed to write config after restoring from snapshot " + result1.asString());
                    }
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to take snapshot", e);
        }
    }

    // Temporary duplication until org.jboss.as.test.integration.management.util.ServerReload removed hard dependency on legacy JUnit 4
    private static void reloadEnhancedAndWaitForCompletion(ManagementClient client, Stability stability, String serverConfig) {
        ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        operation.get(ModelDescriptionConstants.OP).set(RELOAD_ENHANCED);
        operation.get(STABILITY).set(stability.toString());
        if (serverConfig != null) {
            operation.get("server-config").set(serverConfig);
        }
        try {
            ModelNode result = client.getControllerClient().execute(operation);
            if (!"success".equals(result.get(ClientConstants.OUTCOME).asString())) {
                Assertions.fail("Reload operation didn't finish successfully: " + result.asString());
            }
        } catch (IOException e) {
            final Throwable cause = e.getCause();
            if (!(cause instanceof ExecutionException) && !(cause instanceof CancellationException)) {
                throw new RuntimeException(e);
            }
        }
        ServerReload.waitForLiveServerToReload(ServerReload.TIMEOUT,
                client.getMgmtAddress() != null ? client.getMgmtAddress() : TestSuiteEnvironment.getServerAddress(),
                client.getMgmtPort() > 0 ? client.getMgmtPort() : TestSuiteEnvironment.getServerPort());
    }

}
