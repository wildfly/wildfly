/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.web.httpinvoker;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class HTTPInvokerSecuredTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class, "httpinvoker.war");
    }

    @Test
    public void testHttpInvokerConfiguration(@ContainerResource ManagementClient managementClient) throws Exception {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, "undertow");
        address.add(ModelDescriptionConstants.SERVER, "default-server");
        address.add(ModelDescriptionConstants.HOST, "default-host");
        address.add("setting", "http-invoker");

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);

        final ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertTrue("Failure to read http-invoker resource: " + result.toString(),
            Operations.isSuccessfulOutcome(result));

        final ModelNode operationResult = result.get(ModelDescriptionConstants.RESULT);
        validateOperation(operationResult);
    }

    protected abstract void validateOperation(ModelNode operationResult);
}
