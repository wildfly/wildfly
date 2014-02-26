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

import static org.jboss.as.logging.Logging.createOperationFailure;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.logging.LoggingOperations.LoggingWriteAttributeHandler;
import org.jboss.as.logging.validators.RegexValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;
import org.jboss.logmanager.formatters.PatternFormatter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PatternFormatterResourceDefinition extends TransformerResourceDefinition {

    static final String COLOR_MAP_VALIDATION_PATTERN = "^((severe|fatal|error|warn|warning|info|debug|trace|config|fine|finer|finest|):(black|green|red|yellow|blue|magenta|cyan|white|brightblack|brightred|brightgreen|brightblue|brightyellow|brightmagenta|brightcyan|brightwhite|)(,(?!$)|$))*$";

    // Pattern formatter options
    public static final PropertyAttributeDefinition COLOR_MAP = PropertyAttributeDefinition.Builder.of("color-map", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setPropertyName("colors")
            .setValidator(new RegexValidator(ModelType.STRING, true, true, COLOR_MAP_VALIDATION_PATTERN))
            .build();

    public static final PropertyAttributeDefinition PATTERN = PropertyAttributeDefinition.Builder.of("pattern", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(false)
            .setDefaultValue(new ModelNode("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n"))
            .build();

    public static final ObjectTypeAttributeDefinition PATTERN_FORMATTER = ObjectTypeAttributeDefinition.Builder.of("pattern-formatter", PATTERN, COLOR_MAP)
            .setAllowExpression(false)
            .setAllowNull(true)
            .setAttributeMarshaller(new DefaultAttributeMarshaller() {
                @Override
                public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
                    if (isMarshallable(attribute, resourceModel, marshallDefault)) {
                        writer.writeStartElement(attribute.getXmlName());
                        final String pattern = resourceModel.get(PATTERN.getName()).asString();
                        writer.writeAttribute(PATTERN.getXmlName(), pattern);
                        if (resourceModel.hasDefined(COLOR_MAP.getName())) {
                            final String colorMap = resourceModel.get(COLOR_MAP.getName()).asString();
                            writer.writeAttribute(COLOR_MAP.getXmlName(), colorMap);
                        }
                        writer.writeEndElement();
                    }
                }

                @Override
                public boolean isMarshallable(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault) {
                    return resourceModel.hasDefined(PATTERN.getName());
                }
            })
            .build();

    static final PathElement PATTERN_FORMATTER_PATH = PathElement.pathElement(PATTERN_FORMATTER.getName());

    static final PropertyAttributeDefinition[] ATTRIBUTES = {
            COLOR_MAP,
            PATTERN,
    };


    /**
     * A step handler to add a pattern formatter
     */
    static final OperationStepHandler ADD = new LoggingOperations.LoggingAddOperationStepHandler() {

        @Override
        public void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attribute : ATTRIBUTES) {
                attribute.validateAndSet(operation, model);
            }
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            FormatterConfiguration configuration = logContextConfiguration.getFormatterConfiguration(name);
            if (configuration == null) {
                LoggingLogger.ROOT_LOGGER.tracef("Adding formatter '%s' at '%s'", name, LoggingOperations.getAddress(operation));
                configuration = logContextConfiguration.addFormatterConfiguration(null, PatternFormatter.class.getName(), name);
            }

            for (PropertyAttributeDefinition attribute : ATTRIBUTES) {
                attribute.setPropertyValue(context, model, configuration);
            }
        }
    };

    static final OperationStepHandler WRITE = new LoggingWriteAttributeHandler(ATTRIBUTES) {

        @Override
        protected boolean applyUpdate(final OperationContext context, final String attributeName, final String addressName, final ModelNode value, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final FormatterConfiguration configuration = logContextConfiguration.getFormatterConfiguration(addressName);
            for (PropertyAttributeDefinition attribute : ATTRIBUTES) {
                if (attribute.getName().equals(attributeName)) {
                    configuration.setPropertyValueString(attribute.getPropertyName(), value.asString());
                    break;
                }
            }
            return false;
        }
    };

    /**
     * A step handler to remove
     */
    static final OperationStepHandler REMOVE = new LoggingOperations.LoggingRemoveOperationStepHandler() {

        @Override
        protected void performRemove(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);
        }

        @Override
        public void performRuntime(final OperationContext context, final ModelNode operation, final LogContextConfiguration logContextConfiguration, final String name, final ModelNode model) throws OperationFailedException {
            final FormatterConfiguration configuration = logContextConfiguration.getFormatterConfiguration(name);
            if (configuration == null) {
                throw createOperationFailure(LoggingMessages.MESSAGES.formatterNotFound(name));
            }
            logContextConfiguration.removeFormatterConfiguration(name);
        }
    };

    static final PatternFormatterResourceDefinition INSTANCE = new PatternFormatterResourceDefinition();

    public PatternFormatterResourceDefinition() {
        super(PATTERN_FORMATTER_PATH,
                LoggingExtension.getResourceDescriptionResolver(PATTERN_FORMATTER.getName()),
                ADD, REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, WRITE);
        }
    }

    @Override
    public void registerTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder resourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        switch (modelVersion) {
            case VERSION_1_1_0:
            case VERSION_1_2_0:
            case VERSION_1_3_0:
                resourceBuilder.rejectChildResource(PATTERN_FORMATTER_PATH);
                loggingProfileBuilder.rejectChildResource(PATTERN_FORMATTER_PATH);
        }
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
        subsystemBuilder.rejectChildResource(PATTERN_FORMATTER_PATH);
        loggingProfileBuilder.rejectChildResource(PATTERN_FORMATTER_PATH);
        return subsystemBuilder;
    }
}