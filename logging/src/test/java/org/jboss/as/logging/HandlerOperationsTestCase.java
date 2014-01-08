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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class HandlerOperationsTestCase extends AbstractOperationsTestCase {

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

        testAsyncHandler(kernelServices, null);
        testAsyncHandler(kernelServices, PROFILE);

        testConsoleHandler(kernelServices, null);
        testConsoleHandler(kernelServices, PROFILE);

        testFileHandler(kernelServices, null);
        testFileHandler(kernelServices, PROFILE);

        testPeriodicRotatingFileHandler(kernelServices, null);
        testPeriodicRotatingFileHandler(kernelServices, PROFILE);

        testSizeRotatingFileHandler(kernelServices, null);
        testSizeRotatingFileHandler(kernelServices, PROFILE);
    }

    private void testAsyncHandler(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createAsyncHandlerAddress(profileName, "async").toModelNode();
        final ModelNode subhandlers = new ModelNode().setEmptyList().add("CONSOLE");

        // Add the handler
        final ModelNode addOp = OperationBuilder.createAddOperation(address)
                .addAttribute(AsyncHandlerResourceDefinition.QUEUE_LENGTH, 10)
                .build();
        executeOperation(kernelServices, addOp);

        // Add a console handler for subhandler tests
        final ModelNode consoleAddress = createConsoleHandlerAddress(profileName, "CONSOLE").toModelNode();
        executeOperation(kernelServices, SubsystemOperations.createAddOperation(consoleAddress));

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address);
        testWrite(kernelServices, address, AsyncHandlerResourceDefinition.OVERFLOW_ACTION, "BLOCK");
        testWrite(kernelServices, address, AsyncHandlerResourceDefinition.SUBHANDLERS, subhandlers);
        testWrite(kernelServices, address, AsyncHandlerResourceDefinition.QUEUE_LENGTH, 20);

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, AsyncHandlerResourceDefinition.OVERFLOW_ACTION);
        testUndefine(kernelServices, address, AsyncHandlerResourceDefinition.SUBHANDLERS);

        // Test the add-handler operation
        ModelNode op = OperationBuilder.create(AsyncHandlerResourceDefinition.ADD_HANDLER, address)
                .addAttribute(CommonAttributes.HANDLER_NAME, "CONSOLE")
                .build();
        executeOperation(kernelServices, op);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, AsyncHandlerResourceDefinition.SUBHANDLERS);
        ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(subhandlers, SubsystemOperations.readResult(result));

        // Test remove-handler operation
        op = SubsystemOperations.createOperation(AsyncHandlerResourceDefinition.REMOVE_HANDLER.getName(), address);
        op.get(CommonAttributes.HANDLER_NAME.getName()).set("CONSOLE");
        executeOperation(kernelServices, op);
        result = executeOperation(kernelServices, readOp);
        assertTrue("Subhandler CONSOLE should have been removed: " + result, SubsystemOperations.readResult(result)
                .asList()
                .isEmpty());

        // Ensure the model doesn't contain any erroneous attributes
        op = SubsystemOperations.createReadResourceOperation(address);
        result = executeOperation(kernelServices, op);
        final ModelNode asyncHandlerResource = SubsystemOperations.readResult(result);
        validateResourceAttributes(asyncHandlerResource, Logging.join(AsyncHandlerResourceDefinition.ATTRIBUTES, CommonAttributes.NAME, CommonAttributes.FILTER));
        // The name attribute should be the same as the last path element of the address
        assertEquals(asyncHandlerResource.get(CommonAttributes.NAME.getName()).asString(), PathAddress.pathAddress(address).getLastElement().getValue());

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(consoleAddress));
        verifyRemoved(kernelServices, consoleAddress);
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
    }

    private void testConsoleHandler(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createConsoleHandlerAddress(profileName, "CONSOLE").toModelNode();

        // Add the handler
        final ModelNode addOp = SubsystemOperations.createAddOperation(address);
        executeOperation(kernelServices, addOp);

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address);
        testWrite(kernelServices, address, CommonAttributes.AUTOFLUSH, false);
        testWrite(kernelServices, address, ConsoleHandlerResourceDefinition.TARGET, "System.err");

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, CommonAttributes.AUTOFLUSH);
        testUndefine(kernelServices, address, ConsoleHandlerResourceDefinition.TARGET);

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
    }

    private void testFileHandler(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createFileHandlerAddress(profileName, "FILE").toModelNode();
        final String filename = "test-file.log";
        final String newFilename = "new-test-file.log";

        // Add the handler
        final ModelNode addOp = OperationBuilder.createAddOperation(address)
                .addAttribute(CommonAttributes.FILE, createFileValue("jboss.server.log.dir", filename))
                .build();
        executeOperation(kernelServices, addOp);
        verifyFile(filename);

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address);
        testWrite(kernelServices, address, CommonAttributes.APPEND, false);
        testWrite(kernelServices, address, CommonAttributes.AUTOFLUSH, false);

        final ModelNode newFile = createFileValue("jboss.server.log.dir", newFilename);
        testWrite(kernelServices, address, CommonAttributes.FILE, newFile);
        verifyFile(newFilename);

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, CommonAttributes.APPEND);
        testUndefine(kernelServices, address, CommonAttributes.AUTOFLUSH);

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
        removeFile(filename);
        removeFile(newFilename);
    }

    private void testPeriodicRotatingFileHandler(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createPeriodicRotatingFileHandlerAddress(profileName, "FILE").toModelNode();
        final String filename = "test-file.log";
        final String newFilename = "new-test-file.log";

        // Add the handler
        final ModelNode addOp = OperationBuilder.createAddOperation(address)
                .addAttribute(CommonAttributes.FILE, createFileValue("jboss.server.log.dir", filename))
                .addAttribute(PeriodicHandlerResourceDefinition.SUFFIX, ".yyyy-MM-dd")
                .build();
        executeOperation(kernelServices, addOp);
        verifyFile(filename);

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address);
        testWrite(kernelServices, address, CommonAttributes.APPEND, false);
        testWrite(kernelServices, address, CommonAttributes.AUTOFLUSH, false);

        final ModelNode newFile = createFileValue("jboss.server.log.dir", newFilename);
        testWrite(kernelServices, address, CommonAttributes.FILE, newFile);
        verifyFile(newFilename);

        testWrite(kernelServices, address, PeriodicHandlerResourceDefinition.SUFFIX, ".yyyy-MM-dd-HH");

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, CommonAttributes.APPEND);
        testUndefine(kernelServices, address, CommonAttributes.AUTOFLUSH);

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
        removeFile(filename);
        removeFile(newFilename);
    }

    private void testSizeRotatingFileHandler(final KernelServices kernelServices, final String profileName) throws Exception {
        final ModelNode address = createSizeRotatingFileHandlerAddress(profileName, "FILE").toModelNode();
        final String filename = "test-file.log";
        final String newFilename = "new-test-file.log";

        // Add the handler
        final ModelNode addOp = OperationBuilder.createAddOperation(address)
                .addAttribute(CommonAttributes.FILE, createFileValue("jboss.server.log.dir", filename))
                .build();
        executeOperation(kernelServices, addOp);
        verifyFile(filename);

        // Write each attribute and check the value
        testWriteCommonAttributes(kernelServices, address);
        testWrite(kernelServices, address, CommonAttributes.APPEND, false);
        testWrite(kernelServices, address, CommonAttributes.AUTOFLUSH, false);

        final ModelNode newFile = createFileValue("jboss.server.log.dir", newFilename);
        testWrite(kernelServices, address, CommonAttributes.FILE, newFile);
        verifyFile(newFilename);

        testWrite(kernelServices, address, SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX, 20);
        testWrite(kernelServices, address, SizeRotatingHandlerResourceDefinition.ROTATE_SIZE, "50m");

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, CommonAttributes.APPEND);
        testUndefine(kernelServices, address, CommonAttributes.AUTOFLUSH);
        testUndefine(kernelServices, address, SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX);

        // Clean-up
        executeOperation(kernelServices, SubsystemOperations.createRemoveOperation(address));
        verifyRemoved(kernelServices, address);
        removeFile(filename);
        removeFile(newFilename);
    }

    // TODO (jrp) do syslog? only concern is will it active it

    protected void testWriteCommonAttributes(final KernelServices kernelServices, final ModelNode address) throws Exception {
        testWrite(kernelServices, address, CommonAttributes.LEVEL, "INFO");
        testWrite(kernelServices, address, CommonAttributes.ENABLED, true);
        testWrite(kernelServices, address, CommonAttributes.ENCODING, "utf-8");
        testWrite(kernelServices, address, CommonAttributes.FORMATTER, "[test] %d{HH:mm:ss,SSS} %-5p [%c] %s%E%n");
        testWrite(kernelServices, address, CommonAttributes.FILTER_SPEC, "deny");
    }

    protected void testUndefineCommonAttributes(final KernelServices kernelServices, final ModelNode address) throws Exception {
        testUndefine(kernelServices, address, CommonAttributes.LEVEL);
        testUndefine(kernelServices, address, CommonAttributes.ENABLED);
        testUndefine(kernelServices, address, CommonAttributes.ENCODING);
        testUndefine(kernelServices, address, CommonAttributes.FORMATTER);
        testUndefine(kernelServices, address, CommonAttributes.FILTER_SPEC);
    }
}
