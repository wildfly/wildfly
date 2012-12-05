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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.LogContext;
import org.junit.Assert;
import org.junit.Ignore;
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

    @Ignore("Write of logging configuration is broken")
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
        ModelNode op = Operations.createWriteAttributeOperation(consoleAddress.toModelNode(), CommonAttributes.FORMATTER, "%K{level}" + formatPattern);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFormatter(mainServices, modelVersion, consoleAddress.toModelNode());

        // Test update properties
        op = Operations.createOperation(AbstractHandlerDefinition.UPDATE_OPERATION_NAME, consoleAddress.toModelNode());
        op.get(CommonAttributes.FORMATTER.getName()).set("%K{level}" + formatPattern);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFormatter(mainServices, modelVersion, consoleAddress.toModelNode());

        // Write out a filter-spec
        final String filterExpression = "not(match(\"ARJUNA\\\\d\"))";
        op = Operations.createWriteAttributeOperation(consoleAddress.toModelNode(), CommonAttributes.FILTER_SPEC, filterExpression);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFilter(mainServices, modelVersion, consoleAddress.toModelNode(), filterExpression);

        // update-propertes on a filter spec
        op = Operations.createOperation(AbstractHandlerDefinition.UPDATE_OPERATION_NAME, consoleAddress.toModelNode());
        op.get(CommonAttributes.FILTER_SPEC.getName()).set(filterExpression);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFilter(mainServices, modelVersion, consoleAddress.toModelNode(), filterExpression);

        final PathAddress loggerAddress = createLoggerAddress("org.jboss.as.logging");
        // Verify the logger exists, add if it doesn't
        op = Operations.createReadResourceOperation(loggerAddress.toModelNode());
        if (!Operations.successful(mainServices.executeOperation(op))) {
            op = Operations.createAddOperation(loggerAddress.toModelNode());
            executeTransformOperation(mainServices, modelVersion, op);
        }

        // write a filter-spec
        op = Operations.createWriteAttributeOperation(loggerAddress.toModelNode(), CommonAttributes.FILTER_SPEC, filterExpression);
        executeTransformOperation(mainServices, modelVersion, op);
        validateLegacyFilter(mainServices, modelVersion, loggerAddress.toModelNode(), filterExpression);
    }

    private static ModelNode executeTransformOperation(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode op) throws OperationFailedException {
        TransformedOperation transformedOp = kernelServices.transformOperation(modelVersion, op);
        ModelNode result = kernelServices.executeOperation(modelVersion, transformedOp);
        Assert.assertTrue(result.asString(), Operations.successful(result));
        return result;
    }

    private static void validateLegacyFormatter(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode address) throws OperationFailedException {
        final ModelNode op = Operations.createReadAttributeOperation(address, CommonAttributes.FORMATTER);
        final ModelNode result = executeTransformOperation(kernelServices, modelVersion, op);
        Assert.assertTrue(result.asString(), Operations.successful(result));
        final String formatPattern = Operations.readResultAsString(result);
        Assert.assertFalse("Pattern (" + formatPattern + ") contains a color attribute not supported in legacy models.", COLOR_PATTERN.matcher(formatPattern).find());
    }

    private static void validateLegacyFilter(final KernelServices kernelServices, final ModelVersion modelVersion, final ModelNode address, final String filterExpression) throws OperationFailedException {
        ModelNode op = Operations.createReadResourceOperation(address);
        ModelNode result = executeTransformOperation(kernelServices, modelVersion, op);
        // No filter-spec should be there
        Assert.assertFalse("filter-spec found at: " + address.asString(), result.has(CommonAttributes.FILTER_SPEC.getName()));

        op = Operations.createReadAttributeOperation(address, CommonAttributes.FILTER);
        result = executeTransformOperation(kernelServices, modelVersion, op);
        Assert.assertEquals("Transformed spec does not match filter expression.", Filters.filterToFilterSpec(Operations.readResult(result)), filterExpression);
    }
}
