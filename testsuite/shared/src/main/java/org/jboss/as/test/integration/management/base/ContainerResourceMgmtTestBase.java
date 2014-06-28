package org.jboss.as.test.integration.management.base;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 * Class that is extended by management tests that can use resource injection to get the management client
 *
 * @author Stuart Douglas
 */
public abstract class ContainerResourceMgmtTestBase extends AbstractMgmtTestBase {

    @ContainerResource
    private ManagementClient managementClient;


    public ManagementClient getManagementClient() {
        return managementClient;
    }



    public void setManagementClient(ManagementClient managementClient) {
        this.managementClient = managementClient;
    }



    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }
}
