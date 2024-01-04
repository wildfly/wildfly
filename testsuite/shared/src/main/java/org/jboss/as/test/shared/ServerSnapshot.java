/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import static org.junit.Assert.fail;

import java.io.File;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Class that can be used to take a restore server snapshots
 */
public class ServerSnapshot {

    /**
     * Takes a snapshot of the current state of the server.
     *
     * Returns a AutoCloseable that can be used to restore the server state
     * @param client The client
     * @return A closeable that can be used to restore the server
     */
    public static AutoCloseable takeSnapshot(ManagementClient client) {
        try {
            ModelNode node = new ModelNode();
            node.get(ModelDescriptionConstants.OP).set("take-snapshot");
            ModelNode result = client.getControllerClient().execute(node);
            if (!"success".equals(result.get(ClientConstants.OUTCOME).asString())) {
                fail("Reload operation didn't finish successfully: " + result.asString());
            }
            String snapshot = result.get(ModelDescriptionConstants.RESULT).asString();
            final String fileName = snapshot.contains(File.separator) ? snapshot.substring(snapshot.lastIndexOf(File.separator) + 1) : snapshot;
            return new AutoCloseable() {
                @Override
                public void close() throws Exception {
                    ServerReload.executeReloadAndWaitForCompletion(client, fileName);

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
