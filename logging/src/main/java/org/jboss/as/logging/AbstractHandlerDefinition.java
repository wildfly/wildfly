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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.logging.CommonAttributes.ENABLED;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.HandlerOperations.HandlerUpdateOperationStepHandler;
import static org.jboss.as.logging.HandlerOperations.LogHandlerWriteAttributeHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Handler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.RejectExpressionValuesChainedTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.as.logging.HandlerOperations.HandlerAddOperationStepHandler;
import org.jboss.as.logging.LoggingOperations.ReadFilterOperationStepHandler;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractHandlerDefinition extends SimpleResourceDefinition {

    public static final String UPDATE_OPERATION_NAME = "update-properties";
    public static final String CHANGE_LEVEL_OPERATION_NAME = "change-log-level";

    static final AttributeDefinition[] DEFAULT_ATTRIBUTES = {
            LEVEL,
            ENABLED,
            ENCODING,
            FORMATTER,
            FILTER_SPEC,
    };

    static final AttributeDefinition[] LEGACY_ATTRIBUTES = {
            FILTER,
    };

    static final ResourceDescriptionResolver HANDLER_RESOLVER = LoggingExtension.getResourceDescriptionResolver(CommonAttributes.HANDLER.getName());

    static final SimpleOperationDefinition ENABLE_HANDLER = new SimpleOperationDefinitionBuilder(ENABLE, HANDLER_RESOLVER)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .build();

    static final SimpleOperationDefinition DISABLE_HANDLER = new SimpleOperationDefinitionBuilder(DISABLE, HANDLER_RESOLVER)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .build();

    static final SimpleOperationDefinition CHANGE_LEVEL = new SimpleOperationDefinitionBuilder(CHANGE_LEVEL_OPERATION_NAME, HANDLER_RESOLVER)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .setParameters(CommonAttributes.LEVEL)
            .build();

    private final LogHandlerWriteAttributeHandler writeHandler;
    private final AttributeDefinition[] writableAttributes;
    private final AttributeDefinition[] readOnlyAttributes;

    protected AbstractHandlerDefinition(final PathElement path,
                                        final Class<? extends Handler> type,
                                        final AttributeDefinition[] attributes) {
        this(path, type, attributes, null, attributes);
    }

    protected AbstractHandlerDefinition(final PathElement path,
                                        final Class<? extends Handler> type,
                                        final AttributeDefinition[] attributes,
                                        final ConfigurationProperty<?>... constructionProperties) {
        this(path, type, attributes, null, attributes, constructionProperties);
    }

    protected AbstractHandlerDefinition(final PathElement path,
                                        final Class<? extends Handler> type,
                                        final AttributeDefinition[] addAttributes,
                                        final AttributeDefinition[] readOnlyAttributes,
                                        final AttributeDefinition[] writableAttributes,
                                        final ConfigurationProperty<?>... constructionProperties) {
        super(path,
                HANDLER_RESOLVER,
                new HandlerAddOperationStepHandler(type, addAttributes, constructionProperties),
                HandlerOperations.REMOVE_HANDLER);
        this.writableAttributes = writableAttributes;
        writeHandler = new LogHandlerWriteAttributeHandler(this.writableAttributes);
        this.readOnlyAttributes = readOnlyAttributes;
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : writableAttributes) {
            // Filter requires a special reader
            if (def.getName().equals(FILTER.getName())) {
                resourceRegistration.registerReadWriteAttribute(def, ReadFilterOperationStepHandler.INSTANCE, writeHandler);
            } else {
                resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
            }
        }
        if (readOnlyAttributes != null) {
            for (AttributeDefinition def : readOnlyAttributes) {
                resourceRegistration.registerReadOnlyAttribute(def, null);
            }
        }
        resourceRegistration.registerReadOnlyAttribute(NAME, ReadResourceNameOperationStepHandler.INSTANCE);
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration registration) {
        super.registerOperations(registration);

        registration.registerOperationHandler(ENABLE_HANDLER, HandlerOperations.ENABLE_HANDLER);
        registration.registerOperationHandler(DISABLE_HANDLER, HandlerOperations.DISABLE_HANDLER);
        registration.registerOperationHandler(CHANGE_LEVEL, HandlerOperations.CHANGE_LEVEL);
        final SimpleOperationDefinition updateProperties = new SimpleOperationDefinitionBuilder(UPDATE_OPERATION_NAME, HANDLER_RESOLVER)
                .setDeprecated(ModelVersion.create(1, 2, 0))
                .setParameters(writableAttributes)
                .build();
        registration.registerOperationHandler(updateProperties, new HandlerUpdateOperationStepHandler(writableAttributes));
    }

    /**
     * Register the transformers for the handler.
     * <p/>
     * By default the {@link #DEFAULT_ATTRIBUTES default attributes} and {@link #LEGACY_ATTRIBUTES legacy attributes}
     * are added to the reject transformer.
     *
     * @param registration         the root resource
     * @param loggingProfileReg    the logging profile resource which will be discarded
     * @param handlerPath          the path to the handler resource
     * @param additionalAttributes the additional attributes to reject
     */
    static void registerTransformers(final TransformersSubRegistration registration, final TransformersSubRegistration loggingProfileReg,
                                     final PathElement handlerPath, final AttributeDefinition... additionalAttributes) {
        registerTransformers(registration, loggingProfileReg, handlerPath, additionalAttributes, new String[] {});
    }

    /**
     * Register the transformers for the handler.
     * <p/>
     * By default the {@link #DEFAULT_ATTRIBUTES default attributes} and {@link #LEGACY_ATTRIBUTES legacy attributes}
     * are added to the reject transformer.
     *
     * @param registration         the root resource
     * @param loggingProfileReg    the logging profile resource which will be discarded
     * @param handlerPath          the path to the handler resource
     * @param additionalAttributes the additional attributes to reject
     * @param additionalOperations additional operation names to register for the resource
     */
    static void registerTransformers(final TransformersSubRegistration registration, final TransformersSubRegistration loggingProfileReg,
                                     final PathElement handlerPath, final AttributeDefinition[] additionalAttributes, final String... additionalOperations) {
        final Set<String> names = new HashSet<String>();
        for (AttributeDefinition attribute : DEFAULT_ATTRIBUTES) {
            names.add(attribute.getName());
        }
        for (AttributeDefinition attribute :    LEGACY_ATTRIBUTES) {
            names.add(attribute.getName());
        }
        for (AttributeDefinition attribute : additionalAttributes) {
            names.add(attribute.getName());
        }

        // Default resource transformers
        final RejectExpressionValuesChainedTransformer rejectTransformer = new RejectExpressionValuesChainedTransformer(names);
        final LoggingResourceTransformer loggingTransformer = new LoggingResourceTransformer(CommonAttributes.NAME, CommonAttributes.FILTER_SPEC, CommonAttributes.ENABLED);

        // Chained transformers
        final ChainedResourceTransformer chainedResourceTransformer = new ChainedResourceTransformer(rejectTransformer, loggingTransformer);
        final ChainedOperationTransformer chainedOperationTransformer = new ChainedOperationTransformer(rejectTransformer, LoggingOperationTransformer.INSTANCE);

        final TransformersSubRegistration reg = registration.registerSubResource(handlerPath, chainedResourceTransformer);
        reg.registerOperationTransformer(ADD, chainedOperationTransformer);
        reg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION,
                new ChainedOperationTransformer(rejectTransformer.getWriteAttributeTransformer(), LoggingOperationTransformer.INSTANCE));
        reg.registerOperationTransformer(UPDATE_OPERATION_NAME, chainedOperationTransformer);
        for (String operationName : additionalOperations) {
            reg.registerOperationTransformer(operationName, chainedOperationTransformer);
        }

        // Ignore logging profiles
        loggingProfileReg.registerSubResource(handlerPath, true);
    }
}
