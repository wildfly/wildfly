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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.Operations.CompositeOperationBuilder;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.FailedOperationTransformationConfig.RejectExpressionsConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingSubsystemTestCase extends AbstractLoggingSubsystemTest {

    // Color pattern
    private static final Pattern COLOR_PATTERN = Pattern.compile("(%K\\{[a-zA-Z]*?})");

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/logging.xml");
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("/expressions.xml");
    }

    @Test
    public void testConfiguration() throws Exception {
        final KernelServices kernelServices = boot();
        final ModelNode currentModel = getSubsystemModel(kernelServices);
        compare(currentModel, ConfigurationPersistence.getConfigurationPersistence(LogContext.getLogContext()));

        // Compare properties written out to current model
        final String dir = resolveRelativePath(kernelServices, "jboss.server.config.dir");
        Assert.assertNotNull("jboss.server.config.dir could not be resolved", dir);
        final LogContext logContext = LogContext.create();
        final ConfigurationPersistence config = ConfigurationPersistence.getOrCreateConfigurationPersistence(logContext);
        final FileInputStream in = new FileInputStream(new File(dir, "logging.properties"));
        config.configure(in);
        compare(currentModel, config);
    }

    @Test
    public void testTransformers712() throws Exception {
        testTransformer1_1_0("org.jboss.as:jboss-as-logging:7.1.2.Final");
    }

    @Test
    public void testTransformers713() throws Exception {
        testTransformer1_1_0("org.jboss.as:jboss-as-logging:7.1.3.Final");
    }

    @Test
    public void testRejectExpressions712() throws Exception {
        testRejectExpressions1_1_0("org.jboss.as:jboss-as-logging:7.1.2.Final");
    }

    @Test
    public void testRejectExpressions713() throws Exception {
        testRejectExpressions1_1_0("org.jboss.as:jboss-as-logging:7.1.3.Final");
    }

    private void testTransformer1_1_0(final String gav) throws Exception {
        final String subsystemXml = readResource("/logging_1_1.xml");
        final ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        final KernelServicesBuilder builder = createKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance())
                .setSubsystemXml(subsystemXml);

        // Create the legacy kernel
        builder.createLegacyKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance(), modelVersion)
                .addMavenResourceURL(gav)
                .configureReverseControllerCheck(LoggingTestEnvironment.getManagementInstance(), null);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);
        Assert.assertNotNull(legacyServices);
        final ModelNode legacyModel = checkSubsystemModelTransformation(mainServices, modelVersion);

        testTransformOperations(mainServices, modelVersion, legacyModel);
    }

    private void testRejectExpressions1_1_0(final String gav) throws Exception {
        final ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        final KernelServicesBuilder builder = createKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance());

        // Create the legacy kernel
        builder.createLegacyKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance(), modelVersion)
                .addMavenResourceURL(gav);

        KernelServices mainServices = builder.build();
        KernelServices legacyServices = mainServices.getLegacyServices(modelVersion);

        Assert.assertNotNull(legacyServices);
        Assert.assertTrue("main services did not boot", mainServices.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        final List<ModelNode> ops = builder.parseXmlResource("/expressions.xml");
        ModelTestUtils.checkFailedTransformedBootOperations(mainServices, modelVersion, ops,
                new FailedOperationTransformationConfig()
                        .addFailedAttribute(createRootLoggerAddress(),
                                new RejectExpressionsConfig(RootLoggerResourceDefinition.EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(LoggerResourceDefinition.LOGGER_PATH),
                                new RejectExpressionsConfig(LoggerResourceDefinition.EXPRESSION_ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(AsyncHandlerResourceDefinition.ASYNC_HANDLER_PATH),
                                new RejectExpressionsConfig(AsyncHandlerResourceDefinition.ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(ConsoleHandlerResourceDefinition.CONSOLE_HANDLER_PATH),
                                new RejectExpressionsConfig(ConsoleHandlerResourceDefinition.ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(FileHandlerResourceDefinition.FILE_HANDLER_PATH),
                                new RejectExpressionsConfig(FileHandlerResourceDefinition.ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(FileHandlerResourceDefinition.FILE_HANDLER_PATH),
                                FailedOperationTransformationConfig.ChainedConfig.createBuilder(FileHandlerResourceDefinition.ATTRIBUTES)
                                .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(CommonAttributes.ENABLED))
                                .addConfig(new RejectExpressionsConfig(FileHandlerResourceDefinition.ATTRIBUTES))
                                .build())
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(PeriodicHandlerResourceDefinition.PERIODIC_HANDLER_PATH),
                                new RejectExpressionsConfig(PeriodicHandlerResourceDefinition.ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(SizeRotatingHandlerResourceDefinition.SIZE_ROTATING_HANDLER_PATH),
                                new RejectExpressionsConfig(SizeRotatingHandlerResourceDefinition.ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(CustomHandlerResourceDefinition.CUSTOM_HANDLE_PATH),
                                new RejectExpressionsConfig(CustomHandlerResourceDefinition.WRITABLE_ATTRIBUTES))
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(SyslogHandlerResourceDefinition.SYSLOG_HANDLER_PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(CommonAttributes.LOGGING_PROFILE),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(CommonAttributes.LOGGING_PROFILE).append(ConsoleHandlerResourceDefinition.CONSOLE_HANDLER_PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(CommonAttributes.LOGGING_PROFILE).append(RootLoggerResourceDefinition.ROOT_LOGGER_PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(CommonAttributes.LOGGING_PROFILE).append(LoggerResourceDefinition.LOGGER_PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
                        .addFailedAttribute(SUBSYSTEM_ADDRESS.append(CommonAttributes.LOGGING_PROFILE).append(SyslogHandlerResourceDefinition.SYSLOG_HANDLER_PATH),
                                FailedOperationTransformationConfig.REJECTED_RESOURCE)
        );
    }

    private void testTransformOperations(final KernelServices mainServices, final ModelVersion modelVersion, final ModelNode legacyModel) throws Exception {

        final PathAddress consoleAddress = createConsoleHandlerAddress("CONSOLE");
        // Get all the console handler
        final ModelNode consoleHandler = legacyModel.get(consoleAddress.getElement(0).getKey(), consoleAddress.getElement(0).getValue(),
                consoleAddress.getElement(1).getKey(), consoleAddress.getElement(1).getValue());
        String formatPattern = consoleHandler.get(CommonAttributes.FORMATTER.getName()).asString();
        Assert.assertFalse("Pattern (" + formatPattern + ") contains a color attribute not supported in legacy models.", COLOR_PATTERN.matcher(formatPattern).find());

        // Write a pattern with a %K{level} to ensure it gets removed
        ModelNode op = SubsystemOperations.createWriteAttributeOperation(consoleAddress.toModelNode(), CommonAttributes.FORMATTER, "%K{level}" + formatPattern);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFormatter(mainServices, modelVersion, consoleAddress.toModelNode());

        // Test update properties
        op = SubsystemOperations.createOperation(AbstractHandlerDefinition.UPDATE_OPERATION_NAME, consoleAddress.toModelNode());
        op.get(CommonAttributes.FORMATTER.getName()).set("%K{level}" + formatPattern);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFormatter(mainServices, modelVersion, consoleAddress.toModelNode());

        // Write out a filter-spec
        final String filterExpression = "not(match(\"ARJUNA\\\\d\"))";
        op = SubsystemOperations.createWriteAttributeOperation(consoleAddress.toModelNode(), CommonAttributes.FILTER_SPEC, filterExpression);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFilter(mainServices, modelVersion, consoleAddress.toModelNode(), filterExpression);

        // update-propertes on a filter spec
        op = SubsystemOperations.createOperation(AbstractHandlerDefinition.UPDATE_OPERATION_NAME, consoleAddress.toModelNode());
        op.get(CommonAttributes.FILTER_SPEC.getName()).set(filterExpression);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFilter(mainServices, modelVersion, consoleAddress.toModelNode(), filterExpression);

        final PathAddress loggerAddress = createLoggerAddress("org.jboss.as.logging");
        // Verify the logger exists, add if it doesn't
        op = SubsystemOperations.createReadResourceOperation(loggerAddress.toModelNode());
        if (!SubsystemOperations.isSuccessfulOutcome(mainServices.executeOperation(op))) {
            op = SubsystemOperations.createAddOperation(loggerAddress.toModelNode());
            executeTransformOperation(mainServices, modelVersion, op);
        }

        // write a filter-spec
        op = SubsystemOperations.createWriteAttributeOperation(loggerAddress.toModelNode(), CommonAttributes.FILTER_SPEC, filterExpression);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFilter(mainServices, modelVersion, loggerAddress.toModelNode(), filterExpression);

        // create a new handler
        final PathAddress handlerAddress = createFileHandlerAddress("fh");
        op = SubsystemOperations.createAddOperation(handlerAddress.toModelNode());
        op.get(CommonAttributes.FILE.getName(), PathResourceDefinition.RELATIVE_TO.getName()).set("jboss.server.log.dir");
        op.get(CommonAttributes.FILE.getName(), PathResourceDefinition.PATH.getName()).set("fh.log");
        op.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        executeTransformOperation(mainServices, modelVersion, op);

        // add/remove the handler to the root logger
        final PathAddress rootLoggerAddress = createRootLoggerAddress();
        final String handlerName = handlerAddress.getLastElement().getValue();
        addRemoveHandler(mainServices, modelVersion, rootLoggerAddress.toModelNode(), CommonAttributes.HANDLERS, handlerName);

        // add/remove the handler to a logger
        addRemoveHandler(mainServices, modelVersion, loggerAddress.toModelNode(), CommonAttributes.HANDLERS, handlerName);

        // add/remove from an async-handler
        final PathAddress asyncHandlerAddress = createAsyncHandlerAddress("async");
        addRemoveHandler(mainServices, modelVersion, asyncHandlerAddress.toModelNode(), AsyncHandlerResourceDefinition.SUBHANDLERS, handlerName);

        // Test a composite operation
        final CompositeOperationBuilder compositeOperationBuilder = CompositeOperationBuilder.create();

        // Add a logger which should add a category attribute after transformation
        final ModelNode compositeLoggerAddress = createLoggerAddress("compositeLogger").toModelNode();
        compositeOperationBuilder.addStep(SubsystemOperations.createAddOperation(compositeLoggerAddress));

        // Remove the file handler, then re-add it with the enabled attribute that should be removed
        compositeOperationBuilder.addStep(SubsystemOperations.createRemoveOperation(handlerAddress.toModelNode()));
        op = SubsystemOperations.createAddOperation(handlerAddress.toModelNode());
        op.get(CommonAttributes.FILE.getName(), PathResourceDefinition.RELATIVE_TO.getName()).set("jboss.server.log.dir");
        op.get(CommonAttributes.FILE.getName(), PathResourceDefinition.PATH.getName()).set("fh.log");
        op.get(CommonAttributes.AUTOFLUSH.getName()).set(true);
        op.get(CommonAttributes.ENABLED.getName()).set(true);
        compositeOperationBuilder.addStep(op);

        compositeOperationBuilder.addStep(SubsystemOperations.createAddOperation(createAddress(CommonAttributes.LOGGING_PROFILE, "composite-profile").toModelNode()));
        final ModelNode composite = compositeOperationBuilder.build().getOperation();
        // Transform the operation
        final TransformedOperation transformedOp = mainServices.transformOperation(modelVersion, composite);
        op = transformedOp.getTransformedOperation();
        // Iterate the steps and look for specific changes in the operation
        final List<ModelNode> steps = op.get(ClientConstants.STEPS).asList();

        // There should only be 3 steps as the logging-profile should not have been removed
        Assert.assertEquals("Logging profile step should not have been removed", 4, steps.size());

        // First step should be the logger
        ModelNode stepOp = steps.get(0);
        // Verify the category was added
        Assert.assertTrue("category attribute should have been added", stepOp.hasDefined(LoggerResourceDefinition.CATEGORY.getName()));

        // Third operation should the adding the handler
        stepOp = steps.get(2);
        // Verify the enabled attribute was removed
        Assert.assertFalse("enabled attribute should have been removed", stepOp.hasDefined(CommonAttributes.ENABLED.getName()));

        ModelTestUtils.checkFailed(mainServices.executeOperation(modelVersion, transformedOp));

        //Check that the rejected step was the one that got rejected
        final ModelNode success = new ModelNode();
        success.get(OUTCOME).set(SUCCESS);
        Assert.assertTrue(transformedOp.rejectOperation(success));

        final List<ModelNode> newSteps = new ArrayList<ModelNode>(steps);
        newSteps.remove(3);
        final ModelNode newComposite = composite.clone();
        newComposite.get(STEPS).set(newSteps);
        final TransformedOperation newTransformedOperation = mainServices.transformOperation(modelVersion, newComposite);

        Assert.assertFalse(newTransformedOperation.rejectOperation(success));
    }

    private static ModelNode executeTransformOperation(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode op) throws OperationFailedException {
        return executeTransformOperation(kernelServices, modelVersion, kernelServices.transformOperation(modelVersion, op));
    }

    private static ModelNode executeTransformOperation(final KernelServices kernelServices, final ModelVersion modelVersion, final TransformedOperation transformedOp) throws OperationFailedException {
        ModelNode result = kernelServices.executeOperation(modelVersion, transformedOp);
        Assert.assertTrue(result.asString(), SubsystemOperations.isSuccessfulOutcome(result));
        return result;
    }

    private static void addRemoveHandler(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode address,
                                         final AttributeDefinition handlerAttribute, final String handlerName) throws OperationFailedException {
        // Add the handler
        ModelNode op = SubsystemOperations.createOperation(CommonAttributes.ADD_HANDLER_OPERATION_NAME, address);
        op.get(CommonAttributes.HANDLER_NAME.getName()).set(handlerName);
        executeTransformOperation(kernelServices, modelVersion, op);

        // Verify the handler was added
        final ModelNode readOp = SubsystemOperations.createReadAttributeOperation(address, handlerAttribute);
        ModelNode result = executeTransformOperation(kernelServices, modelVersion, readOp);
        Assert.assertTrue("Handler (" + handlerName + ") not found on the resource: " + address, SubsystemOperations.readResultAsList(result).contains(handlerName));

        // Remove the handler
        op = SubsystemOperations.createOperation(CommonAttributes.REMOVE_HANDLER_OPERATION_NAME, address);
        op.get(CommonAttributes.HANDLER_NAME.getName()).set(handlerName);
        executeTransformOperation(kernelServices, modelVersion, op);

        // Verify the handler was removed
        result = executeTransformOperation(kernelServices, modelVersion, readOp);
        Assert.assertFalse("Handler (" + handlerName + ") was not removed on the resource: " + address, SubsystemOperations.readResultAsList(result).contains(handlerName));
    }

    private static void validateLegacyFormatter(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode address) throws OperationFailedException {
        final ModelNode op = SubsystemOperations.createReadAttributeOperation(address, CommonAttributes.FORMATTER);
        final ModelNode result = executeTransformOperation(kernelServices, modelVersion, op);
        Assert.assertTrue(result.asString(), SubsystemOperations.isSuccessfulOutcome(result));
        final String formatPattern = SubsystemOperations.readResultAsString(result);
        Assert.assertFalse("Pattern (" + formatPattern + ") contains a color attribute not supported in legacy models.", COLOR_PATTERN.matcher(formatPattern).find());
    }

    private static void validateLegacyFilter(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode address, final String filterExpression) throws OperationFailedException {
        ModelNode op = SubsystemOperations.createReadResourceOperation(address);
        ModelNode result = executeTransformOperation(kernelServices, modelVersion, op);
        // No filter-spec should be there
        Assert.assertFalse("filter-spec found at: " + address.asString(), result.has(CommonAttributes.FILTER_SPEC.getName()));

        op = SubsystemOperations.createReadAttributeOperation(address, CommonAttributes.FILTER);
        result = executeTransformOperation(kernelServices, modelVersion, op);
        Assert.assertEquals("Transformed spec does not match filter expression.", Filters.filterToFilterSpec(SubsystemOperations.readResult(result)), filterExpression);
    }
}
