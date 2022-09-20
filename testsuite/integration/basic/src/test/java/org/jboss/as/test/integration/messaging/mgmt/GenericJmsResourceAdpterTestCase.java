/*
 * Copyright 2022 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
