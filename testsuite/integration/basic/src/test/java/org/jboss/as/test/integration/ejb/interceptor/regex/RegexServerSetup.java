/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * Server setup task which set up ejb3 subsystem to allow regex
 * for ejb names in interceptor binding.
 *
 * @author Stuart Douglas
 */
public class RegexServerSetup implements ServerSetupTask {
    private static final String ALLOW_EJB_NAME_REGEX = "allow-ejb-name-regex";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        ModelNode node = new ModelNode();
        node.get(ADDRESS).set(PathAddress.parseCLIStyleAddress("/subsystem=ejb3").toModelNode());
        node.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        node.get(NAME).set(ALLOW_EJB_NAME_REGEX);
        node.get(VALUE).set(true);

        final ModelNode result = managementClient.getControllerClient().execute(node);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail(Operations.getFailureDescription(result).asString());
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelNode node = new ModelNode();
        node.get(ADDRESS).set(PathAddress.parseCLIStyleAddress("/subsystem=ejb3").toModelNode());
        node.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
        node.get(NAME).set(ALLOW_EJB_NAME_REGEX);
        final ModelNode result = managementClient.getControllerClient().execute(node);
        if (!Operations.isSuccessfulOutcome(result)) {
            Assert.fail(Operations.getFailureDescription(result).asString());
        }
    }
}