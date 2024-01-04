/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.security.workmanager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.util.function.Consumer;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

public abstract class AbstractRaSetup implements ServerSetupTask {

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient mcc = managementClient.getControllerClient();
        addResourceAdapter(mcc);
        addAdminObject(mcc);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        removeResourceAdapterSilently(managementClient.getControllerClient());
    }

    private void addResourceAdapter(ModelControllerClient client) throws IOException {
        ModelNode addRaOperation = Operations.createAddOperation(getResourceAdapterAddress().toModelNode());
        addRaOperation.get("archive").set("wf-ra-wm-security-domain-rar.rar");
        addRaOperation.get("bootstrap-context").set(getBootstrapContextName());
        addRaOperation.get("transaction-support").set("NoTransaction");
        getAddRAOperationConsumer().accept(addRaOperation);

        ModelNode response = execute(addRaOperation, client);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    private void addAdminObject(ModelControllerClient client) throws IOException {
        PathAddress adminObjectAddress = getResourceAdapterAddress().append("admin-objects", "admObj");
        ModelNode addAdminObjectOperation = Operations.createAddOperation(adminObjectAddress.toModelNode());
        addAdminObjectOperation.get("class-name").set("org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl");
        addAdminObjectOperation.get("jndi-name").set(getAdminObjectJNDIName());

        ModelNode response = execute(addAdminObjectOperation, client);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    private void removeResourceAdapterSilently(ModelControllerClient client) throws IOException {
        ModelNode removeRaOperation = Operations.createRemoveOperation(getResourceAdapterAddress().toModelNode());
        removeRaOperation.get(ClientConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set("true");
        client.execute(removeRaOperation);
    }

    private ModelNode execute(ModelNode operation, ModelControllerClient client) throws IOException {
        return client.execute(operation);
    }

    private PathAddress getResourceAdapterAddress() {
        return PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "resource-adapters")
                .append("resource-adapter", getResourceAdapterName());
    }

    protected abstract String getResourceAdapterName();

    protected abstract String getBootstrapContextName();

    protected abstract String getAdminObjectJNDIName();

    protected abstract Consumer<ModelNode> getAddRAOperationConsumer();
}
