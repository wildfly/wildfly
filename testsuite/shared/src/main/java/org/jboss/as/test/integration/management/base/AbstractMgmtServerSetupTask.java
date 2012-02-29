package org.jboss.as.test.integration.management.base;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 * @author Stuart Douglas
 */
public abstract class AbstractMgmtServerSetupTask extends AbstractMgmtTestBase implements ServerSetupTask {

    private ManagementClient managementClient;

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }

    @Override
    public final void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        this.managementClient = managementClient;
        doSetup(managementClient);
    }

    protected abstract void doSetup(final ManagementClient managementClient) throws Exception;
}
