/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

/**
 * ServerSetupTask that takes a snapshot when the test starts, and then restores it at the end
 * <p>
 * If this setup task is in use it should be the first setup task in the task list, as otherwise the snapshot
 * will have changes from the previous setup task.
 * <p>
 * It can either be used standalone or via inheritance.
 */
public class SnapshotRestoreSetupTask implements ServerSetupTask {

    /**
     * This is a map rather than just a field to allow this to be used in multi server test suites
     */
    private final Map<String, AutoCloseable> snapshots = new HashMap<>();

    @Override
    public final void setup(ManagementClient managementClient, String containerId) throws Exception {
        snapshots.put(containerId, ServerSnapshot.takeSnapshot(managementClient));
        doSetup(managementClient, containerId);
    }

    protected void doSetup(ManagementClient client, String containerId) throws Exception {

    }

    @Override
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        AutoCloseable snapshot = snapshots.remove(containerId);
        if (snapshot != null) {
            snapshot.close();
        }
        nonManagementCleanUp();
    }

    protected void nonManagementCleanUp() throws Exception {

    }
}
