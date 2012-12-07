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
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.logging.Operations.CompositeOperationBuilder;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(BMUnitRunner.class)
public class LoggingSubsystemRollbackTestCase extends AbstractLoggingSubsystemTest {
    private static final String PROFILE_NAME = "test-profile";

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/rollback-logging.xml");
    }

    @Override
    protected void standardSubsystemTest(final String configId) throws Exception {
        // do nothing as this is not a subsystem parsing test
    }

    @After
    public void cleanUp() throws Exception {
        cleanUp(LogContext.getLogContext());
        final LoggingProfileContextSelector contextSelector = LoggingProfileContextSelector.getInstance();
        if (contextSelector.exists(PROFILE_NAME)) {
            cleanUp(contextSelector.get(PROFILE_NAME));
            contextSelector.remove(PROFILE_NAME);
        }
    }

    private void cleanUp(final LogContext logContext) {
        final ConfigurationPersistence configuration = ConfigurationPersistence.getConfigurationPersistence(logContext);
        if (configuration != null) {
            final LogContextConfiguration logContextConfiguration = configuration.getLogContextConfiguration();
            // Remove all loggers
            for (String loggerName : logContextConfiguration.getLoggerNames()) {
                logContextConfiguration.removeLoggerConfiguration(loggerName);
            }
            // Remove all the handlers
            for (String handlerName : logContextConfiguration.getHandlerNames()) {
                logContextConfiguration.removeHandlerConfiguration(handlerName);
            }
            // Remove all the filters
            for (String filterName : logContextConfiguration.getFilterNames()) {
                logContextConfiguration.removeFilterConfiguration(filterName);
            }
            // Remove all the formatters
            for (String formatterName : logContextConfiguration.getFormatterNames()) {
                logContextConfiguration.removeFormatterConfiguration(formatterName);
            }
            // Remove all the error managers
            for (String errorManager : logContextConfiguration.getErrorManagerNames()) {
                logContextConfiguration.removeErrorManagerConfiguration(errorManager);
            }
            configuration.commit();
        }
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

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        // The logger address
        final PathAddress address = createLoggerAddress("org.jboss.as.logging.test");

        // Operation should fail based on byteman script
        ModelNode op = Operations.createAddOperation(address.toModelNode());
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // Verify the loggers are not there - operation should fail on missing resource
        op = Operations.createReadResourceOperation(address.toModelNode());
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(validSubsystemModel, currentModel);

        final ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext());
        compare(currentModel, config);
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

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        // Handler address
        final PathAddress address = createConsoleHandlerAddress("CONSOLE2");
        // Operation should fail based on byteman script
        ModelNode op = Operations.createAddOperation(address.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.FORMATTER.getName()).set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) CONSOLE2: %s%E%n");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // Verify the loggers are not there - operation should fail on missing resource
        op = Operations.createReadResourceOperation(address.toModelNode());
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(validSubsystemModel, currentModel);

        final ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext());
        compare(currentModel, config);
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
        final PathAddress consoleHandler = createConsoleHandlerAddress("CONSOLE2");
        // Create a new handler
        ModelNode op = Operations.createAddOperation(consoleHandler.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.FORMATTER.getName()).set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) CONSOLE2: %s%E%n");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));

        // Add the handler to a logger
        final PathAddress loggerAddress = createLoggerAddress("org.jboss.as.logging");
        op = Operations.createOperation(LoggerResourceDefinition.ADD_HANDLER_OPERATION_NAME, loggerAddress.toModelNode());
        op.get(CommonAttributes.HANDLER_NAME.getName()).set(consoleHandler.getLastElement().getValue());
        result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);


        final CompositeOperationBuilder operationBuilder = CompositeOperationBuilder.create();

        // create a new handler
        final PathAddress fileHandlerAddress = createFileHandlerAddress("fail-fh");
        final ModelNode fileHandlerOp = Operations.createAddOperation(fileHandlerAddress.toModelNode());
        fileHandlerOp.get(CommonAttributes.FILE.getName(), PathResourceDefinition.RELATIVE_TO.getName()).set("jboss.server.log.dir");
        fileHandlerOp.get(CommonAttributes.FILE.getName(), PathResourceDefinition.PATH.getName()).set("fail-fh.log");
        fileHandlerOp.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        operationBuilder.addStep(fileHandlerOp);

        // create a new logger
        final PathAddress testLoggerAddress = createLoggerAddress("test");
        final ModelNode testLoggerOp = Operations.createAddOperation(testLoggerAddress.toModelNode());
        operationBuilder.addStep(testLoggerOp);

        // add handler to logger
        operationBuilder.addStep(Operations.createWriteAttributeOperation(testLoggerAddress.toModelNode(), CommonAttributes.HANDLERS, new ModelNode().setEmptyList().add("fail-fh")));

        // remove the console handler
        operationBuilder.addStep(Operations.createRemoveOperation(consoleHandler.toModelNode(), false));

        // add handler to existing logger - should force fail on this one
        operationBuilder.addStep(Operations.createWriteAttributeOperation(loggerAddress.toModelNode(), CommonAttributes.HANDLERS, new ModelNode().setEmptyList().add("fail-fh")));

        // verify the operation failed
        result = kernelServices.executeOperation(operationBuilder.build().getOperation());
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(validSubsystemModel, currentModel);

        final ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext());
        compare(currentModel, config);
    }

    @Test
    @BMRule(name = "Test handler rollback handler",
            targetClass = "org.jboss.as.logging.LoggerOperations$LoggerWriteAttributeHandler",
            targetMethod = "applyUpdate",
            targetLocation = "AT EXIT",
            condition = "$3.equals(\"org.jboss.as.logging\")",
            action = "$1.setRollbackOnly()")
    public void testRollbackCompositeLoggingProfile() throws Exception {
        final KernelServices kernelServices = boot();

        // Add a handler to be removed
        final PathAddress consoleHandler = createConsoleHandlerAddress("CONSOLE2");
        // Create a new handler
        ModelNode op = Operations.createAddOperation(consoleHandler.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.FORMATTER.getName()).set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) CONSOLE2: %s%E%n");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));

        // Add the handler to a logger
        final PathAddress loggerAddress = createLoggerAddress("org.jboss.as.logging");
        op = Operations.createOperation(LoggerResourceDefinition.ADD_HANDLER_OPERATION_NAME, loggerAddress.toModelNode());
        op.get(CommonAttributes.HANDLER_NAME.getName()).set(consoleHandler.getLastElement().getValue());
        result = kernelServices.executeOperation(op);
        Assert.assertTrue(Operations.getFailureDescription(result), Operations.successful(result));

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        final CompositeOperationBuilder operationBuilder = CompositeOperationBuilder.create();

        // create a new handler
        final PathAddress fileHandlerAddress = createFileHandlerAddress("fail-fh");
        final ModelNode fileHandlerOp = Operations.createAddOperation(fileHandlerAddress.toModelNode());
        fileHandlerOp.get(CommonAttributes.FILE.getName(), PathResourceDefinition.RELATIVE_TO.getName()).set("jboss.server.log.dir");
        fileHandlerOp.get(CommonAttributes.FILE.getName(), PathResourceDefinition.PATH.getName()).set("fail-fh.log");
        fileHandlerOp.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        operationBuilder.addStep(fileHandlerOp);

        // create a new logger
        final PathAddress testLoggerAddress = createLoggerAddress(PROFILE_NAME, "test");
        final ModelNode testLoggerOp = Operations.createAddOperation(testLoggerAddress.toModelNode());
        operationBuilder.addStep(testLoggerOp);

        // remove the console handler
        operationBuilder.addStep(Operations.createRemoveOperation(consoleHandler.toModelNode(), false));

        // add handler to existing logger - should force fail on this one
        operationBuilder.addStep(Operations.createWriteAttributeOperation(loggerAddress.toModelNode(), CommonAttributes.HANDLERS, new ModelNode().setEmptyList().add("fail-fh")));

        // verify the operation failed
        result = kernelServices.executeOperation(operationBuilder.build().getOperation());
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(validSubsystemModel, currentModel);

        final ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext());
        compare(currentModel, config);
    }

    @Test
    @BMRules(rules = {
            @BMRule(name = "Test handler rollback handler",
                    targetClass = "org.jboss.as.logging.HandlerOperations$HandlerAddOperationStepHandler",
                    targetMethod = "performRuntime",
                    targetLocation = "AT EXIT",
                    condition = "$4.equals(\"CONSOLE2\")",
                    action = "$1.setRollbackOnly()"),
            @BMRule(name = "Test logger rollback handler",
                    targetClass = "org.jboss.as.logging.LoggerOperations$LoggerAddOperationStepHandler",
                    targetMethod = "performRuntime",
                    targetLocation = "AT EXIT",
                    condition = "$4.equals(\"org.jboss.as.logging.test\")",
                    action = "$1.setRollbackOnly()")
    })
    public void testRollbackAdd() throws Exception {
        rollbackAdd(null);
        rollbackAdd(PROFILE_NAME);
    }

    @Test
    @BMRules(rules = {
            @BMRule(name = "Test handler write-attribute rollback handler",
                    targetClass = "org.jboss.as.logging.HandlerOperations$LogHandlerWriteAttributeHandler",
                    targetMethod = "applyUpdate",
                    targetLocation = "AT EXIT",
                    condition = "$3.equals(\"CONSOLE\")",
                    action = "$1.setRollbackOnly()"),
            @BMRule(name = "Test logger write-attribute rollback handler",
                    targetClass = "org.jboss.as.logging.LoggerOperations$LoggerWriteAttributeHandler",
                    targetMethod = "applyUpdate",
                    targetLocation = "AT EXIT",
                    condition = "$3.equals(\"ROOT\")",
                    action = "$1.setRollbackOnly()")
    })
    public void testRollbackWriteAttribute() throws Exception {
        rollbackWriteAttribute(null);
        rollbackWriteAttribute(PROFILE_NAME);
    }

    @Test
    @BMRules(rules = {
            @BMRule(name = "Test handler rollback handler",
                    targetClass = "org.jboss.as.logging.HandlerOperations$HandlerUpdateOperationStepHandler",
                    targetMethod = "performRuntime",
                    targetLocation = "AT EXIT",
                    condition = "$4.equals(\"CONSOLE\")",
                    action = "$1.setRollbackOnly()"),
            @BMRule(name = "Test logger rollback handler",
                    targetClass = "org.jboss.as.logging.LoggerOperations$LoggerUpdateOperationStepHandler",
                    targetMethod = "performRuntime",
                    targetLocation = "AT EXIT",
                    condition = "$4.equals(\"ROOT\")",
                    action = "$1.setRollbackOnly()")
    })
    public void testRollbackUpdateAttribute() throws Exception {
        rollbackUpdateAttribute(null);
        rollbackUpdateAttribute(PROFILE_NAME);
    }

    @Test
    @BMRules(rules = {
            @BMRule(name = "Test handler write-attribute rollback handler",
                    targetClass = "org.jboss.as.logging.LoggerOperations$LoggerWriteAttributeHandler",
                    targetMethod = "applyUpdate",
                    targetLocation = "AT EXIT",
                    condition = "$3.equals(\"ROOT\")",
                    action = "$1.setRollbackOnly()")
    })
    public void testRollbackRemove() throws Exception {
        rollbackRemove(null);
        rollbackRemove(PROFILE_NAME);
    }

    @Test
    @BMRules(rules = {
            @BMRule(name = "Test handler write-attribute rollback handler",
                    targetClass = "org.jboss.as.logging.LoggerOperations$LoggerWriteAttributeHandler",
                    targetMethod = "applyUpdate",
                    targetLocation = "AT EXIT",
                    condition = "$3.equals(\"ROOT\")",
                    action = "$1.setRollbackOnly()")
    })
    public void testRollbackRemoveProfile() throws Exception {
        final KernelServices kernelServices = boot();

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        final CompositeOperationBuilder compositeOperationBuilder = CompositeOperationBuilder.create();

        // The handler address to remove
        final PathAddress profileAddress = createAddress(CommonAttributes.LOGGING_PROFILE, PROFILE_NAME);
        // Remove the handler
        compositeOperationBuilder.addStep(Operations.createRemoveOperation(profileAddress.toModelNode(), true));

        // Add a step to fail
        final ModelNode rootLoggerAddress = createRootLoggerAddress().toModelNode();
        compositeOperationBuilder.addStep(Operations.createWriteAttributeOperation(rootLoggerAddress, CommonAttributes.LEVEL, "INFO"));

        ModelNode result = kernelServices.executeOperation(compositeOperationBuilder.build().getOperation());
        Assert.assertFalse("The update operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(validSubsystemModel, currentModel);

        ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext());
        compare(currentModel, config);
        // Check the profile was rolled back
        config = ConfigurationPersistence.getConfigurationPersistence(LoggingProfileContextSelector.getInstance().get(PROFILE_NAME));
        compare(PROFILE_NAME, currentModel, config);
    }

    public void rollbackAdd(final String profileName) throws Exception {
        final KernelServices kernelServices = boot();

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        // Add a handler to be removed
        final PathAddress consoleHandler = createConsoleHandlerAddress(profileName, "CONSOLE2");
        // Create a new handler
        ModelNode op = Operations.createAddOperation(consoleHandler.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("INFO");
        op.get(CommonAttributes.FORMATTER.getName()).set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) CONSOLE2: %s%E%n");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        final LogContext logContext = (profileName == null ? LogContext.getLogContext() : LoggingProfileContextSelector.getInstance().get(profileName));
        ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);

        // Fail on a logger write attribute
        final PathAddress loggerAddress = createLoggerAddress(profileName, "org.jboss.as.logging.test");
        op = Operations.createAddOperation(loggerAddress.toModelNode());
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The add operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);
    }

    public void rollbackWriteAttribute(final String profileName) throws Exception {
        final KernelServices kernelServices = boot();

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        // Add a handler to be removed
        final PathAddress consoleHandler = createConsoleHandlerAddress(profileName, "CONSOLE");
        // Create a new handler
        ModelNode op = Operations.createWriteAttributeOperation(consoleHandler.toModelNode(), CommonAttributes.TARGET, "System.err");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The write operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        final LogContext logContext = (profileName == null ? LogContext.getLogContext() : LoggingProfileContextSelector.getInstance().get(profileName));
        ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);

        // Fail on a logger write attribute
        final PathAddress rootLoggerAddress = createRootLoggerAddress(profileName);
        op = Operations.createWriteAttributeOperation(rootLoggerAddress.toModelNode(), CommonAttributes.LEVEL, "TRACE");
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The write operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);

    }

    public void rollbackUpdateAttribute(final String profileName) throws Exception {
        final KernelServices kernelServices = boot();

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        // Add a handler to be removed
        final PathAddress consoleHandler = createConsoleHandlerAddress(profileName, "CONSOLE");
        // Create a new handler
        ModelNode op = Operations.createOperation(AbstractHandlerDefinition.CHANGE_LEVEL_OPERATION_NAME, consoleHandler.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("DEBUG");
        ModelNode result = kernelServices.executeOperation(op);
        Assert.assertFalse("The update operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        final LogContext logContext = (profileName == null ? LogContext.getLogContext() : LoggingProfileContextSelector.getInstance().get(profileName));
        ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);

        // Fail on a logger write attribute
        final PathAddress rootLoggerAddress = createRootLoggerAddress(profileName);
        op = Operations.createOperation(RootLoggerResourceDefinition.ROOT_LOGGER_CHANGE_LEVEL_OPERATION_NAME, rootLoggerAddress.toModelNode());
        op.get(CommonAttributes.LEVEL.getName()).set("TRACE");
        result = kernelServices.executeOperation(op);
        Assert.assertFalse("The update operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);

    }

    public void rollbackRemove(final String profileName) throws Exception {

        final KernelServices kernelServices = boot();

        // Save the current model
        final ModelNode validSubsystemModel = getSubsystemModel(kernelServices);

        final CompositeOperationBuilder compositeOperationBuilder = CompositeOperationBuilder.create();

        // The handler address to remove
        final PathAddress consoleHandler = createConsoleHandlerAddress(profileName, "CONSOLE");
        // Remove the handler
        compositeOperationBuilder.addStep(Operations.createRemoveOperation(consoleHandler.toModelNode(), false));

        // The logger to remove
        final PathAddress loggerAddress = createLoggerAddress(profileName, "org.jboss.as.logging");
        compositeOperationBuilder.addStep(Operations.createRemoveOperation(loggerAddress.toModelNode(), false));

        // Add a step to fail
        final ModelNode rootLoggerAddress = createRootLoggerAddress(profileName).toModelNode();
        compositeOperationBuilder.addStep(Operations.createWriteAttributeOperation(rootLoggerAddress, CommonAttributes.LEVEL, "INFO"));

        ModelNode result = kernelServices.executeOperation(compositeOperationBuilder.build().getOperation());
        Assert.assertFalse("The update operation should have failed, but was successful: " + result, Operations.successful(result));

        // verify the subsystem model matches the old model
        ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(profileName, validSubsystemModel, currentModel);

        final LogContext logContext = (profileName == null ? LogContext.getLogContext() : LoggingProfileContextSelector.getInstance().get(profileName));
        ConfigurationPersistence config = ConfigurationPersistence.getConfigurationPersistence(logContext);
        compare(profileName, currentModel, config);

    }
}
