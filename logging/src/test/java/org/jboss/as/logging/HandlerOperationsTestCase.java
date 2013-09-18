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

import static org.jboss.as.controller.client.helpers.Operations.createAddOperation;
import static org.jboss.as.controller.client.helpers.Operations.createRemoveOperation;
import static org.jboss.as.subsystem.test.SubsystemOperations.OperationBuilder;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class HandlerOperationsTestCase extends AbstractOperationsTestCase {

    static final String ENCODING = "UTF-8";

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

    @Test
    @Ignore("WFLY-1860 - reenable after JIRA is resolved")
    public void testFormats() throws Exception {
        final KernelServices kernelServices = boot();

        final File logFile = new File(LoggingTestEnvironment.get().getLogDir(), "formatter.log");
        // Delete the file if it exists
        if (logFile.exists()) logFile.delete();

        // Create a file handler
        final String fileHandlerName = "formatter-handler";
        final ModelNode handlerAddress = createFileHandlerAddress(fileHandlerName).toModelNode();
        ModelNode op = SubsystemOperations.createAddOperation(handlerAddress);
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.ENCODING.getName()).set(ENCODING);
        op.get(CommonAttributes.FILE.getName()).get(PathResourceDefinition.PATH.getName()).set(logFile.getAbsolutePath());
        op.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        op.get(FileHandlerResourceDefinition.FORMATTER.getName()).set("%s%n");
        executeOperation(kernelServices, op);

        // Create a logger
        final Logger logger = LogContext.getSystemLogContext().getLogger(HandlerOperationsTestCase.class.getName());
        final ModelNode loggerAddress = createLoggerAddress(logger.getName()).toModelNode();
        op = SubsystemOperations.createAddOperation(loggerAddress);
        op.get(LoggerResourceDefinition.USE_PARENT_HANDLERS.getName()).set(false);
        op.get(CommonAttributes.HANDLERS.getName()).setEmptyList().add(fileHandlerName);
        executeOperation(kernelServices, op);

        // Log a few records
        logger.log(Level.INFO, "Test message 1");
        logger.log(Level.INFO, "Test message 2");

        // Read the file
        List<String> lines = FileUtils.readLines(logFile, ENCODING);
        assertEquals("Number of lines logged and found in the file do not match", 2, lines.size());

        // Check the lines
        assertEquals("Test message 1", lines.get(0));
        assertEquals("Test message 2", lines.get(1));

        // Create a pattern formatter
        final ModelNode patternFormatterAddress = createPatternFormatterAddress("PATTERN").toModelNode();
        op = SubsystemOperations.createAddOperation(patternFormatterAddress);
        op.get(PatternFormatterResourceDefinition.PATTERN.getName()).set("%K{level}[changed-pattern] %s%n");
        op.get(PatternFormatterResourceDefinition.COLOR_MAP.getName()).set("info:cyan");
        executeOperation(kernelServices, op);

        // Assign the pattern to the handler
        executeOperation(kernelServices, SubsystemOperations.createWriteAttributeOperation(handlerAddress, FileHandlerResourceDefinition.NAMED_FORMATTER, "PATTERN"));

        // Check that the formatter attribute was undefined
        op = SubsystemOperations.createReadAttributeOperation(handlerAddress, FileHandlerResourceDefinition.FORMATTER);
        op.get("include-defaults").set(false);
        ModelNode result = executeOperation(kernelServices, op);
        assertFalse("formatter attribute was not undefined after the change to a named-formatter", SubsystemOperations.readResult(result).isDefined());

        // Log some more records
        logger.log(Level.INFO, "Test message 3");
        logger.log(Level.INFO, "Test message 4");

        // Read the file
        lines = FileUtils.readLines(logFile, ENCODING);
        // 5th line contains nothing but the clear color code
        assertEquals("Number of lines logged and found in the file do not match", 5, lines.size());

        // Check the lines
        assertTrue("Line logged does not match expected: 3", Arrays.equals("\033[36m[changed-pattern] Test message 3".getBytes(ENCODING), lines.get(2).getBytes(ENCODING)));
        // Second line will start with the clear string, followed by the color string
        assertTrue("Line logged does not match expected: 4", Arrays.equals("\033[0m\033[36m[changed-pattern] Test message 4".getBytes(ENCODING), lines.get(3).getBytes(ENCODING)));

        // Change to use a formatter with a color-map
        executeOperation(kernelServices, SubsystemOperations.createWriteAttributeOperation(handlerAddress, FileHandlerResourceDefinition.FORMATTER, "%K{level}[changed-formatter] %s%n"));

        // Check that the named-formatter attribute was undefined
        op = SubsystemOperations.createReadAttributeOperation(handlerAddress, FileHandlerResourceDefinition.NAMED_FORMATTER);
        op.get("include-defaults").set(false);
        result = executeOperation(kernelServices, op);
        assertFalse("named-formatter attribute was not undefined after the change to a formatter", SubsystemOperations.readResult(result).isDefined());

        // Log some more records
        logger.log(Level.INFO, "Test message 5");
        logger.log(Level.INFO, "Test message 6");

        // Read the file
        lines = FileUtils.readLines(logFile, ENCODING);
        // 5th line contains nothing but the clear color code
        assertEquals("Number of lines logged and found in the file do not match", 7, lines.size());

        // Check the lines
        // Lines will start with the clear string due to previous color-map in the pattern-formatter used above,
        // following  clear code prepended to each line after.
        assertTrue("Line logged does not match expected: 5", Arrays.equals("\033[0m\033[0m[changed-formatter] Test message 5".getBytes(ENCODING), lines.get(4).getBytes(ENCODING)));
        assertTrue("Line logged does not match expected: 6", Arrays.equals("\033[0m\033[0m[changed-formatter] Test message 6".getBytes(ENCODING), lines.get(5).getBytes(ENCODING)));

        // Finally clean everything up
        op = SubsystemOperations.CompositeOperationBuilder.create()
                .addStep(SubsystemOperations.createRemoveOperation(handlerAddress))
                .addStep(SubsystemOperations.createRemoveOperation(patternFormatterAddress))
                .addStep(SubsystemOperations.createRemoveOperation(loggerAddress))
                .build().getOperation();
        executeOperation(kernelServices, op);

    }

    @Test
    public void testFormatsNoColor() throws Exception {
        final KernelServices kernelServices = boot();

        final File logFile = new File(LoggingTestEnvironment.get().getLogDir(), "formatter.log");
        // Delete the file if it exists
        if (logFile.exists()) logFile.delete();

        // Create a file handler
        final String fileHandlerName = "formatter-handler";
        final ModelNode handlerAddress = createFileHandlerAddress(fileHandlerName).toModelNode();
        ModelNode op = SubsystemOperations.createAddOperation(handlerAddress);
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.ENCODING.getName()).set(ENCODING);
        op.get(CommonAttributes.FILE.getName()).get(PathResourceDefinition.PATH.getName()).set(logFile.getAbsolutePath());
        op.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        op.get(FileHandlerResourceDefinition.FORMATTER.getName()).set("%s%n");
        executeOperation(kernelServices, op);

        // Create a logger
        final Logger logger = LogContext.getSystemLogContext().getLogger(HandlerOperationsTestCase.class.getName());
        final ModelNode loggerAddress = createLoggerAddress(logger.getName()).toModelNode();
        op = SubsystemOperations.createAddOperation(loggerAddress);
        op.get(LoggerResourceDefinition.USE_PARENT_HANDLERS.getName()).set(false);
        op.get(CommonAttributes.HANDLERS.getName()).setEmptyList().add(fileHandlerName);
        executeOperation(kernelServices, op);

        // Log a few records
        logger.log(Level.INFO, "Test message 1");
        logger.log(Level.INFO, "Test message 2");

        // Read the file
        List<String> lines = FileUtils.readLines(logFile, ENCODING);
        assertEquals("Number of lines logged and found in the file do not match", 2, lines.size());

        // Check the lines
        assertEquals("Test message 1", lines.get(0));
        assertEquals("Test message 2", lines.get(1));

        // Create a pattern formatter
        final ModelNode patternFormatterAddress = createPatternFormatterAddress("PATTERN").toModelNode();
        op = SubsystemOperations.createAddOperation(patternFormatterAddress);
        op.get(PatternFormatterResourceDefinition.PATTERN.getName()).set("[changed-pattern] %s%n");
        executeOperation(kernelServices, op);

        // Assign the pattern to the handler
        executeOperation(kernelServices, SubsystemOperations.createWriteAttributeOperation(handlerAddress, FileHandlerResourceDefinition.NAMED_FORMATTER, "PATTERN"));

        // Check that the formatter attribute was undefined
        op = SubsystemOperations.createReadAttributeOperation(handlerAddress, FileHandlerResourceDefinition.FORMATTER);
        op.get("include-defaults").set(false);
        ModelNode result = executeOperation(kernelServices, op);
        assertFalse("formatter attribute was not undefined after the change to a named-formatter", SubsystemOperations.readResult(result).isDefined());

        // Log some more records
        logger.log(Level.INFO, "Test message 3");
        logger.log(Level.INFO, "Test message 4");

        // Read the file
        lines = FileUtils.readLines(logFile, ENCODING);
        assertEquals("Number of lines logged and found in the file do not match", 4, lines.size());

        // Check the lines
        assertTrue("Line logged does not match expected: 3", Arrays.equals("[changed-pattern] Test message 3".getBytes(ENCODING), lines.get(2).getBytes(ENCODING)));
        // Second line will start with the clear string, followed by the color string
        assertTrue("Line logged does not match expected: 4", Arrays.equals("[changed-pattern] Test message 4".getBytes(ENCODING), lines.get(3).getBytes(ENCODING)));

        // Finally clean everything up
        op = SubsystemOperations.CompositeOperationBuilder.create()
                .addStep(SubsystemOperations.createRemoveOperation(handlerAddress))
                .addStep(SubsystemOperations.createRemoveOperation(patternFormatterAddress))
                .addStep(SubsystemOperations.createRemoveOperation(loggerAddress))
                .build().getOperation();
        executeOperation(kernelServices, op);

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
        testWrite(kernelServices, address, CommonAttributes.LEVEL, "INFO");
        testWrite(kernelServices, address, CommonAttributes.ENABLED, true);
        testWrite(kernelServices, address, CommonAttributes.FILTER_SPEC, "deny");
        testWrite(kernelServices, address, AsyncHandlerResourceDefinition.OVERFLOW_ACTION, "BLOCK");
        testWrite(kernelServices, address, AsyncHandlerResourceDefinition.SUBHANDLERS, subhandlers);
        testWrite(kernelServices, address, AsyncHandlerResourceDefinition.QUEUE_LENGTH, 20);

        // Undefine attributes
        testUndefine(kernelServices, address, CommonAttributes.LEVEL);
        testUndefine(kernelServices, address, CommonAttributes.ENABLED);
        testUndefine(kernelServices, address, CommonAttributes.FILTER_SPEC);
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
        testWrite(kernelServices, address, SizeRotatingHandlerResourceDefinition.ROTATE_ON_BOOT, true);

        // Undefine attributes
        testUndefineCommonAttributes(kernelServices, address);
        testUndefine(kernelServices, address, CommonAttributes.APPEND);
        testUndefine(kernelServices, address, CommonAttributes.AUTOFLUSH);
        testUndefine(kernelServices, address, SizeRotatingHandlerResourceDefinition.MAX_BACKUP_INDEX);
        testUndefine(kernelServices, address, SizeRotatingHandlerResourceDefinition.ROTATE_ON_BOOT);

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
        testWrite(kernelServices, address, CommonAttributes.ENCODING, ENCODING);
        testWrite(kernelServices, address, AbstractHandlerDefinition.FORMATTER, "[test] %d{HH:mm:ss,SSS} %-5p [%c] %s%E%n");
        testWrite(kernelServices, address, CommonAttributes.FILTER_SPEC, "deny");

        // Add a pattern-formatter
        addPatternFormatter(kernelServices, LoggingProfileOperations.getLoggingProfileName(PathAddress.pathAddress(address)), "PATTERN");
        testWrite(kernelServices, address, AbstractHandlerDefinition.NAMED_FORMATTER, "PATTERN");
    }

    protected void testUndefineCommonAttributes(final KernelServices kernelServices, final ModelNode address) throws Exception {
        testUndefine(kernelServices, address, CommonAttributes.LEVEL);
        testUndefine(kernelServices, address, CommonAttributes.ENABLED);
        testUndefine(kernelServices, address, CommonAttributes.ENCODING);
        testUndefine(kernelServices, address, AbstractHandlerDefinition.FORMATTER);
        testUndefine(kernelServices, address, CommonAttributes.FILTER_SPEC);

        // Remove a pattern-formatter
        testUndefine(kernelServices, address, AbstractHandlerDefinition.NAMED_FORMATTER);
        removePatternFormatter(kernelServices, LoggingProfileOperations.getLoggingProfileName(PathAddress.pathAddress(address)), "PATTERN");
    }

    private void addPatternFormatter(final KernelServices kernelServices, final String profileName, final String name) throws Exception {
        final ModelNode address = createPatternFormatterAddress(profileName, name).toModelNode();
        final ModelNode op = createAddOperation(address);
        op.get(PatternFormatterResourceDefinition.PATTERN.getName()).set("[test-pattern] %d{HH:mm:ss,SSS} %-5p [%c] %s%E%n");
        executeOperation(kernelServices, op);
    }

    private void removePatternFormatter(final KernelServices kernelServices, final String profileName, final String name) throws Exception {
        final ModelNode address = createPatternFormatterAddress(profileName, name).toModelNode();
        final ModelNode op = createRemoveOperation(address);
        executeOperation(kernelServices, op);
    }
}
