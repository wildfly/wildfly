/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ServerSnapshot;

/**
 * Implementation of ServerSetupTask for JCA related tests
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public abstract class JcaMgmtServerSetupTask extends JcaMgmtBase implements ServerSetupTask {

    private AutoCloseable snapshot;

    @Override
    public final void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        snapshot = ServerSnapshot.takeSnapshot(managementClient);
        setManagementClient(managementClient);
        doSetup(managementClient);
    }

    protected abstract void doSetup(final ManagementClient managementClient) throws Exception;

    @Override
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        snapshot.close();
    }
}
