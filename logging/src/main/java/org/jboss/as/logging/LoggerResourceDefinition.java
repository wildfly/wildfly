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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.logging.CommonAttributes.ADD_HANDLER_OPERATION_NAME;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.REMOVE_HANDLER_OPERATION_NAME;
import static org.jboss.as.logging.Logging.join;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.logging.LoggingOperations.ReadFilterOperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggerResourceDefinition extends SimpleResourceDefinition {

    public static final String CHANGE_LEVEL_OPERATION_NAME = "change-log-level";
    public static final String LEGACY_ADD_HANDLER_OPERATION_NAME = "assign-handler";
    public static final String LEGACY_REMOVE_HANDLER_OPERATION_NAME = "unassign-handler";
    public static final String LOGGER = "logger";
    static final PathElement LOGGER_PATH = PathElement.pathElement(LOGGER);

    static final ResourceDescriptionResolver LOGGER_RESOLVER = LoggingExtension.getResourceDescriptionResolver(LOGGER);

    static final OperationDefinition CHANGE_LEVEL_OPERATION = new SimpleOperationDefinitionBuilder(CHANGE_LEVEL_OPERATION_NAME, LOGGER_RESOLVER)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .setParameters(CommonAttributes.LEVEL)
            .build();

    static final OperationDefinition LEGACY_ADD_HANDLER_OPERATION = new SimpleOperationDefinitionBuilder(LEGACY_ADD_HANDLER_OPERATION_NAME, LOGGER_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .build();

    static final OperationDefinition LEGACY_REMOVE_HANDLER_OPERATION = new SimpleOperationDefinitionBuilder(LEGACY_REMOVE_HANDLER_OPERATION_NAME, LOGGER_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .build();

    static final OperationDefinition ADD_HANDLER_OPERATION = new SimpleOperationDefinitionBuilder(ADD_HANDLER_OPERATION_NAME, LOGGER_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    static final OperationDefinition REMOVE_HANDLER_OPERATION = new SimpleOperationDefinitionBuilder(REMOVE_HANDLER_OPERATION_NAME, LOGGER_RESOLVER)
            .setParameters(CommonAttributes.HANDLER_NAME)
            .build();

    public static final PropertyAttributeDefinition USE_PARENT_HANDLERS = PropertyAttributeDefinition.Builder.of("use-parent-handlers", ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(true))
            .setPropertyName("useParentHandlers")
            .build();

    public static final SimpleAttributeDefinition CATEGORY = SimpleAttributeDefinitionBuilder.create("category", ModelType.STRING, true).build();
    static final AttributeDefinition[] ATTRIBUTES = {
            CATEGORY,
            FILTER_SPEC,
            LEVEL,
            HANDLERS,
            USE_PARENT_HANDLERS
    };

    static final AttributeDefinition[] WRITABLE_ATTRIBUTES = {
            FILTER_SPEC,
            LEVEL,
            HANDLERS,
            USE_PARENT_HANDLERS
    };

    static final AttributeDefinition[] LEGACY_ATTRIBUTES = {
            FILTER,
    };

    static final AttributeDefinition[] EXPRESSION_ATTRIBUTES = {
            FILTER,
            FILTER_SPEC,
            LEVEL,
            HANDLERS,
            USE_PARENT_HANDLERS
    };

    private final AttributeDefinition[] writableAttributes;
    private final OperationStepHandler writeHandler;

    public LoggerResourceDefinition(final boolean includeLegacy) {
        super(LOGGER_PATH,
                LoggingExtension.getResourceDescriptionResolver(LOGGER),
                (includeLegacy ? new LoggerOperations.LoggerAddOperationStepHandler(join(ATTRIBUTES, LEGACY_ATTRIBUTES)) : new LoggerOperations.LoggerAddOperationStepHandler(ATTRIBUTES)),
                LoggerOperations.REMOVE_LOGGER);
        writableAttributes = (includeLegacy ? join(WRITABLE_ATTRIBUTES, LEGACY_ATTRIBUTES) : WRITABLE_ATTRIBUTES);
        this.writeHandler = new LoggerOperations.LoggerWriteAttributeHandler(writableAttributes);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : writableAttributes) {
            // Filter requires a special reader
            if (def.getName().equals(FILTER.getName())) {
                resourceRegistration.registerReadWriteAttribute(def, ReadFilterOperationStepHandler.INSTANCE, writeHandler);
            } else {
                resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
            }
        }
        resourceRegistration.registerReadOnlyAttribute(CATEGORY, null);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        registration.registerOperationHandler(CHANGE_LEVEL_OPERATION, LoggerOperations.CHANGE_LEVEL);
        registration.registerOperationHandler(ADD_HANDLER_OPERATION, LoggerOperations.ADD_HANDLER);
        registration.registerOperationHandler(REMOVE_HANDLER_OPERATION, LoggerOperations.REMOVE_HANDLER);
        registration.registerOperationHandler(LEGACY_ADD_HANDLER_OPERATION, LoggerOperations.ADD_HANDLER);
        registration.registerOperationHandler(LEGACY_REMOVE_HANDLER_OPERATION, LoggerOperations.REMOVE_HANDLER);
    }

    /**
     * Add the transformers for the logger.
     *
     * @param subsystemBuilder      the default subsystem builder
     * @param loggingProfileBuilder the logging profile builder
     *
     * @return the builder created for the resource
     */
    static ResourceTransformationDescriptionBuilder addTransformers(final ResourceTransformationDescriptionBuilder subsystemBuilder,
                                                                    final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        // Register the logger resource
        final ResourceTransformationDescriptionBuilder child = subsystemBuilder.addChildResource(LOGGER_PATH)
                // Register operation transformers
                .addOperationTransformationOverride(ADD)
                .setCustomOperationTransformer(LoggingOperationTransformer.INSTANCE)
                .inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(WRITE_ATTRIBUTE_OPERATION)
                .setCustomOperationTransformer(LoggingOperationTransformer.INSTANCE)
                .inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(ADD_HANDLER_OPERATION_NAME)
                .setCustomOperationTransformer(LoggingOperationTransformer.INSTANCE)
                .inheritResourceAttributeDefinitions().end()
                .addOperationTransformationOverride(REMOVE_HANDLER_OPERATION_NAME)
                .setCustomOperationTransformer(LoggingOperationTransformer.INSTANCE)
                .inheritResourceAttributeDefinitions().end()
                        // Add attributes that should reject expressions
                .getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, EXPRESSION_ATTRIBUTES).end()
                        // Set the custom resource transformer
                .setCustomResourceTransformer(new LoggingResourceTransformer(CATEGORY, FILTER_SPEC));

        // Reject logging profile resources
        loggingProfileBuilder.rejectChildResource(LOGGER_PATH);

        return child;
    }
}
