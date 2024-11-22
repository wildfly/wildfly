/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.stabilitylevel;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELOAD_ENHANCED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STABILITY;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Assume;

/**
 * For tests that need to run under a specific server stability level,
 * the server setup tasks from the inner classes can be used to change the stability level of the server to the desired level.
 * Once the test is done, the original stability level is restored.
 *
 * In order to not pollute the configuration with XML from a different stability level following the run of the test,
 * it takes a snapshot of the server configuration in the {@code setup()} method, and then restores to that snapshot in
 * the {@code teardown()} method
 */
public abstract class StabilityServerSetupSnapshotRestoreTasks implements ServerSetupTask {
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
        Assume.assumeTrue(
                String.format("%s is not a supported stability level. Supported levels: %s", desiredStability, supportedStabilityLevels),
                supportedStabilityLevels.contains(desiredStability));

        // Check the reload-enhanced operation exists in the current stability level
        Assume.assumeTrue(
                "The reload-enhanced operation is not registered at this stability level",
                checkReloadEnhancedOperationIsAvailable(managementClient));

        // Check the reload-enhanced operation exists in the stability level we want to load to so that
        // we can reload back to the current one
        Stability reloadOpStability = getReloadEnhancedOperationStabilityLevel(managementClient);
        Assume.assumeTrue(desiredStability.enables(reloadOpStability));


        originalStability = readCurrentStability(managementClient.getControllerClient());

        // We only want to reload to lower stability levels (e.g. when running the ts.preview tests, that contains
        // some configuration at 'preview' level, so the reload to 'community' fails
        Assume.assumeTrue(desiredStability.enables(originalStability));

        // Take a snapshot, indicating that when reloading we want to go back to the original stability
        snapshot = takeSnapshot(managementClient, originalStability);

        // All good, let's do it!
        reloadToDesiredStability(managementClient.getControllerClient(), desiredStability);

        // Do any additional setup from the sub-classes
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

    private Stability reloadToDesiredStability(ModelControllerClient client, Stability stability) throws Exception {
        // Check the stability
        Stability currentStability = readCurrentStability(client);
        if (currentStability == stability) {
            return originalStability;
        }

        //Reload the server to the desired stability level
        ServerReload.Parameters parameters = new ServerReload.Parameters()
                .setStability(stability);
        // Execute the reload
        ServerReload.executeReloadAndWaitForCompletion(client, parameters);

        Stability reloadedStability = readCurrentStability(client);
        Assert.assertEquals(stability, reloadedStability);
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
                fail("take-snapshot operation didn't finish successfully: " + result.asString());
            }
            String snapshot = result.get(ModelDescriptionConstants.RESULT).asString();
            final String fileName = snapshot.contains(File.separator) ? snapshot.substring(snapshot.lastIndexOf(File.separator) + 1) : snapshot;
            return new AutoCloseable() {
                @Override
                public void close() throws Exception {
                    ServerReload.Parameters parameters = new ServerReload.Parameters();
                    parameters.setServerConfig(fileName);
                    if (reloadToStability != null) {
                        parameters.setStability(reloadToStability);
                    }
                    ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient(), parameters);

                    ModelNode node = new ModelNode();
                    node.get(ModelDescriptionConstants.OP).set("write-config");
                    ModelNode result = client.getControllerClient().execute(node);
                    if (!"success".equals(result.get(ClientConstants.OUTCOME).asString())) {
                        fail("Failed to write config after restoring from snapshot " + result.asString());
                    }
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to take snapshot", e);
        }
    }
}
