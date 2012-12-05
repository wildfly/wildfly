/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.logging.Operations.CompositeOperationBuilder;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Ignore("Rollback handlers are broken")
@RunWith(BMUnitRunner.class)
public class LoggingSubsystemRollbackTestCase extends AbstractLoggingSubsystemTest {

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/rollback-logging.xml");
    }

    @Test
    @BMRule(name = "Test logger rollback handler",
            targetClass = "org.jboss.as.logging.LoggerOperations$LoggerAddOperationStepHandler",
            targetMethod = "performRuntime",
            targetLocation = "AT EXIT",
            condition = "$4.equals(\"org.jboss.as.logging.test\")",
            action = "$1.setRollbackOnly()"
    )
    public void testRollbackLogger() throws Exception {
        final KernelServices kernelServices = boot();
        Assert.assertTrue("Failed to boot", kernelServices.isSuccessfulBoot());

        // The logger address
        final PathAddress address = createLoggerAddress("org.jboss.as.logging.test");

        // Operation should fail based on byteman script
        ModelNode op = Operations.createAddOperation(address.toModelNode());
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // Verify the loggers are not there
        op = Operations.createReadResourceOperation(address.toModelNode());
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));
    }

    @Test
    @BMRule(name = "Test handler rollback handler",
            targetClass = "org.jboss.as.logging.HandlerOperations$HandlerAddOperationStepHandler",
            targetMethod = "performRuntime",
            targetLocation = "AT EXIT",
            condition = "$4.equals(\"CONSOLE2\")",
            action = "$1.setRollbackOnly()")
    public void testRollbackHandler() throws Exception {
        final KernelServices kernelServices = boot();
        Assert.assertTrue("Failed to boot", kernelServices.isSuccessfulBoot());
        // Handler address
        final PathAddress address = createConsoleHandlerAddress("CONSOLE2");
        // Operation should fail based on byteman script
        ModelNode op = Operations.createAddOperation(address.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.FORMATTER.getName()).set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) CONSOLE2: %s%E%n");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // Verify the loggers are not there
        op = Operations.createReadResourceOperation(address.toModelNode());
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));
    }

    @Test
    @BMRule(name = "Test handler rollback handler",
            targetClass = "org.jboss.as.logging.LoggerOperations$LoggerWriteAttributeHandler",
            targetMethod = "applyUpdate",
            targetLocation = "AT EXIT",
            condition = "$3.equals(\"org.jboss.as.logging\")",
            action = "$1.setRollbackOnly()")
    public void testRollbackComposite() throws Exception {
        final KernelServices kernelServices = boot();

        // Add a handler to be removed
        final PathAddress console2Handler = createConsoleHandlerAddress("CONSOLE2");
        // Create a new handler
        ModelNode op = Operations.createAddOperation(console2Handler.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.FORMATTER.getName()).set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) CONSOLE2: %s%E%n");
        op = Operations.createAddOperation(console2Handler.toModelNode());
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));

        // Add the handler to a logger
        final PathAddress loggerAddress = createLoggerAddress("org.jboss.as.logging");
        op = Operations.createOperation(LoggerResourceDefinition.ADD_HANDLER_OPERATION_NAME, loggerAddress.toModelNode());
        op.get(CommonAttributes.HANDLER_NAME.getName()).set(console2Handler.getLastElement().getValue());
        result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);


        final CompositeOperationBuilder operationBuilder = CompositeOperationBuilder.create();

        // create a new handler
        final PathAddress fileHandlerAddress = createFileHandlerAddress("fail-fh");
        final ModelNode fileHandlerOp = Operations.createAddOperation(fileHandlerAddress.toModelNode());
        fileHandlerOp.get(CommonAttributes.FILE.getName(), CommonAttributes.RELATIVE_TO.getName()).set("jboss.server.log.dir");
        fileHandlerOp.get(CommonAttributes.FILE.getName(), CommonAttributes.PATH.getName()).set("fail-fh.log");
        fileHandlerOp.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        operationBuilder.addStep(fileHandlerOp);

        // create a new logger
        final PathAddress testLoggerAddress = createLoggerAddress("test");
        final ModelNode testLoggerOp = Operations.createAddOperation(testLoggerAddress.toModelNode());
        operationBuilder.addStep(testLoggerOp);

        // add handler to logger
        operationBuilder.addStep(Operations.createWriteAttributeOperation(testLoggerAddress.toModelNode(), CommonAttributes.HANDLERS, new ModelNode().setEmptyList().add("fail-fh")));

        // remove the console2 handler
        operationBuilder.addStep(Operations.createRemoveOperation(console2Handler.toModelNode(), false));

        // add handler to existing logger - should force fail on this one
        operationBuilder.addStep(Operations.createWriteAttributeOperation(loggerAddress.toModelNode(), CommonAttributes.HANDLERS, new ModelNode().setEmptyList().add("fail-fh")));

        // verify the operation failed
        result = kernelServices.executeOperation(operationBuilder.build().getOperation());
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // TODO rollback is failing, but not asserting the failure on the test

        // verify the subsystem model matches the old model
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(validSubsystemModel, currentModel);

        final ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext());
        compare(currentModel, config);
    }
}
