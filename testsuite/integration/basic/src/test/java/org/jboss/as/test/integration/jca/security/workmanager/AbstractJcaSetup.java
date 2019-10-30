/*
 *
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.jboss.as.test.integration.jca.security.workmanager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

public abstract class AbstractJcaSetup extends SnapshotRestoreSetupTask {
    private static final PathAddress JCA_SUBSYSTEM_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "jca");

    @Override
    public void doSetup(ManagementClient managementClient, String containerId) throws Exception {
        ModelControllerClient mcc = managementClient.getControllerClient();
        addWM(mcc);
        addThreadPool(mcc);
        addBootstrapContext(mcc);
    }

    private void addWM(ModelControllerClient client) throws IOException {
        ModelNode addWMOperation = Operations.createAddOperation(getWorkManagerAddress().toModelNode());
        addWMOperation.get("name").set(getWorkManagerName());
        if(getElytronEnabled() != null)
            addWMOperation.get("elytron-enabled").set(getElytronEnabled());
        ModelNode response = execute(addWMOperation, client);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    private void addThreadPool(ModelControllerClient client) throws IOException {
        PathAddress shortRunningThreads = getWorkManagerAddress().append("short-running-threads", getWorkManagerName());
        ModelNode addShortRunningThreadsOperation = Operations.createAddOperation(shortRunningThreads.toModelNode());
        addShortRunningThreadsOperation.get("core-threads").set("20");
        addShortRunningThreadsOperation.get("queue-length").set("20");
        addShortRunningThreadsOperation.get("max-threads").set("20");

        ModelNode response = execute(addShortRunningThreadsOperation, client);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    private void addBootstrapContext(ModelControllerClient client) throws IOException {
        ModelNode addBootstrapCtxOperation = Operations.createAddOperation(getBootstrapContextAddress().toModelNode());
        addBootstrapCtxOperation.get("name").set(getBootstrapContextName());
        addBootstrapCtxOperation.get("workmanager").set(getWorkManagerName());

        ModelNode response = execute(addBootstrapCtxOperation, client);
        Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
    }

    private ModelNode execute(ModelNode operation, ModelControllerClient client) throws IOException {
        return client.execute(operation);
    }

    private PathAddress getWorkManagerAddress() {
        return JCA_SUBSYSTEM_ADDRESS.append("workmanager", getWorkManagerName());
    }

    private PathAddress getBootstrapContextAddress() {
        return JCA_SUBSYSTEM_ADDRESS.append("bootstrap-context", getBootstrapContextName());
    }

    protected abstract String getWorkManagerName();

    protected abstract String getBootstrapContextName();

    protected abstract Boolean getElytronEnabled();
}
