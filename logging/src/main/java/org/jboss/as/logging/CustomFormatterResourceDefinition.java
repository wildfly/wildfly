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

import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.Logging.createOperationFailure;

import java.util.List;
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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logmanager.config.FormatterConfiguration;
import org.jboss.logmanager.config.LogContextConfiguration;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class CustomFormatterResourceDefinition extends TransformerResourceDefinition {

    public static final ObjectTypeAttributeDefinition CUSTOM_FORMATTER = ObjectTypeAttributeDefinition.Builder.of("custom-formatter", CLASS, MODULE, PROPERTIES)
            .setAllowExpression(false)
            .setAllowNull(true)
            .setAttributeMarshaller(new DefaultAttributeMarshaller() {
                @Override
                public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
                    if (isMarshallable(attribute, resourceModel, marshallDefault)) {
                        writer.writeStartElement(attribute.getXmlName());
                        MODULE.marshallAsAttribute(resourceModel, writer);
                        CLASS.marshallAsAttribute(resourceModel, writer);
                        if (resourceModel.hasDefined(PROPERTIES.getName())) {
                            PROPERTIES.marshallAsElement(resourceModel, writer);
                        }
                        writer.writeEndElement();
                    }
                }

                @Override
                public boolean isMarshallable(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault) {
                    return resourceModel.hasDefined(CLASS.getName());
                }
            })
            .build();

    static final PathElement CUSTOM_FORMATTER_PATH = PathElement.pathElement(CUSTOM_FORMATTER.getName());

    static final AttributeDefinition[] ATTRIBUTES = {
            CLASS,
            MODULE,
            PROPERTIES
    };


    /**
     * A step handler to add a custom formatter
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
                final String className = CLASS.resolveModelAttribute(context, model).asString();
                final ModelNode moduleNameNode = MODULE.resolveModelAttribute(context, model);
                final String moduleName = moduleNameNode.isDefined() ? moduleNameNode.asString() : null;
                configuration = logContextConfiguration.addFormatterConfiguration(moduleName, className, name);
                final ModelNode properties = PROPERTIES.resolveModelAttribute(context, operation);
                if (properties.isDefined()) {
                    for (Property property : properties.asPropertyList()) {
                        configuration.setPropertyValueString(property.getName(), property.getValue().asString());
                    }
                }
            }
        }
    };

    static final OperationStepHandler WRITE = new LoggingWriteAttributeHandler(ATTRIBUTES) {

        @Override
        protected boolean applyUpdate(final OperationContext context, final String attributeName, final String addressName, final ModelNode value, final LogContextConfiguration logContextConfiguration) throws OperationFailedException {
            final FormatterConfiguration configuration = logContextConfiguration.getFormatterConfiguration(addressName);
            if (PROPERTIES.getName().equals(attributeName)) {
                if (value.isDefined()) {
                    for (Property property : value.asPropertyList()) {
                        configuration.setPropertyValueString(property.getName(), property.getValue().asString());
                    }
                } else {
                    // Remove all current properties
                    final List<String> names = configuration.getPropertyNames();
                    for (String name : names) {
                        configuration.removeProperty(name);
                    }
                }
            }

            // Writing a class attribute or module will require the previous formatter to be removed and a new formatter
            // added. It's best to require a restart.
            return CLASS.getName().equals(attributeName) || MODULE.getName().equals(attributeName);
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

    static final CustomFormatterResourceDefinition INSTANCE = new CustomFormatterResourceDefinition();

    public CustomFormatterResourceDefinition() {
        super(CUSTOM_FORMATTER_PATH,
                LoggingExtension.getResourceDescriptionResolver(CUSTOM_FORMATTER.getName()),
                ADD, REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, WRITE);
        }
    }

    @Override
    public void registerTransformers(final KnownModelVersion modelVersion, final ResourceTransformationDescriptionBuilder rootResourceBuilder, final ResourceTransformationDescriptionBuilder loggingProfileBuilder) {
        switch (modelVersion) {
            case VERSION_1_1_0:
            case VERSION_1_2_0:
            case VERSION_1_3_0:
                rootResourceBuilder.rejectChildResource(CUSTOM_FORMATTER_PATH);
                if (loggingProfileBuilder != null) {
                    loggingProfileBuilder.rejectChildResource(CUSTOM_FORMATTER_PATH);
                }
        }
    }
}
