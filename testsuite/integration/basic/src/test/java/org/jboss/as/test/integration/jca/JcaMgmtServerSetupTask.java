package org.jboss.as.test.integration.jca;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

public abstract class JcaMgmtServerSetupTask extends  JcaMgmtBase implements ServerSetupTask {


    @Override
    public final void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        setManagementClient(managementClient);
        doSetup(managementClient);
    }

    protected abstract void doSetup(final ManagementClient managementClient) throws Exception;
    
}
