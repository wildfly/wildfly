/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
                    ServerReload.executeReloadAndWaitForCompletion(client.getControllerClient(), fileName);

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
