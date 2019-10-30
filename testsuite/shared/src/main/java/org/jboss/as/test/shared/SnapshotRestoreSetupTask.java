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
