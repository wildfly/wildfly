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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.MapValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Defining characteristics of an {@link ModelType#OBJECT} attribute in a {@link org.jboss.as.controller.registry.Resource},
 * where all children of the object have values of the same type; i.e. the attribute represents a logical map of
 * arbitrary key value pairs.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class MapAttributeDefinition extends AttributeDefinition {

    private final ParameterValidator elementValidator;

    public MapAttributeDefinition(final String name, final boolean allowNull, final ParameterValidator elementValidator) {
        this(name, name, allowNull, false, 0, Integer.MAX_VALUE, null, elementValidator,
                null, null, null, false, null, null, (Boolean) null);
    }

    @Deprecated
    public MapAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                  final int minSize, final int maxSize, final ParameterValidator elementValidator) {
        this(name, xmlName, allowNull, false, minSize, maxSize, null, elementValidator, null, null, null,
                false, null, null, (Boolean) null);
    }

    @Deprecated
    public MapAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                  final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                  final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, false, minSize, maxSize, null, elementValidator, alternatives, requires, null,
                false, null, null, null, flags);
    }

    @Deprecated
    protected MapAttributeDefinition(final String name, final String xmlName, final boolean allowNull, boolean allowExpression,
            final int minSize, final int maxSize, final ParameterCorrector corrector, final ParameterValidator elementValidator,
            final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly, final DeprecationData deprecated,
            final AccessConstraintDefinition[] accessConstraints, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, allowExpression, minSize, maxSize, corrector, elementValidator,
                alternatives, requires, attributeMarshaller, resourceOnly, deprecated, accessConstraints, null, flags);
    }

    protected MapAttributeDefinition(final String name, final String xmlName, final boolean allowNull, boolean allowExpression,
                                     final int minSize, final int maxSize, final ParameterCorrector corrector, final ParameterValidator elementValidator,
                                     final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,
                                     final boolean resourceOnly, final DeprecationData deprecated,
                                     final AccessConstraintDefinition[] accessConstraints,
                                     final Boolean nullSignificant, final AttributeAccess.Flag... flags) {
        super(name, xmlName, null, ModelType.OBJECT, allowNull, allowExpression, null, corrector, new MapValidator(elementValidator, allowNull, minSize, maxSize), false,
                alternatives, requires, attributeMarshaller, resourceOnly, deprecated, accessConstraints, nullSignificant, flags);
        this.elementValidator = elementValidator;
    }

    protected MapAttributeDefinition(final String name, final String xmlName, final ModelNode defaultValue,
            final boolean allowNull, boolean allowExpression, final int minSize, final int maxSize,
            final ParameterCorrector corrector, final ParameterValidator elementValidator, final String[] alternatives,
            final String[] requires, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly,
            final DeprecationData deprecated, final AccessConstraintDefinition[] accessConstraints,
            final Boolean nullSignificant, final AttributeAccess.Flag... flags) {
        super(name, xmlName, defaultValue, ModelType.OBJECT, allowNull, allowExpression, null, corrector, new MapValidator(
                elementValidator, allowNull, minSize, maxSize), false, alternatives, requires, attributeMarshaller,
                resourceOnly, deprecated, accessConstraints, nullSignificant, flags);
        this.elementValidator = elementValidator;
    }

    /**
     * Creates and returns a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link #getValidator() this object's validator}.
     * <p>
     * If {@code value} is {@code null} and a {@link #getDefaultValue() default value} is available, the value of that
     * default value will be used.
     * </p>
     *
     * @param value the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param location current location of the parser's {@link javax.xml.stream.XMLStreamReader}. Used for any exception
     *                 message
     *
     * @return {@code ModelNode} representing the parsed value
     *
     * @throws XMLStreamException if {@code value} is not valid
     */
    public ModelNode parse(final String value, final Location location) throws XMLStreamException {

        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null) {
            if (isAllowExpression()) {
                node = ParseUtils.parsePossibleExpression(trimmed);
            } else {
                node = new ModelNode().set(trimmed);
            }
            if (node.getType() != ModelType.EXPRESSION) {
                // Convert the string to the expected type
                switch (getType()) {
                    case BIG_DECIMAL:
                        node.set(node.asBigDecimal());
                        break;
                    case BIG_INTEGER:
                        node.set(node.asBigInteger());
                        break;
                    case BOOLEAN:
                        node.set(node.asBoolean());
                        break;
                    case BYTES:
                        node.set(node.asBytes());
                        break;
                    case DOUBLE:
                        node.set(node.asDouble());
                        break;
                    case INT:
                        node.set(node.asInt());
                        break;
                    case LONG:
                        node.set(node.asLong());
                        break;
                }
            }
        }
        else {
            node = new ModelNode();
        }


        try {
            elementValidator.validateParameter(getXmlName(), node);
        } catch (OperationFailedException e) {
            throw new XMLStreamException(e.getFailureDescription().toString(), location);
        }

        return node;
    }

    public void parseAndAddParameterElement(final String key, final String value, final ModelNode operation, final XMLExtendedStreamReader reader) throws XMLStreamException {
        ModelNode paramVal = parse(value, reader.getLocation());
        operation.get(getName()).get(key).set(paramVal);
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
        final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
        addValueTypeDescription(result, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
        final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);
        addValueTypeDescription(result, bundle);
        return result;
    }

    protected abstract void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle);

    @Override
    public ModelNode addResourceAttributeDescription(ModelNode resourceDescription, ResourceDescriptionResolver resolver,
                                                     Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
        addAttributeValueTypeDescription(result, resolver, locale, bundle);
        return result;
    }

    protected abstract void addAttributeValueTypeDescription(ModelNode result, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle);

    @Override
    public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName,
                                                      ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
        addOperationParameterValueTypeDescription(result, operationName, resolver, locale, bundle);
        return result;
    }

    protected abstract void addOperationParameterValueTypeDescription(ModelNode result, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle);

    @Override
    public void marshallAsElement(ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
        if (attributeMarshaller.isMarshallable(this,resourceModel,marshallDefault)){
            attributeMarshaller.marshallAsElement(this, resourceModel, marshallDefault, writer);
        }
    }

    /**
     * Iterates through the items in the {@code parameter} map, calling {@link #convertParameterElementExpressions(ModelNode)}
     * for each value.
     * <p>
     * <strong>Note</strong> that the default implementation of {@link #convertParameterElementExpressions(ModelNode)}
     * will only convert simple {@link ModelType#STRING} values. If users need to handle complex values
     * with embedded expressions, they should use a subclass that overrides that method.
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    protected ModelNode convertParameterExpressions(ModelNode parameter) {
        ModelNode result = parameter;
        if (parameter.isDefined()) {
            boolean changeMade = false;
            ModelNode newMap = new ModelNode().setEmptyObject();
            for (Property prop : parameter.asPropertyList()) {
                ModelNode converted = convertParameterElementExpressions(prop.getValue());
                newMap.get(prop.getName()).set(converted);
                changeMade |= !converted.equals(prop.getValue());
            }
            if (changeMade) {
                result = newMap;
            }
        }
        return result;
    }

    /**
     * Examine the given value item of a parameter map for any expression syntax, converting the relevant node to
     * {@link ModelType#EXPRESSION} if such is supported.
     *
     * @param parameterElementValue the node to examine. Will not be {@code null}
     * @return the parameter element value with expressions converted, or the original parameter if no conversion
     *         was performed. Cannot return {@code null}
     */
    protected ModelNode convertParameterElementExpressions(ModelNode parameterElementValue) {
        return isAllowExpression() ? convertStringExpression(parameterElementValue) : parameterElementValue;
    }

    protected static ModelNode convertToModel(Map<String, ? extends Object> convertMe) {
        if (convertMe == null || convertMe.size() == 0) {
            return null;
        }
        final ModelNode model = new ModelNode();

        for (String key : convertMe.keySet()) {
            final Object value = convertMe.get(key);
            if (value instanceof Long) {
                model.add(key, (Long) value);
            } else if (value instanceof Double) {
                model.add(key, (Double) value);
            } else if (value instanceof String) {
                model.add(key, (String) value);
            } else if (value instanceof Integer) {
                model.add(key, (Integer) value);
            } else if (value instanceof Boolean) {
                model.add(key, (Boolean) value);
            } else if (value instanceof ModelNode) {
                model.add(key, (ModelNode) value);
            } else if (value instanceof BigInteger) {
                model.add(key, (BigInteger) value);
            } else if (value instanceof BigDecimal) {
                model.add(key, (BigDecimal) value);
            } else if (value instanceof byte[]) {
                model.add(key, (byte[]) value);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return model;
    }

    public static final ParameterCorrector LIST_TO_MAP_CORRECTOR = new ParameterCorrector() {
        public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
            if (newValue.isDefined()) {
                if (newValue.getType() == ModelType.LIST) {
                    int listSize = newValue.asList().size();
                    List<Property> propertyList = newValue.asPropertyList();
                    if (propertyList.size() == 0) {
                        //The list cannot be converted to a map
                        if (listSize == 0) {
                            return new ModelNode();
                        }
                        if (listSize > 0) {
                            //It is a list of simple values, so just return the original
                            return newValue;
                        }
                    }
                    ModelNode corrected = new ModelNode();
                    for (Property p : newValue.asPropertyList()) {
                        corrected.get(p.getName()).set(p.getValue());
                    }
                    return corrected;
                }
            }
            return newValue;
        }
    };
}
