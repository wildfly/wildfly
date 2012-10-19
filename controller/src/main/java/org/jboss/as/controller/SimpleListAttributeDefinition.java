/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class SimpleListAttributeDefinition extends ListAttributeDefinition {
    private final AttributeDefinition valueType;

    protected SimpleListAttributeDefinition(final String name, final String xmlName, final AttributeDefinition valueType, final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, AttributeMarshaller attributeMarshaller,final boolean resourceOnly, final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, minSize, maxSize, valueType.getValidator(), alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        this.valueType = valueType;
    }

    public AttributeDefinition getValueType() {
        return valueType;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
        final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
        addValueTypeDescription(result, prefix, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
        final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);
        addValueTypeDescription(result, prefix, bundle);
        return result;
    }


    @Override
    protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(getValueTypeDescription(false));
    }


    protected void addValueTypeDescription(final ModelNode node, final String prefix, final ResourceBundle bundle) {
        final ModelNode valueTypeDesc = getValueTypeDescription(false);
        valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).set(valueType.getAttributeTextDescription(bundle, prefix));
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    @Override
    protected void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        final ModelNode valueTypeDesc = getValueTypeDescription(false);
        valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, valueType.getName()));
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(final ModelNode node, final String operationName, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        final ModelNode valueTypeDesc = getValueTypeDescription(true);
        valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, valueType.getName()));
        node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    private ModelNode getValueTypeDescription(boolean forOperation) {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.TYPE).set(valueType.getType());
        result.get(ModelDescriptionConstants.DESCRIPTION); // placeholder
        result.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(valueType.isAllowExpression());
        if (forOperation) {
            result.get(ModelDescriptionConstants.REQUIRED).set(!valueType.isAllowNull());
        }
        result.get(ModelDescriptionConstants.NILLABLE).set(isAllowNull());
        final ModelNode defaultValue = valueType.getDefaultValue();
        if (!forOperation && defaultValue != null && defaultValue.isDefined()) {
            result.get(ModelDescriptionConstants.DEFAULT).set(defaultValue);
        }
        MeasurementUnit measurementUnit = valueType.getMeasurementUnit();
        if (measurementUnit != null && measurementUnit != MeasurementUnit.NONE) {
            result.get(ModelDescriptionConstants.UNIT).set(measurementUnit.getName());
        }
        final String[] alternatives = valueType.getAlternatives();
        if (alternatives != null) {
            for (final String alternative : alternatives) {
                result.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
            }
        }
        final String[] requires = valueType.getRequires();
        if (requires != null) {
            for (final String required : requires) {
                result.get(ModelDescriptionConstants.REQUIRES).add(required);
            }
        }
        final ParameterValidator validator = valueType.getValidator();
        if (validator instanceof MinMaxValidator) {
            MinMaxValidator minMax = (MinMaxValidator) validator;
            Long min = minMax.getMin();
            if (min != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                    case BYTES:
                        result.get(ModelDescriptionConstants.MIN_LENGTH).set(min);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MIN).set(min);
                }
            }
            Long max = minMax.getMax();
            if (max != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                    case BYTES:
                        result.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MAX).set(max);
                }
            }
        }
        addAllowedValuesToDescription(result, validator);
        return result;
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder,SimpleListAttributeDefinition>{
        private final AttributeDefinition valueType;

        public Builder(final String name, final AttributeDefinition valueType) {
            super(name, ModelType.LIST);
            this.valueType = valueType;
        }

        public Builder(final SimpleListAttributeDefinition basis) {
            super(basis);
            valueType = basis.getValueType();
        }

        public static Builder of(final String name, final AttributeDefinition valueType) {
            return new Builder(name, valueType);
        }

        /**
         * Reintroduced since some legacy subsystems require this method, and they now get booted up
         * for transformers subsystem testing.
         */
        @Deprecated
        public static Builder of(final String name, final SimpleAttributeDefinition valueType) {
            return new Builder(name, valueType);
        }

        public SimpleListAttributeDefinition build() {
            if (xmlName == null) xmlName = name;
            if (maxSize < 1) maxSize = Integer.MAX_VALUE;
            if (attributeMarshaller == null) {
                attributeMarshaller = new AttributeMarshaller() {
                    @Override
                    public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                        if (resourceModel.hasDefined(attribute.getName())) {
                            writer.writeStartElement(attribute.getXmlName());
                            for (ModelNode handler : resourceModel.get(attribute.getName()).asList()) {
                                valueType.marshallAsElement(handler, writer);
                            }
                            writer.writeEndElement();
                        }
                    }
                };
            }
            return new SimpleListAttributeDefinition(name, xmlName, valueType, allowNull, minSize, maxSize, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        }

        /*
        --------------------------
        added for binary compatibility with older versions
         */
        @Override
        public Builder setAllowNull(boolean allowNull) {
            return super.setAllowNull(allowNull);
        }

        @Override
        public Builder setMaxSize(final int maxSize) {
            return super.setMaxSize(maxSize);
        }

        @Override
        public Builder setMinSize(final int minSize) {
            return super.setMinSize(minSize);
        }
    }
}
