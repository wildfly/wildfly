/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.async;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.undertow.Constants.SERVLET_CONTAINER;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@ServerSetup(StabilityServerSetupSnapshotRestoreTasks.Preview.class)
public class AsyncServletContainerTimeoutTestCase extends AsyncServletTimeoutTestCaseBase {

    private static final PathAddress SERVLET_CONTAINER_ADDRESS = PathAddress
            .pathAddress(PathElement.pathElement(SUBSYSTEM, "undertow"), PathElement.pathElement(SERVLET_CONTAINER, "default"));

    private void setDefaultTimeout(final long timeout) throws IOException {
        final ModelNode setTimeoutOperation = Operations.createWriteAttributeOperation(SERVLET_CONTAINER_ADDRESS.toModelNode(),
                Constants.DEFAULT_ASYNC_CONTEXT_TIMEOUT, timeout);
        final ModelControllerClient controllerClient = managementClient.getControllerClient();
        final ModelNode response = controllerClient.execute(setTimeoutOperation);
        Assert.assertTrue(Operations.isSuccessfulOutcome(response));
        ServerReload.executeReloadAndWaitForCompletion(controllerClient);
    }

    @Override
    protected String getUrl() {
        return url + "init/execute";
    }

    @Override
    protected long getTimeout() {
        return 3000;
    }

    @Override
    protected void setup() throws IOException {
        setDefaultTimeout(getTimeout());
    }
}
