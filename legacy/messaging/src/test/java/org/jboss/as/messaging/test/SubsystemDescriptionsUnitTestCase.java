/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.messaging.test;

import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.MessagingExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
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
        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.ADMIN_ONLY_HC).setBootOperations(empty).build();

        final ModelNode operation = createReadResourceDescriptionOperation();
        final ModelNode result = servicesA.executeOperation(operation);

        Assert.assertEquals(ModelDescriptionConstants.SUCCESS, result.get(ModelDescriptionConstants.OUTCOME).asString());
        servicesA.shutdown();
    }

    static ModelNode createReadResourceDescriptionOperation() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "messaging");

        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);
        operation.get(ModelDescriptionConstants.OPERATIONS).set(true);
        operation.get(ModelDescriptionConstants.INHERITED).set(false);
        return operation;
    }

}
