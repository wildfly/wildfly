/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.subsystem.test.SubsystemOperations.OperationBuilder;
import static org.junit.Assert.*;

import java.io.IOException;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggerOperationsTestCase extends AbstractOperationsTestCase {

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/empty-subsystem.xml");
    }

    @Test
    public void testOperations() throws Exception {
        final KernelServices kernelServices = boot();

        testRootLogger(kernelServices, null);
        testRootLogger(kernelServices, PROFILE);

        testLogger(kernelServices, null);
        testLogger(kernelServices, PROFILE);
    }

    private void testRootLogger(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createRootLoggerAddress(profileName).toModelNode();
        final ModelNode handlers = new ModelNode().setEmptyList().add("CONSOLE");

        // Add the logger
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        executeOperation(kernelServices, addOp);

        // Add a console handler for handlers tests
        final ModelNode consoleAddress = createConsoleHandlerAddress(profileName, "CONSOLE").toModelNode();
        executeOperation(kernelServices, SubsystemOperations.createAddOperation(consoleAddress));

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address, handlers);

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);

        // Test the add-handler operation
        ModelNode op = OperationBuilder.create(RootLoggerResourceDefinition.ADD_HANDLER_OPERATION, address)
                .addAttribute(CommonAttributes.HANDLER_NAME, "CONSOLE")
                .build();
        executeOperation(kernelServices, op);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, CommonAttributes.HANDLERS.getName());
        ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(handlers, SubsystemOperations.readResult(result));

        // Test remove-handler operation
        op = SubsystemOperations.createOperation(RootLoggerResourceDefinition.REMOVE_HANDLER_OPERATION.getName(), address);
        op.get(CommonAttributes.HANDLER_NAME.getName()).set("CONSOLE");
        executeOperation(kernelServices, op);
        result = executeOperation(kernelServices, readOp);
        assertTrue("Handler CONSOLE should have been removed: " + result, SubsystemOperations.readResult(result)
                .asList()
                .isEmpty());

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(consoleAddress));
        verifyRemoved(kernelServices, consoleAddress);
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
    }

    private void testLogger(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createLoggerAddress(profileName, "org.jboss.as.logging").toModelNode();
        final ModelNode handlers = new ModelNode().setEmptyList().add("CONSOLE");

        // Add the logger
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        executeOperation(kernelServices, addOp);

        // Add a console handler for handlers tests
        final ModelNode consoleAddress = createConsoleHandlerAddress(profileName, "CONSOLE").toModelNode();
        executeOperation(kernelServices, SubsystemOperations.createAddOperation(consoleAddress));

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address, handlers);
        testWrite(kernelServices, address, LoggerResourceDefinition.USE_PARENT_HANDLERS, false);

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, LoggerResourceDefinition.USE_PARENT_HANDLERS);

        // Test the add-handler operation
        ModelNode op = OperationBuilder.create(LoggerResourceDefinition.ADD_HANDLER_OPERATION, address)
                .addAttribute(CommonAttributes.HANDLER_NAME, "CONSOLE")
                .build();
        executeOperation(kernelServices, op);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, CommonAttributes.HANDLERS.getName());
        ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(handlers, SubsystemOperations.readResult(result));

        // Test remove-handler operation
        op = SubsystemOperations.createOperation(LoggerResourceDefinition.REMOVE_HANDLER_OPERATION.getName(), address);
        op.get(CommonAttributes.HANDLER_NAME.getName()).set("CONSOLE");
        executeOperation(kernelServices, op);
        result = executeOperation(kernelServices, readOp);
        assertTrue("Handler CONSOLE should have been removed: " + result, SubsystemOperations.readResult(result)
                .asList()
                .isEmpty());

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(consoleAddress));
        verifyRemoved(kernelServices, consoleAddress);
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
    }

    protected void testWriteCommonAttributes(final KernelServices kernelServices, final ModelNode address, final ModelNode handlers) throws Exception {
        testWrite(kernelServices, address, CommonAttributes.FILTER_SPEC, "deny");
        testWrite(kernelServices, address, CommonAttributes.LEVEL, "INFO");
        testWrite(kernelServices, address, CommonAttributes.HANDLERS, handlers);
    }

    protected void testUndefineCommonAttributes(final KernelServices kernelServices, final ModelNode address) throws Exception {
        testUndefine(kernelServices, address, CommonAttributes.FILTER_SPEC);
        testUndefine(kernelServices, address, CommonAttributes.LEVEL);
        testUndefine(kernelServices, address, CommonAttributes.HANDLERS);
    }
}
