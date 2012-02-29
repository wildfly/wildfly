/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.mgmt;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

/**
 * Test validating that subsystems register a "describe" operation in order to be able
 * to run in the domain mode.
 *
 * @author Emanuel Muckenhuber
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DescribeOperationUnitTestCase {

    private static final Set<String> ignored = new HashSet<String>();

    @ContainerResource
    private ManagementClient managementClient;

    static {
        // Only a few subsystems are NOT supposed to work in the domain mode
        ignored.add("deployment-scanner");
    }

    @Test
    public void testOperationNames() throws Exception {
        // Get a list of all registered subsystems
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(CHILD_TYPE).set(SUBSYSTEM);

        final ModelNode subsystemsResult = executeForResult(operation);
        for(final ModelNode subsystem : subsystemsResult.asList()) {
            final String name = subsystem.asString();
            if(ignored.contains(name)) {
                continue; // Only a few subsystems are not supposed to work in the domain mode
            }

            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, name);

            // Check that the actual describe operation completes successfully
            final ModelNode describe = new ModelNode();
            describe.get(OP).set(DESCRIBE);
            describe.get(OP_ADDR).set(address);
            executeForResult(describe);

            // Check that the describe operation is registered a 'private' operation
            final ModelNode operationNames = new ModelNode();
            operationNames.get(OP).set(READ_OPERATION_NAMES_OPERATION);
            operationNames.get(OP_ADDR).set(address);

            final ModelNode operationNamesResult = executeForResult(operationNames);
            boolean found = false;
            for(final ModelNode operationName : operationNamesResult.asList()) {
                if(DESCRIBE.equals(operationName.asString())) {
                    found = true;
                    break;
                }
            }
            Assert.assertFalse(String.format("'describe' operation not registered as private in subsystem '%s'", name), found);
        }
    }

    private ModelNode executeForResult(final ModelNode operation) throws Exception {
        final ModelNode result = managementClient.getControllerClient().execute(operation);
        checkSuccessful(result, operation);
        return result.get(RESULT);
    }

    static void checkSuccessful(final ModelNode result, final ModelNode operation) {
        if(! SUCCESS.equals(result.get(OUTCOME).asString())) {
            System.out.println("Failed result:\n" + result + "\n for operation:\n" + operation);
            Assert.fail("operation failed: " + result.get(FAILURE_DESCRIPTION));
        }
    }

}
