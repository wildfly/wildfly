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

import static org.junit.Assert.*;

import java.io.File;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.BeforeClass;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractOperationsTestCase extends AbstractLoggingSubsystemTest {

    static final String PROFILE = "testProfile";

    private static File logDir;

    @BeforeClass
    public static void setupLoggingDir() {
        logDir = LoggingTestEnvironment.get().getLogDir();
        for (File file : logDir.listFiles()) {
            file.delete();
        }
    }

    @After
    @Override
    public void clearLogContext() {
        super.clearLogContext();
        final LoggingProfileContextSelector contextSelector = LoggingProfileContextSelector.getInstance();
        if (contextSelector.exists(PROFILE)) {
            clearLogContext(contextSelector.get(PROFILE));
            contextSelector.remove(PROFILE);
        }
    }

    protected void testWrite(final KernelServices kernelServices, final ModelNode address, final AttributeDefinition attribute, final String value) {
        final ModelNode writeOp = SubsystemOperations.createWriteAttributeOperation(address, attribute, value);
        executeOperation(kernelServices, writeOp);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, attribute);
        final ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(value, SubsystemOperations.readResultAsString(result));
    }

    protected void testWrite(final KernelServices kernelServices, final ModelNode address, final AttributeDefinition attribute, final boolean value) {
        final ModelNode writeOp = SubsystemOperations.createWriteAttributeOperation(address, attribute, value);
        executeOperation(kernelServices, writeOp);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, attribute);
        final ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(value, SubsystemOperations.readResult(result).asBoolean());
    }

    protected void testWrite(final KernelServices kernelServices, final ModelNode address, final AttributeDefinition attribute, final int value) {
        final ModelNode writeOp = SubsystemOperations.createWriteAttributeOperation(address, attribute, value);
        executeOperation(kernelServices, writeOp);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, attribute);
        final ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(value, SubsystemOperations.readResult(result).asInt());
    }

    protected void testWrite(final KernelServices kernelServices, final ModelNode address, final AttributeDefinition attribute, final ModelNode value) {
        final ModelNode writeOp = SubsystemOperations.createWriteAttributeOperation(address, attribute, value);
        executeOperation(kernelServices, writeOp);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, attribute);
        final ModelNode result = executeOperation(kernelServices, readOp);
        assertEquals(value, SubsystemOperations.readResult(result));
    }

    protected void testUndefine(final KernelServices kernelServices, final ModelNode address, final AttributeDefinition attribute) {
        final ModelNode undefineOp = SubsystemOperations.createUndefineAttributeOperation(address, attribute);
        executeOperation(kernelServices, undefineOp);
        // Create the read operation
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, attribute);
        readOp.get("include-defaults").set(false);
        final ModelNode result = executeOperation(kernelServices, readOp);
        assertFalse("Attribute '" + attribute.getName() + "' was not undefined.", SubsystemOperations.readResult(result)
                .isDefined());
    }

    protected void verifyRemoved(final KernelServices kernelServices, final ModelNode address) {
        final ModelNode op = SubsystemOperations.createReadResourceOperation(address);
        final ModelNode result = kernelServices.executeOperation(op);
        assertFalse("Resource not removed: " + address, SubsystemOperations.isSuccessfulOutcome(result));
    }

    protected ModelNode executeOperation(final KernelServices kernelServices, final ModelNode op) {
        final ModelNode result = kernelServices.executeOperation(op);
        assertTrue(SubsystemOperations.getFailureDescriptionAsString(result), SubsystemOperations.isSuccessfulOutcome(result));
        return result;
    }

    protected ModelNode createFileValue(final String relativeTo, final String path) {
        final ModelNode file = new ModelNode().setEmptyObject();
        file.get(PathResourceDefinition.RELATIVE_TO.getName()).set(relativeTo);
        file.get(PathResourceDefinition.PATH.getName()).set(path);
        return file;
    }

    static void verifyFile(final String filename) {
        final File logFile = new File(logDir, filename);
        assertTrue("Log file was not found", logFile.exists());
    }

    static void removeFile(final String filename) {
        final File logFile = new File(logDir, filename);
        assertTrue("Log file was not deleted", logFile.delete());
    }
}
