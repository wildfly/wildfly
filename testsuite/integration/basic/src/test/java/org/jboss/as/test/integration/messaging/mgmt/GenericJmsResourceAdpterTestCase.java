/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.mgmt;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test trying to deploy the Generic JMS Resource Adapter
 *
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class GenericJmsResourceAdpterTestCase {
    private static final Logger LOGGER = Logger.getLogger(GenericJmsResourceAdpterTestCase.class);

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testAddResourceAdapter() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ClientConstants.OP_ADDR).add("subsystem", "resource-adapters");
        op.get(ClientConstants.OP_ADDR).add("resource-adapter", "MyResourceAdapter");
        op.get(ClientConstants.OP).set(ClientConstants.ADD);
        op.get("module").set("org.jboss.genericjms");
        execute(op, true);
        op = new ModelNode();
        op.get(ClientConstants.OP_ADDR).add("subsystem", "resource-adapters");
        op.get(ClientConstants.OP_ADDR).add("resource-adapter", "MyResourceAdapter");
        op.get(ClientConstants.OP).set(ClientConstants.REMOVE_OPERATION);
        execute(op, true);
    }

    private ModelNode execute(final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        final String outcome = response.get("outcome").asString();
        if (expectSuccess) {
            if (!"success".equals(outcome)) {
                LOGGER.trace(response);
            }
            Assert.assertEquals("success", outcome);
            return response.get("result");
        } else {
            if ("success".equals(outcome)) {
                LOGGER.trace(response);
            }
            Assert.assertEquals("failed", outcome);
            return response.get("failure-description");
        }
    }
}
