/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that the read-resource-description operation works.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SubsystemDescriptionsUnitTestCase extends AbstractSubsystemTest {

    public SubsystemDescriptionsUnitTestCase() {
        super(MessagingExtension.SUBSYSTEM_NAME, new MessagingExtension());
    }


    @Test
    public void testSubsystemDescriptions() throws Exception {

        List<ModelNode> empty = Collections.emptyList();
        KernelServices servicesA = createKernelServicesBuilder(null).setBootOperations(empty).build();

        final ModelNode operation = createReadResourceDescriptionOperation();
        final ModelNode result = servicesA.executeOperation(operation);

        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
        servicesA.shutdown();
    }

    static ModelNode createReadResourceDescriptionOperation() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging-activemq");

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);
        operation.get(ModelDescriptionConstants.OPERATIONS).set(true);
        operation.get(ModelDescriptionConstants.INHERITED).set(false);
        return operation;
    }

}
