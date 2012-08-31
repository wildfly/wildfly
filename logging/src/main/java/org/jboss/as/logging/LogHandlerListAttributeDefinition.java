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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.logging.resolvers.HandlerResolver;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.config.PropertyConfigurable;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
//todo replace with   SimpleListAttributeDefinition
public class LogHandlerListAttributeDefinition extends ListAttributeDefinition implements ConfigurationProperty<Set<String>> {
    private final SimpleAttributeDefinition valueType;
    private final String propertyName;
    private final HandlerResolver resolver = HandlerResolver.INSTANCE;

    LogHandlerListAttributeDefinition(final String name, final String xmlName, final String propertyName, final SimpleAttributeDefinition valueType, final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, minSize, maxSize, valueType.getValidator(), alternatives, requires, flags);
        this.propertyName = propertyName;
        this.valueType = valueType;
    }

    @Override
    public ModelNodeResolver<Set<String>> resolver() {
        return resolver;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Set<String> resolvePropertyValue(final OperationContext context, final ModelNode model) throws OperationFailedException {
        Set<String> result = Collections.emptySet();
        final ModelNode value = resolveModelAttribute(context, model);
        if (value.isDefined()) {
            result = resolver.resolveValue(context, value);
        }
        return result;
    }

    @Override
    public void setPropertyValue(final OperationContext context, final ModelNode model, final PropertyConfigurable configuration) throws OperationFailedException {
        throw LoggingMessages.MESSAGES.unsupportedMethod("setPropertyValue", getClass().getName());
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
        node.get(VALUE_TYPE, valueType.getName()).set(getValueTypeDescription(false));
    }


    protected void addValueTypeDescription(final ModelNode node, final String prefix, final ResourceBundle bundle) {
        final ModelNode valueTypeDesc = getValueTypeDescription(false);
        valueTypeDesc.get(DESCRIPTION).set(valueType.getAttributeTextDescription(bundle, prefix));
        node.get(VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    @Override
    protected void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        final ModelNode valueTypeDesc = getValueTypeDescription(false);
        valueTypeDesc.get(DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, valueType.getName()));
        node.get(VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(final ModelNode node, final String operationName, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        final ModelNode valueTypeDesc = getValueTypeDescription(true);
        valueTypeDesc.get(DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, valueType.getName()));
        node.get(VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
    }

    @Override
    public void marshallAsElement(final ModelNode resourceModel, final boolean marshalDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            writer.writeStartElement(getXmlName());
            for (ModelNode handler : resourceModel.get(getName()).asList()) {
                valueType.marshallAsElement(handler, writer);
            }
            writer.writeEndElement();
        }
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
                        result.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MAX).set(max);
                }
            }
        }
        if (validator instanceof AllowedValuesValidator) {
            AllowedValuesValidator avv = (AllowedValuesValidator) validator;
            List<ModelNode> allowed = avv.getAllowedValues();
            if (allowed != null) {
                for (ModelNode ok : allowed) {
                    result.get(ModelDescriptionConstants.ALLOWED).add(ok);
                }
            }
        }
        return result;
    }
}
