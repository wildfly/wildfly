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

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Defining characteristics of an {@link ModelType#LIST} attribute in a {@link org.jboss.as.controller.registry.Resource}, with utility
 * methods for conversion to and from xml and for validation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class ListAttributeDefinition extends AttributeDefinition {

    private final ParameterValidator elementValidator;

    public ListAttributeDefinition(final String name, final boolean allowNull, final ParameterValidator elementValidator) {
        this(name, name, allowNull, 0, Integer.MAX_VALUE, elementValidator, null, null);
    }

    public ListAttributeDefinition(final String name, final boolean allowNull, final ParameterValidator elementValidator, final AttributeAccess.Flag... flags) {
        this(name, name, allowNull, 0, Integer.MAX_VALUE, elementValidator, null, null, flags);
    }

    public ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                   final int minSize, final int maxSize, final ParameterValidator elementValidator) {
        this(name, xmlName, allowNull, minSize, maxSize, elementValidator, null, null);
    }

    public ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                   final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                   final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        super(name, xmlName, null, ModelType.LIST, allowNull, false, null, new ListValidator(elementValidator, allowNull, minSize, maxSize), alternatives, requires, flags);
        this.elementValidator = elementValidator;
    }

    /**
     * The validator used to validate elements in the list.
     * @return  the element validator
     */
    public ParameterValidator getElementValidator() {
        return elementValidator;
    }

    /**
     * Creates and returns a {@link org.jboss.dmr.ModelNode} using the given {@code value} after first validating the node
     * against {@link #getElementValidator() this object's element validator}.
     * <p>
     * If {@code value} is {@code null} an {@link ModelType#UNDEFINED undefined} node will be returned.
     * </p>
     *
     * @param value the value. Will be {@link String#trim() trimmed} before use if not {@code null}.
     * @param reader {@link XMLStreamReader} from which the {@link XMLStreamReader#getLocation() location} from which
     *               the attribute value was read can be obtained and used in any {@code XMLStreamException}, in case
     *               the given value is invalid.
     *
     * @return {@code ModelNode} representing the parsed value
     *
     * @throws javax.xml.stream.XMLStreamException if {@code value} is not valid
     *
     * @see #parseAndAddParameterElement(String, ModelNode, XMLStreamReader)
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
     * against {@link #getElementValidator() this object's element validator}.
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
     * @see #parseAndAddParameterElement(String, ModelNode, Location)
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
     * model node as an element in a {@link ModelType#LIST} value in a key/value pair whose key is this attribute's
     * {@link #getName() name}.
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
    public void parseAndAddParameterElement(final String value, final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException {
        ModelNode paramVal = parse(value, reader);
        operation.get(getName()).add(paramVal);
    }

    /**
     * Creates a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link #getValidator() this object's validator}, and then stores it in the given {@code operation}
     * model node as an element in a {@link ModelType#LIST} value in a key/value pair whose key is this attribute's
     * {@link #getName() name}.
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
     * @deprecated use {@link #parseAndAddParameterElement(String, ModelNode, XMLStreamReader)}
     */
    @Deprecated
    public void parseAndAddParameterElement(final String value, final ModelNode operation, final Location location) throws XMLStreamException {
        @SuppressWarnings("deprecation")
        ModelNode paramVal = parse(value, location);
        operation.get(getName()).add(paramVal);
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
        final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
        addValueTypeDescription(result, bundle);
        return result;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ModelNode resourceDescription, ResourceDescriptionResolver resolver,
                                                     Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
        addAttributeValueTypeDescription(result, resolver, locale, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ModelNode resourceDescription, String operationName,
                                                      ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
        addOperationParameterValueTypeDescription(result, operationName, resolver, locale, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
        final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);
        addValueTypeDescription(result, bundle);
        return result;
    }

    protected abstract void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle);

    protected abstract void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver,
                                                             final Locale locale, final ResourceBundle bundle);

    protected abstract void addOperationParameterValueTypeDescription(final ModelNode node, final String operationName,
                                                                      final ResourceDescriptionResolver resolver,
                                                                      final Locale locale, final ResourceBundle bundle);

    private ModelNode parse(final String value) throws OperationFailedException  {

        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null ) {
            node = new ModelNode().set(trimmed);
        } else {
            node = new ModelNode();
        }

        elementValidator.validateParameter(getXmlName(), node);

        return node;
    }
}
