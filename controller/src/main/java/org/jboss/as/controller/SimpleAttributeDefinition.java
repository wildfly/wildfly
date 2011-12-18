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

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Defining characteristics of an attribute in a {@link org.jboss.as.controller.registry.Resource}, with utility
 * methods for conversion to and from xml and for validation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SimpleAttributeDefinition extends AttributeDefinition {

    public SimpleAttributeDefinition(final String name, final ModelType type, final boolean allowNull) {
        this(name, name, null, type, allowNull, false, null);
    }

    public SimpleAttributeDefinition(final String name, final ModelType type, final boolean allowNull, final AttributeAccess.Flag... flags) {
        this(name, name, null, type, allowNull, false, null, flags);
    }

    public SimpleAttributeDefinition(final String name, final ModelType type, final boolean allowNull, final MeasurementUnit measurementUnit) {
        this(name, name, null, type, allowNull, false, measurementUnit);
    }

    public SimpleAttributeDefinition(final String name, final ModelType type, final boolean allowNull,
                                     final MeasurementUnit measurementUnit, final AttributeAccess.Flag... flags) {
        this(name, name, null, type, allowNull, false, measurementUnit, flags);
    }

    public SimpleAttributeDefinition(final String name, final ModelNode defaultValue, final ModelType type, final boolean allowNull) {
        this(name, name, defaultValue, type, allowNull, false, null);
    }

    public SimpleAttributeDefinition(final String name, final ModelNode defaultValue, final ModelType type,
                                     final boolean allowNull, final AttributeAccess.Flag... flags) {
        this(name, name, defaultValue, type, allowNull, false, null, flags);
    }

    public SimpleAttributeDefinition(final String name, final ModelNode defaultValue, final ModelType type, final boolean allowNull, final MeasurementUnit measurementUnit) {
        this(name, name, defaultValue, type, allowNull, false, measurementUnit);
    }

    public SimpleAttributeDefinition(final String name, final ModelNode defaultValue, final ModelType type, final boolean allowNull,
                                     final MeasurementUnit measurementUnit, final AttributeAccess.Flag... flags) {
        this(name, name, defaultValue, type, allowNull, false, measurementUnit, flags);
    }

    public SimpleAttributeDefinition(final String name, final String xmlName, final ModelNode defaultValue, final ModelType type,
                                     final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit) {
        this(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit, (ParameterValidator) null, null, null);
    }

    public SimpleAttributeDefinition(final String name, final String xmlName, final ModelNode defaultValue, final ModelType type,
                                     final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
                                     final AttributeAccess.Flag... flags) {
        this(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                null, null, null, flags);
    }

    public SimpleAttributeDefinition(final String name, final String xmlName, final ModelNode defaultValue, final ModelType type,
                                     final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
                                     final ParameterValidator validator) {
        this(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit, validator, null, null);
    }

    public SimpleAttributeDefinition(String name, String xmlName, final ModelNode defaultValue, final ModelType type,
                                     final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
                                     final ParameterValidator validator, String[] alternatives, String[] requires, AttributeAccess.Flag... flags) {
        super(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                createParameterValidator(validator, type, allowNull, allowExpression), alternatives, requires, flags);
    }

    public SimpleAttributeDefinition(String name, String xmlName, final ModelNode defaultValue, final ModelType type,
            final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
            final ParameterCorrector corrector, final ParameterValidator validator,
            String[] alternatives, String[] requires, AttributeAccess.Flag... flags) {
        super(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                corrector, createParameterValidator(validator, type, allowNull, allowExpression), alternatives, requires, flags);
    }

    public SimpleAttributeDefinition(final String name, final ModelNode defaultValue, final ModelType type, final boolean allowNull, final String[] alternatives) {
        this(name, name, defaultValue, type, allowNull, false, MeasurementUnit.NONE, null, alternatives, null);
    }

    public SimpleAttributeDefinition(final String name, final ModelType type, final boolean allowNull, ParameterCorrector corrector, ParameterValidator validator) {
        this(name, name, null, type, allowNull, false, MeasurementUnit.NONE, corrector, validator, null, null);
    }

    private static ParameterValidator createParameterValidator(final ParameterValidator existing, final ModelType type,final boolean allowNull, final boolean allowExpression) {
        if (existing != null) {
            return existing;
        } else if (type == ModelType.STRING) {
            return new StringLengthValidator(1, Integer.MAX_VALUE, allowNull, allowExpression);
        } else {
            return new ModelTypeValidator(type, allowNull, allowExpression);
        }
    }

    /**
     * Creates and returns a {@link org.jboss.dmr.ModelNode} using the given {@code value} after first validating the node
     * against {@link #getValidator() this object's validator}.
     * <p>
     * If {@code value} is {@code null} an {@link ModelType#UNDEFINED undefined} node will be returned.
     * </p>
     *
     * @param value the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param reader {@link XMLStreamReader} from which the {@link XMLStreamReader#getLocation() location} from which
     *               the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *               the given value is invalid.
     * @return {@code ModelNode} representing the parsed value
     *
     * @throws javax.xml.stream.XMLStreamException if {@code value} is not valid
     *
     * @see #parseAndSetParameter(String, ModelNode, XMLStreamReader)
     */
    public ModelNode parse(final String value, final XMLStreamReader reader) throws XMLStreamException {
        try {
            return parse(value);
        } catch (OperationFailedException e) {
            throw new XMLStreamException(e.getFailureDescription().toString(), reader.getLocation());
        }
    }

    /**
     * Creates and returns a {@link org.jboss.dmr.ModelNode} using the given {@code value} after first validating the node
     * against {@link #getValidator() this object's validator}.
     * <p>
     * If {@code value} is {@code null} an {@link ModelType#UNDEFINED undefined} node will be returned.
     * </p>
     *
     * @param value the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param location current location of the parser's {@link javax.xml.stream.XMLStreamReader}. Used for any exception
     *                 message
     *
     * @return {@code ModelNode} representing the parsed value
     *
     * @throws javax.xml.stream.XMLStreamException if {@code value} is not valid
     *
     * @deprecated use {@link #parse(String, XMLStreamReader)}
     *
     * @see #parseAndSetParameter(String, ModelNode, Location)
     */
    @Deprecated
    public ModelNode parse(final String value, final Location location) throws XMLStreamException {
        try {
            return parse(value);
        } catch (OperationFailedException e) {
            throw new XMLStreamException(e.getFailureDescription().toString(), location);
        }
    }

    /**
     * Creates a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link #getValidator() this object's validator}, and then stores it in the given {@code operation}
     * model node as a key/value pair whose key is this attribute's {@link #getName() name}.
     * <p>
     * If {@code value} is {@code null} an {@link ModelType#UNDEFINED undefined} node will be stored if such a value
     * is acceptable to the validator.
     * </p>
     * <p>
     * The expected usage of this method is in parsers seeking to build up an operation to store their parsed data
     * into the configuration.
     * </p>
     *
     * @param value the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param operation model node of type {@link ModelType#OBJECT} into which the parsed value should be stored
     * @param reader {@link XMLStreamReader} from which the {@link XMLStreamReader#getLocation() location} from which
     *               the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *               the given value is invalid.
     * @throws XMLStreamException if {@code value} is not valid
     */
    public void parseAndSetParameter(final String value, final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException {
        ModelNode paramVal = parse(value, reader);
        operation.get(getName()).set(paramVal);
    }

    /**
     * Creates a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link #getValidator() this object's validator}, and then stores it in the given {@code operation}
     * model node as a key/value pair whose key is this attribute's {@link #getName() name}.
     * <p>
     * If {@code value} is {@code null} an {@link ModelType#UNDEFINED undefined} node will be stored if such a value
     * is acceptable to the validator.
     * </p>
     * <p>
     * The expected usage of this method is in parsers seeking to build up an operation to store their parsed data
     * into the configuration.
     * </p>
     *
     * @param value the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param operation model node of type {@link ModelType#OBJECT} into which the parsed value should be stored
     * @param location current location of the parser's {@link javax.xml.stream.XMLStreamReader}. Used for any exception
     *                 message
     * @throws XMLStreamException if {@code value} is not valid
     *
     * @deprecated use {@link #parseAndSetParameter(String, ModelNode, XMLStreamReader)}
     */
    @Deprecated
    public void parseAndSetParameter(final String value, final ModelNode operation, final Location location) throws XMLStreamException {
        @SuppressWarnings("deprecation")
        ModelNode paramVal = parse(value, location);
        operation.get(getName()).set(paramVal);
    }


    /**
     * Marshalls the value from the given {@code resourceModel} as an xml attribute, if it
     * {@link #isMarshallable(org.jboss.dmr.ModelNode, boolean) is marshallable}.
     * <p>
     * Invoking this method is the same as calling {@code marshallAsAttribute(resourceModel, true, writer)}
     * </p>
     *
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param writer stream writer to use for writing the attribute
     * @throws javax.xml.stream.XMLStreamException if {@code writer} throws an exception
     */
    public void marshallAsAttribute(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException {
        marshallAsAttribute(resourceModel, true, writer);
    }

    /**
     * Marshalls the value from the given {@code resourceModel} as an xml attribute, if it
     * {@link #isMarshallable(org.jboss.dmr.ModelNode, boolean) is marshallable}.
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param marshallDefault {@code true} if the value should be marshalled even if it matches the default value
     * @param writer stream writer to use for writing the attribute
     * @throws javax.xml.stream.XMLStreamException if {@code writer} throws an exception
     */
    public void marshallAsAttribute(final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(resourceModel, marshallDefault)) {
            writer.writeAttribute(getXmlName(), resourceModel.get(getName()).asString());
        }
    }

    /**
     * {@inheritDoc}
     * Invoking this method is the same as calling {@code marshallAsElementText(resourceModel, true, writer)}
     */
    @Override
    public void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException {
        marshallAsElement(resourceModel, true, writer);
    }

    /**
     * {@inheritDoc}
     *
     * This implementation marshalls the attribute value as text content of the element.
     */
    public void marshallAsElement(final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(resourceModel, marshallDefault)) {
            writer.writeStartElement(getXmlName());
            writer.writeCharacters(resourceModel.get(getName()).asString());
            writer.writeEndElement();
        }
    }

    private ModelNode parse(final String value) throws OperationFailedException  {

        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null ) {
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
//        else if (getDefaultValue()!= null && getDefaultValue().isDefined()) {
//            node = new ModelNode().set(getDefaultValue());
//        }
        else {
            node = new ModelNode();
        }

        getValidator().validateParameter(getXmlName(), node);

        return node;
    }
}
