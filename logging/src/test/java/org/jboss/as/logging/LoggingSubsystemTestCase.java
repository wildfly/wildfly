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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.SubsystemOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
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
    public void testTransformers_1_1() throws Exception {
        final String subsystemXml = getSubsystemXml();
        final ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        final KernelServicesBuilder builder = createKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance())
                .setSubsystemXml(subsystemXml);

        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance(), modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-logging:7.1.2.Final");

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());

        final ModelNode legacyModel = checkSubsystemModelTransformation(mainServices, modelVersion);

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
        addRemoveHandler(mainServices, modelVersion, asyncHandlerAddress.toModelNode(), CommonAttributes.SUBHANDLERS, handlerName);
    }

    private static ModelNode executeTransformOperation(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode op) throws OperationFailedException {
        TransformedOperation transformedOp = kernelServices.transformOperation(modelVersion, op);
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
        Assert.assertTrue("Handler (" + handlerName + ") not found on the resource: " + address , SubsystemOperations.readResultAsList(result).contains(handlerName));

        // Remove the handler
        op = SubsystemOperations.createOperation(CommonAttributes.REMOVE_HANDLER_OPERATION_NAME, address);
        op.get(CommonAttributes.HANDLER_NAME.getName()).set(handlerName);
        executeTransformOperation(kernelServices, modelVersion, op);

        // Verify the handler was removed
        result = executeTransformOperation(kernelServices, modelVersion, readOp);
        Assert.assertFalse("Handler (" + handlerName + ") was not removed on the resource: " + address , SubsystemOperations.readResultAsList(result).contains(handlerName));
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
