/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import java.util.List;

import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class RootSubsystemOperationsTestCase extends AbstractOperationsTestCase {
    private final String msg = "Test message ";

    @Before
    public void clearLogDir() {
        final File dir = LoggingTestEnvironment.get().getLogDir();
        deleteRecursively(dir);
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return LoggingTestEnvironment.get();
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/simple-subsystem.xml");
    }

    @Test
    public void testAttributes() throws Exception {
        final KernelServices kernelServices = boot();
        final ModelNode address = SUBSYSTEM_ADDRESS.toModelNode();
        testWrite(kernelServices, address, LoggingResourceDefinition.ADD_LOGGING_API_DEPENDENCIES, true);
        testUndefine(kernelServices, address, LoggingResourceDefinition.ADD_LOGGING_API_DEPENDENCIES);
    }

    @Test
    public void testLogFileResource() throws Exception {
        final KernelServices kernelServices = boot();

        // Subsystem address
        final ModelNode address = SUBSYSTEM_ADDRESS.append("log-file").toModelNode();
        ModelNode op = SubsystemOperations.createReadResourceOperation(address);
        //op.get("include-runtime").set(true);
        ModelNode result = executeOperation(kernelServices, op);
        List<ModelNode> resources = SubsystemOperations.readResult(result).asList();
        assertFalse("No Resources were found: " + result, resources.isEmpty());

        final int currentSize = resources.size();

        // Add a new file not in the jboss.server.log.dir directory
        final File logFile = new File(LoggingTestEnvironment.get().getLogDir(), "fh.log");
        final ModelNode fhAddress = createFileHandlerAddress("fh").toModelNode();
        op = SubsystemOperations.createAddOperation(fhAddress);
        op.get("file").set(createFileValue(null, logFile.getAbsolutePath()));
        executeOperation(kernelServices, op);

        // Re-read the log-file resource, the size should be the same
        result = executeOperation(kernelServices, SubsystemOperations.createReadResourceOperation(address));
        resources = SubsystemOperations.readResult(result).asList();
        assertEquals("Log file " + logFile.getAbsolutePath() + " should not be a resource", currentSize, resources.size());

        // Change the file path of the file handler which should make it a log-file resource
        op = SubsystemOperations.createWriteAttributeOperation(fhAddress, "file", createFileValue("jboss.server.log.dir", "fh-2.log"));
        executeOperation(kernelServices, op);
        // Should be an additional resource
        result = executeOperation(kernelServices, SubsystemOperations.createReadResourceOperation(address));
        resources = SubsystemOperations.readResult(result).asList();
        assertEquals("Additional log-file resource failed to dynamically get added", currentSize + 1, resources.size());

        // Test the read-log-file on the
        final ModelNode simpleLogAddress = SUBSYSTEM_ADDRESS.append("log-file", "simple.log").toModelNode();
        op = SubsystemOperations.createOperation("read-log-file", simpleLogAddress);
        testReadLogFile(kernelServices, op, getLogger());

        // Test on the logging-profile
        final ModelNode profileAddress = SUBSYSTEM_ADDRESS.append("logging-profile", "testProfile").append("log-file", "profile-simple.log").toModelNode();
        op = SubsystemOperations.createOperation("read-log-file", profileAddress);
        testReadLogFile(kernelServices, op, getLogger("testProfile"));

    }

    private void testReadLogFile(final KernelServices kernelServices, final ModelNode op, final Logger logger) {
        // Log some messages
        for (int i = 0; i < 50; i++) {
            logger.info(msg + i);
        }

        ModelNode result = executeOperation(kernelServices, op);
        List<String> logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(10, logLines.size());
        checkLogLines(logLines, 40);

        // Read from top
        op.get("tail").set(false);
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(10, logLines.size());
        checkLogLines(logLines, 0);

        // Read more lines from top
        op.get("lines").set(20);
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(20, logLines.size());
        checkLogLines(logLines, 0);

        // Read from bottom
        op.get("tail").set(true);
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(20, logLines.size());
        checkLogLines(logLines, 30);

        // Skip lines from bottom
        op.get("tail").set(true);
        op.get("skip").set(5);
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(20, logLines.size());
        checkLogLines(logLines, 25);

        // Skip lines from top
        op.get("tail").set(false);
        op.get("skip").set(5);
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(20, logLines.size());
        checkLogLines(logLines, 5);

        // Read all lines
        op.get("tail").set(false);
        op.get("lines").set(-1);
        op.remove("skip");
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(50, logLines.size());
        checkLogLines(logLines, 0);

        // Read all lines, but 5 lines
        op.get("tail").set(false);
        op.get("lines").set(-1);
        op.get("skip").set(5);
        result = executeOperation(kernelServices, op);
        logLines = SubsystemOperations.readResultAsList(result);
        assertEquals(45, logLines.size());
        checkLogLines(logLines, 5);
    }

    private void checkLogLines(final List<String> logLines, final int start) {
        int index = start;
        for (String line : logLines) {
            final String lineMsg = msg + index;
            assertTrue(String.format("Expected line containing '%s', found '%s", lineMsg, line), line.contains(msg + index));
            index++;
        }
    }

    private Logger getLogger() {
        return LogContext.getSystemLogContext().getLogger("org.jboss.as.logging.test");
    }

    private Logger getLogger(final String loggingProfile) {
        return LoggingProfileContextSelector.getInstance().get(loggingProfile).getLogger("org.jboss.as.logging.test");
    }

    static void deleteRecursively(final File dir) {
        if (dir.isDirectory()) {
            final File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteRecursively(file);
                }
                file.delete();
            }
        }
    }
}
