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

import java.io.IOException;
import java.util.Comparator;
import java.util.regex.Pattern;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LoggingSubsystemTestCase extends AbstractSubsystemBaseTest {

    // Color pattern
    private static final Pattern COLOR_PATTERN = Pattern.compile("(%K\\{[a-zA-Z]*?})");

    @BeforeClass
    public static void setUp() {
        // Just need to set-up the test environment
        LoggingTestEnvironment.get();
    }

    public LoggingSubsystemTestCase() {
        super(LoggingExtension.SUBSYSTEM_NAME, new LoggingExtension(), RemoveOperationComparator.INSTANCE);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/logging.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return LoggingTestEnvironment.get();
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
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

        final PathAddress consoleAddress = PathAddress.pathAddress(
                LoggingTestEnvironment.SUBSYSTEM_PATH,
                PathElement.pathElement(CommonAttributes.CONSOLE_HANDLER, "CONSOLE")
        );
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

        final PathAddress loggerAddress = PathAddress.pathAddress(
                LoggingTestEnvironment.SUBSYSTEM_PATH,
                PathElement.pathElement(CommonAttributes.LOGGER, "org.jboss.as.logging")
                );
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


    static class RemoveOperationComparator implements Comparator<PathAddress> {
        static final RemoveOperationComparator INSTANCE = new RemoveOperationComparator();
        static final int GREATER = 1;
        static final int EQUAL = 0;
        static final int LESS = -1;

        @Override
        public int compare(final PathAddress o1, final PathAddress o2) {
            final String key1 = o1.getLastElement().getKey();
            final String key2 = o2.getLastElement().getKey();
            int result = key1.compareTo(key2);
            if (result != EQUAL) {
                if (LoggingProfileOperations.isLoggingProfileAddress(o1) && !LoggingProfileOperations.isLoggingProfileAddress(o2)) {
                    result = GREATER;
                } else if (!LoggingProfileOperations.isLoggingProfileAddress(o1) && LoggingProfileOperations.isLoggingProfileAddress(o2)) {
                    result = LESS;
                } else if (LoggingProfileOperations.isLoggingProfileAddress(o1) && LoggingProfileOperations.isLoggingProfileAddress(o2)) {
                    if (CommonAttributes.LOGGING_PROFILE.equals(key1) && !CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                        result = LESS;
                    } else if (!CommonAttributes.LOGGING_PROFILE.equals(key1) && CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                        result = GREATER;
                    } else {
                        result = compare(key1, key2);
                    }
                } else {
                    result = compare(key1, key2);
                }
            }
            return result;
        }

        private int compare(final String key1, final String key2) {
            int result = EQUAL;
            if (ModelDescriptionConstants.SUBSYSTEM.equals(key1)) {
                result = LESS;
            } else if (ModelDescriptionConstants.SUBSYSTEM.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.LOGGING_PROFILE.equals(key1)) {
                result = LESS;
            } else if (CommonAttributes.LOGGING_PROFILE.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.ROOT_LOGGER.equals(key1)) {
                result = LESS;
            } else if (CommonAttributes.ROOT_LOGGER.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.LOGGER.equals(key1)) {
                result = LESS;
            } else if (CommonAttributes.LOGGER.equals(key2)) {
                result = GREATER;
            } else if (CommonAttributes.ASYNC_HANDLER.equals(key1) && !(CommonAttributes.LOGGER.equals(key2) || CommonAttributes.ROOT_LOGGER.equals(key2))) {
                result = LESS;
            } else if (CommonAttributes.ASYNC_HANDLER.equals(key2) && !(CommonAttributes.LOGGER.equals(key1) || CommonAttributes.ROOT_LOGGER.equals(key1))) {
                result = GREATER;
            }
            return result;
        }
    }
}
