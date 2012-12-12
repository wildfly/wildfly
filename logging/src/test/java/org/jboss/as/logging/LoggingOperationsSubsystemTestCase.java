/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LoggingOperationsSubsystemTestCase extends AbstractLoggingSubsystemTest {

    private static final String PROFILE = "testProfile";
    private static final String FQCN = LoggingOperationsSubsystemTestCase.class.getName();
    private static final Level[] LEVELS = {
            org.jboss.logmanager.Level.FATAL,
            org.jboss.logmanager.Level.ERROR,
            org.jboss.logmanager.Level.WARN,
            org.jboss.logmanager.Level.INFO,
            org.jboss.logmanager.Level.DEBUG,
            org.jboss.logmanager.Level.TRACE,

    };

    private static File logDir;

    @BeforeClass
    public static void setupLoggingDir() {
        logDir = LoggingTestEnvironment.get().getLogDir();
        for (File file : logDir.listFiles()) {
            file.delete();
        }
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/operations.xml");
    }

    @Test
    public void testChangeRootLogLevel() throws Exception {
        testChangeRootLogLevel(null);
        testChangeRootLogLevel(PROFILE);
    }

    @Test
    public void testSetRootLogger() throws Exception {
        testSetRootLogger(null);
        testSetRootLogger(PROFILE);
    }

    @Test
    public void testAddRemoveFileHandler() throws Exception {
        testAddRemoveFileHandler(null);
        testAddRemoveFileHandler(PROFILE);
    }

    @Test
    public void testLegacyFilters() throws Exception {
        final KernelServices kernelServices = boot();
        final String fileHandlerName = "test-file-handler";

        // add new file logger so we can track logged messages
        final File logFile = createLogFile();
        final ModelNode handlerAddress = createFileHandlerAddress(fileHandlerName).toModelNode();
        addFileHandler(kernelServices, null, fileHandlerName, org.jboss.logmanager.Level.TRACE, logFile, true);
        // Write legacy filters
        for (Map.Entry<String, ModelNode> entry : FilterConversionTestCase.MAP.entrySet()) {
            // Validate the write-attribute operation
            ModelNode op = Operations.createWriteAttributeOperation(handlerAddress, CommonAttributes.FILTER, entry.getValue());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(handlerAddress, CommonAttributes.FILTER_SPEC);
            String filterSpecResult = Operations.readResultAsString(executeOperation(kernelServices, op));
            assertEquals(entry.getKey(), filterSpecResult);

            // Validate an add operation
            final ModelNode tempHandlerAddress = createConsoleHandlerAddress("temp").toModelNode();
            op = Operations.createAddOperation(tempHandlerAddress);
            op.get(CommonAttributes.FILTER.getName()).set(entry.getValue());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(tempHandlerAddress, CommonAttributes.FILTER_SPEC);
            filterSpecResult = Operations.readResultAsString(executeOperation(kernelServices, op));
            assertEquals(entry.getKey(), filterSpecResult);
            // Remove the temp handler
            op = Operations.createRemoveOperation(tempHandlerAddress, true);
            executeOperation(kernelServices, op);

            // Add to a logger
            final ModelNode loggerAddress = createLoggerAddress("test-logger").toModelNode();
            op = Operations.createAddOperation(loggerAddress);
            op.get(CommonAttributes.FILTER.getName()).set(entry.getValue());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(loggerAddress, CommonAttributes.FILTER_SPEC);
            filterSpecResult = Operations.readResultAsString(executeOperation(kernelServices, op));
            assertEquals(entry.getKey(), filterSpecResult);

            // Remove the attribute
            op = Operations.createUndefineAttributeOperation(loggerAddress, CommonAttributes.FILTER_SPEC);
            executeOperation(kernelServices, op);
            op = Operations.createReadAttributeOperation(loggerAddress, CommonAttributes.FILTER);
            // Filter and filter spec should be undefined
            assertEquals("Filter was not undefined", Operations.UNDEFINED, Operations.readResult(executeOperation(kernelServices, op)));
            op = Operations.createReadAttributeOperation(loggerAddress, CommonAttributes.FILTER_SPEC);
            assertEquals("Filter was not undefined", Operations.UNDEFINED, Operations.readResult(executeOperation(kernelServices, op)));

            // Test writing the attribute to the logger
            op = Operations.createWriteAttributeOperation(loggerAddress, CommonAttributes.FILTER, entry.getValue());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(loggerAddress, CommonAttributes.FILTER_SPEC);
            filterSpecResult = Operations.readResultAsString(executeOperation(kernelServices, op));
            assertEquals(entry.getKey(), filterSpecResult);

            // Remove the logger
            op = Operations.createRemoveOperation(loggerAddress, true);
            executeOperation(kernelServices, op);
        }

        // Write new filters
        for (Map.Entry<String, ModelNode> entry : FilterConversionTestCase.MAP.entrySet()) {
            // Write to a handler
            ModelNode op = Operations.createWriteAttributeOperation(handlerAddress, CommonAttributes.FILTER_SPEC, entry.getKey());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(handlerAddress, CommonAttributes.FILTER);
            ModelNode filterResult = Operations.readResult(executeOperation(kernelServices, op));
            ModelTestUtils.compare(entry.getValue(), filterResult);

            // Validate an add operation
            final ModelNode tempHandlerAddress = createConsoleHandlerAddress("temp").toModelNode();
            op = Operations.createAddOperation(tempHandlerAddress);
            op.get(CommonAttributes.FILTER_SPEC.getName()).set(entry.getKey());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(tempHandlerAddress, CommonAttributes.FILTER);
            filterResult = Operations.readResult(executeOperation(kernelServices, op));
            ModelTestUtils.compare(entry.getValue(), filterResult);
            // Remove the temp handler
            op = Operations.createRemoveOperation(tempHandlerAddress, true);
            executeOperation(kernelServices, op);

            // Add to a logger
            final ModelNode loggerAddress = createLoggerAddress("test-logger").toModelNode();
            op = Operations.createAddOperation(loggerAddress);
            op.get(CommonAttributes.FILTER_SPEC.getName()).set(entry.getKey());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(loggerAddress, CommonAttributes.FILTER);
            filterResult = Operations.readResult(executeOperation(kernelServices, op));
            ModelTestUtils.compare(entry.getValue(), filterResult);

            // Test writing the attribute to the logger
            op = Operations.createWriteAttributeOperation(loggerAddress, CommonAttributes.FILTER_SPEC, entry.getKey());
            executeOperation(kernelServices, op);
            // Read the current value
            op = Operations.createReadAttributeOperation(loggerAddress, CommonAttributes.FILTER);
            filterResult = Operations.readResult(executeOperation(kernelServices, op));
            ModelTestUtils.compare(entry.getValue(), filterResult);

            // Remove the logger
            op = Operations.createRemoveOperation(loggerAddress, true);
            executeOperation(kernelServices, op);
        }
        removeFileHandler(kernelServices, null, fileHandlerName, true);
    }

    @Test
    public void testLoggingProfile() throws Exception {
        final KernelServices kernelServices = boot();
        final String handlerName = "test-file-handler";

        final File logFile = createLogFile();
        final File profileLogFile = createLogFile("profile.log");
        final ModelNode handlerAddress = createFileHandlerAddress(handlerName).toModelNode();
        final ModelNode profileHandlerAddress = createFileHandlerAddress(PROFILE, handlerName).toModelNode();

        // Add handlers
        addFileHandler(kernelServices, null, handlerName, org.jboss.logmanager.Level.INFO, logFile, true);
        addFileHandler(kernelServices, PROFILE, handlerName, org.jboss.logmanager.Level.INFO, profileLogFile, true);

        // Change the format
        ModelNode op = Operations.createReadAttributeOperation(handlerAddress, CommonAttributes.FORMATTER);
        final String defaultHandlerFormat = Operations.readResultAsString(executeOperation(kernelServices, op));
        op = Operations.createReadAttributeOperation(profileHandlerAddress, CommonAttributes.FORMATTER);
        final String defaultProfileHandlerFormat = Operations.readResultAsString(executeOperation(kernelServices, op));
        op = Operations.createWriteAttributeOperation(handlerAddress, CommonAttributes.FORMATTER, "%m%n");
        executeOperation(kernelServices, op);
        op = Operations.createWriteAttributeOperation(profileHandlerAddress, CommonAttributes.FORMATTER, "%m%n");
        executeOperation(kernelServices, op);

        // Log with and without profile
        final String msg = "This is a test message";
        doLog(null, LEVELS, msg);
        doLog(PROFILE, LEVELS, msg);

        // Reset the formatters
        op = Operations.createWriteAttributeOperation(handlerAddress, CommonAttributes.FORMATTER, defaultHandlerFormat);
        executeOperation(kernelServices, op);
        op = Operations.createWriteAttributeOperation(profileHandlerAddress, CommonAttributes.FORMATTER, defaultProfileHandlerFormat);
        executeOperation(kernelServices, op);

        // Remove the handler
        removeFileHandler(kernelServices, null, handlerName, true);
        removeFileHandler(kernelServices, PROFILE, handlerName, true);

        // Read the files to a string
        final String result = FileUtils.readFileToString(logFile);
        final String profileResult = FileUtils.readFileToString(profileLogFile);

        // Check generated log file
        assertTrue(result.contains(msg));
        assertTrue(profileResult.contains(msg));

        // The contents of the files should match
        assertTrue(String.format("Contents don't match: %nResult:%n%s%nProfileResult%n%s", result, profileResult), result.equals(profileResult));
    }

    private void testChangeRootLogLevel(final String loggingProfile) throws Exception {
        final KernelServices kernelServices = boot();
        final String fileHandlerName = "test-file-handler";

        // add new file logger so we can track logged messages
        final File logFile = createLogFile();
        addFileHandler(kernelServices, loggingProfile, fileHandlerName, org.jboss.logmanager.Level.TRACE, logFile, true);

        final Level[] levels = {
                org.jboss.logmanager.Level.FATAL,
                org.jboss.logmanager.Level.ERROR,
                org.jboss.logmanager.Level.WARN,
                org.jboss.logmanager.Level.INFO,
                org.jboss.logmanager.Level.DEBUG,
                org.jboss.logmanager.Level.TRACE
        };
        final Map<Level, Integer> levelOrd = new HashMap<Level, Integer>();
        levelOrd.put(org.jboss.logmanager.Level.FATAL, 0);
        levelOrd.put(org.jboss.logmanager.Level.ERROR, 1);
        levelOrd.put(org.jboss.logmanager.Level.WARN, 2);
        levelOrd.put(org.jboss.logmanager.Level.INFO, 3);
        levelOrd.put(org.jboss.logmanager.Level.DEBUG, 4);
        levelOrd.put(org.jboss.logmanager.Level.TRACE, 5);

        // log messages on all levels with different root logger level settings
        final ModelNode address = createRootLoggerAddress(loggingProfile).toModelNode();
        for (Level level : levels) {
            // change root log level
            final ModelNode op = Operations.createWriteAttributeOperation(address, CommonAttributes.LEVEL, level.getName());
            executeOperation(kernelServices, op);
            doLog(loggingProfile, levels, "RootLoggerTestCaseTST %s", level);
        }

        // Remove the handler
        removeFileHandler(kernelServices, loggingProfile, fileHandlerName, true);

        // go through logged messages - test that with each root logger level settings
        // message with equal priority and also messages with all higher
        // priorities were logged

        final boolean[][] logFound = new boolean[levelOrd.size()][levelOrd.size()];

        final List<String> logLines = FileUtils.readLines(logFile);
        for (String line : logLines) {
            if (!line.contains("RootLoggerTestCaseTST")) continue; // not our log
            final String[] words = line.split("\\s+");
            try {
                final Level lineLogLevel = Level.parse(words[1]);
                final Level rootLogLevel = Level.parse(words[5]);
                final int producedLevel = levelOrd.get(lineLogLevel);
                final int loggedLevel = levelOrd.get(rootLogLevel);
                assertTrue(String.format("Produced level(%s) greater than logged level (%s)", lineLogLevel, rootLogLevel), producedLevel <= loggedLevel);
                logFound[producedLevel][loggedLevel] = true;
            } catch (Exception e) {
                throw new Exception("Unexpected log:" + line);
            }
        }
        for (Level level : levels) {
            final int rl = levelOrd.get(level);
            for (int ll = 0; ll <= rl; ll++) assertTrue(logFound[ll][rl]);
        }

    }

    private void testSetRootLogger(final String loggingProfile) throws Exception {
        final KernelServices kernelServices = boot();
        final String fileHandlerName = "test-file-handler";

        // Add new file logger so we can test root logger change
        final File logFile = createLogFile();
        addFileHandler(kernelServices, loggingProfile, fileHandlerName, org.jboss.logmanager.Level.INFO, logFile, false);

        // Read root logger
        final ModelNode rootLoggerAddress = createRootLoggerAddress(loggingProfile).toModelNode();
        ModelNode op = Operations.createOperation(Operations.READ_RESOURCE, rootLoggerAddress);
        final ModelNode rootLoggerResult = executeOperation(kernelServices, op);
        final List<String> handlers = modelNodeAsStringList(rootLoggerResult.get(CommonAttributes.HANDLERS.getName()));

        // Remove the root logger
        op = Operations.createRemoveOperation(rootLoggerAddress, false);
        executeOperation(kernelServices, op);

        // Set a new root logger
        op = Operations.createOperation(ModelDescriptionConstants.ADD, rootLoggerAddress);
        op.get(CommonAttributes.LEVEL.getName()).set(rootLoggerResult.get(CommonAttributes.LEVEL.getName()));
        for (String handler : handlers) op.get(CommonAttributes.HANDLERS.getName()).add(handler);
        op.get(CommonAttributes.HANDLERS.getName()).add(fileHandlerName);
        executeOperation(kernelServices, op);
        doLog(loggingProfile, LEVELS, "Test123");

        // Remove the root logger
        op = Operations.createRemoveOperation(rootLoggerAddress, false);
        executeOperation(kernelServices, op);


        // Revert root logger
        op = Operations.createOperation(ModelDescriptionConstants.ADD, rootLoggerAddress);
        op.get(CommonAttributes.LEVEL.getName()).set(rootLoggerResult.get(CommonAttributes.LEVEL.getName()));
        op.get(CommonAttributes.HANDLERS.getName()).set(rootLoggerResult.get(CommonAttributes.HANDLERS.getName()));
        executeOperation(kernelServices, op);

        // remove file handler
        removeFileHandler(kernelServices, loggingProfile, fileHandlerName, false);

        // check that root logger were changed - file logger was registered
        String log = FileUtils.readFileToString(logFile);
        assertTrue(log.contains("Test123"));
    }

    private void testAddRemoveFileHandler(final String loggingProfile) throws Exception {
        final KernelServices kernelServices = boot();
        final String fileHandlerName = "test-file-handler";

        File logFile = createLogFile();

        // Add file handler
        addFileHandler(kernelServices, loggingProfile, fileHandlerName, org.jboss.logmanager.Level.INFO, logFile, true);

        // Ensure the handler is listed
        final ModelNode rootLoggerAddress = createRootLoggerAddress(loggingProfile).toModelNode();
        ModelNode op = Operations.createReadAttributeOperation(rootLoggerAddress, CommonAttributes.HANDLERS);
        ModelNode handlerResult = executeOperation(kernelServices, op);
        List<String> handlerList = Operations.readResultAsList(handlerResult);
        assertTrue(String.format("Handler '%s' was not found. Result: %s", fileHandlerName, handlerResult), handlerList.contains(fileHandlerName));
        doLog(loggingProfile, LEVELS, "Test123");

        // Remove handler from logger
        op = Operations.createOperation(RootLoggerResourceDefinition.ROOT_LOGGER_REMOVE_HANDLER_OPERATION_NAME, rootLoggerAddress);
        op.get(CommonAttributes.NAME.getName()).set(fileHandlerName);
        executeOperation(kernelServices, op);

        // Ensure the handler is not listed
        op = Operations.createReadAttributeOperation(rootLoggerAddress, CommonAttributes.HANDLERS);
        handlerResult = executeOperation(kernelServices, op);
        handlerList = Operations.readResultAsList(handlerResult);
        assertFalse(String.format("Handler '%s' was not removed. Result: %s", fileHandlerName, handlerResult), handlerList.contains(fileHandlerName));

        // Remove the handler
        removeFileHandler(kernelServices, loggingProfile, fileHandlerName, false);

        // check generated log file
        assertTrue(FileUtils.readFileToString(logFile).contains("Test123"));

        // verify that the logger is stopped, no more logs are comming to the file
        long checksum = FileUtils.checksumCRC32(logFile);
        doLog(loggingProfile, LEVELS, "Test123");
        assertEquals(checksum, FileUtils.checksumCRC32(logFile));
    }


    private void addFileHandler(final KernelServices kernelServices, final String loggingProfile, final String name,
                                final Level level, final File file, final boolean assign) throws Exception {

        // add file handler
        ModelNode op = Operations.createAddOperation(createFileHandlerAddress(loggingProfile, name).toModelNode());
        op.get(CommonAttributes.NAME.getName()).set(name);
        op.get(CommonAttributes.LEVEL.getName()).set(level.getName());
        op.get(CommonAttributes.FILE.getName()).get(PathResourceDefinition.PATH.getName()).set(file.getAbsolutePath());
        executeOperation(kernelServices, op);

        if (!assign) return;

        // register it with root logger
        op = Operations.createOperation(RootLoggerResourceDefinition.ROOT_LOGGER_ADD_HANDLER_OPERATION_NAME, createRootLoggerAddress(loggingProfile).toModelNode());
        op.get(CommonAttributes.NAME.getName()).set(name);
        executeOperation(kernelServices, op);
    }

    private void removeFileHandler(final KernelServices kernelServices, final String loggingProfile, final String name,
                                   final boolean unassign) throws Exception {

        if (unassign) {
            // Remove the handler from the logger
            final ModelNode op = Operations.createOperation(RootLoggerResourceDefinition.ROOT_LOGGER_REMOVE_HANDLER_OPERATION_NAME, createRootLoggerAddress(loggingProfile).toModelNode());
            op.get(CommonAttributes.NAME.getName()).set(name);
            executeOperation(kernelServices, op);
        }

        // Remove the handler
        final ModelNode op = Operations.createRemoveOperation(createFileHandlerAddress(loggingProfile, name).toModelNode(), false);
        executeOperation(kernelServices, op);
    }

    private ModelNode executeOperation(final KernelServices kernelServices, final ModelNode op) {
        return executeOperation(kernelServices, op, true);
    }

    private ModelNode executeOperation(final KernelServices kernelServices, final ModelNode op, final boolean validateResult) {
        final ModelNode result = kernelServices.executeOperation(op);
        if (validateResult) assertTrue(Operations.getFailureDescription(result), Operations.successful(result));
        return result;
    }

    private void doLog(final String loggingProfile, final Level[] levels, final String format, final Object... params) {
        final LogContext logContext;
        if (loggingProfile != null) {
            logContext = LoggingProfileContextSelector.getInstance().get(loggingProfile);
        } else {
            logContext = LogContext.getSystemLogContext();
        }
        final Logger log = logContext.getLogger(FQCN);
        // log a message
        for (Level lvl : levels) {
            log.log(lvl, String.format(format, params));
        }
    }

    private static File createLogFile() {
        return createLogFile("test-fh.log");
    }

    private static File createLogFile(final String filename) {
        final File logFile = new File(logDir, filename);
        if (logFile.exists()) assertTrue("Log file was not deleted", logFile.delete());
        return logFile;
    }
}
