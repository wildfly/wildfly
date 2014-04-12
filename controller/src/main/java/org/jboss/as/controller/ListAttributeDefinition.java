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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.NillableOrExpressionParameterValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.parsing.ParseUtils;
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

    @SuppressWarnings("deprecation")
    public ListAttributeDefinition(final String name, final boolean allowNull, final ParameterValidator elementValidator) {
        this(name, name, allowNull, false, 0, Integer.MAX_VALUE, elementValidator, null, null, null,false, null,
                null, null, null, (AttributeAccess.Flag[]) null);
    }

    @SuppressWarnings("deprecation")
    public ListAttributeDefinition(final String name, final boolean allowNull, final ParameterValidator elementValidator,
                                      final AttributeAccess.Flag... flags) {
        this(name, name, allowNull, false, 0, Integer.MAX_VALUE, elementValidator, null, null, null,false, null, null, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    public ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                   final int minSize, final int maxSize, final ParameterValidator elementValidator) {
        this(name, xmlName, allowNull, false, minSize, maxSize, elementValidator, null, null, null, false, null, null,
                null, (AttributeParser) null);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                      final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                      final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller, boolean resourceOnly, DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, false, minSize, maxSize, elementValidator, alternatives, requires, attributeMarshaller, resourceOnly,
                deprecated, null, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull,
                                      final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                      final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, false, minSize, maxSize, elementValidator, alternatives, requires, null, false, null, null, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions,
            final int minSize, final int maxSize, final ParameterValidator elementValidator,
            final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,
            boolean resourceOnly, final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, allowExpressions, minSize, maxSize, elementValidator, alternatives, requires, attributeMarshaller, resourceOnly,
                deprecated, null, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions,
                                   final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                   final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,
                                   final boolean resourceOnly, final DeprecationData deprecated,
                                   final AccessConstraintDefinition[] accessConstraints, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, allowExpressions, minSize, maxSize, elementValidator, alternatives, requires,
                attributeMarshaller, resourceOnly, deprecated, accessConstraints, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions,
                                   final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                   final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, allowExpressions, minSize, maxSize, elementValidator, alternatives, requires,
                null, false, null, null, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions,
                                   final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                   final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,
                                   final AccessConstraintDefinition[] accessConstraints, final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, allowExpressions, minSize, maxSize, elementValidator, alternatives, requires,
                attributeMarshaller, false, null, accessConstraints, null, null, flags);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions,
                                      final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                      final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,
                                      final boolean resourceOnly,  final DeprecationData deprecated,
                                      final AccessConstraintDefinition[] accessConstraints, final Boolean niSignificant,
                                      final AttributeAccess.Flag... flags) {
        this(name, xmlName, allowNull, allowExpressions, minSize, maxSize, elementValidator, alternatives, requires,
                attributeMarshaller, resourceOnly, deprecated, accessConstraints, niSignificant, null, flags);
    }

    @Deprecated
    protected ListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions,
                                          final int minSize, final int maxSize, final ParameterValidator elementValidator,
                                          final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,
                                          final boolean resourceOnly,  final DeprecationData deprecated,
                                          final AccessConstraintDefinition[] accessConstraints,
                                          final Boolean niSignificant,
                                          final AttributeParser parser,
                                          final AttributeAccess.Flag... flags) {
        super(name, xmlName, null, ModelType.LIST, allowNull, allowExpressions, null, null,
                new ListValidator(elementValidator, allowNull, minSize, maxSize), allowNull, alternatives, requires,
                attributeMarshaller, resourceOnly, deprecated, accessConstraints, niSignificant, parser, flags);
        this.elementValidator = elementValidator;
    }

    protected ListAttributeDefinition(ListAttributeDefinition.Builder<?, ?> builder) {
        super(builder);
        this.elementValidator = builder.getElementValidator();
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
     * Creates a {@link ModelNode} using the given {@code value} after first validating the node
     * against {@link #getElementValidator() this object's element validator}, and then stores it in the given {@code operation}
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
        } else {
            node = new ModelNode();
        }

        elementValidator.validateParameter(getXmlName(), node);

        return node;
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
        attributeMarshaller.marshallAsElement(this,resourceModel,marshallDefault,writer);
    }

    /**
     * Iterates through the elements in the {@code parameter} list, calling {@link #convertParameterElementExpressions(ModelNode)}
     * for each.
     * <p>
     * <strong>Note</strong> that the default implementation of {@link #convertParameterElementExpressions(ModelNode)}
     * will only convert simple {@link ModelType#STRING} elements. If users need to handle complex elements
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
            ModelNode newList = new ModelNode().setEmptyList();
            for (ModelNode item : parameter.asList()) {
                ModelNode converted = convertParameterElementExpressions(item);
                newList.add(converted);
                changeMade |= !converted.equals(item);
            }
            if (changeMade) {
                result = newList;
            }
        }
        return result;
    }

    /**
     * Examine the given element of a parameter list for any expression syntax, converting the relevant node to
     * {@link ModelType#EXPRESSION} if such is supported. This implementation will only convert elements of
     * {@link ModelType#STRING}. Subclasses that need to handle complex elements should override this method.
     *
     * @param parameterElement the node to examine. Will not be {@code null}
     * @return the parameter element with expressions converted, or the original parameter if no conversion was performed
     *         Cannot return {@code null}
     */
    protected ModelNode convertParameterElementExpressions(ModelNode parameterElement) {
        return isAllowExpression() ? convertStringExpression(parameterElement) : parameterElement;
    }

    public abstract static class Builder<BUILDER extends Builder, ATTRIBUTE extends ListAttributeDefinition>
            extends AbstractAttributeDefinitionBuilder<BUILDER, ATTRIBUTE> {

        private ParameterValidator elementValidator;
        private Boolean allowNullElement;

        protected Builder(String attributeName) {
            super(attributeName, ModelType.LIST);
        }

        protected Builder(String attributeName, boolean allowNull) {
            super(attributeName, ModelType.LIST, allowNull);
        }

        public Builder(ListAttributeDefinition basis) {
            super(basis);
            this.elementValidator = basis.getElementValidator();
        }

        /**
         * Gets the validator to use for validating list elements. En
         * @return the validator, or {@code null} if no validator has been set
         */
        public ParameterValidator getElementValidator() {
            if (elementValidator == null) {
                return null;
            }

            ParameterValidator toWrap = elementValidator;
            ParameterValidator wrappedElementValidator = null;
            if (elementValidator instanceof  NillableOrExpressionParameterValidator) {
                // See if it's configured correctly already; if so don't re-wrap
                NillableOrExpressionParameterValidator wrapped = (NillableOrExpressionParameterValidator) elementValidator;
                Boolean allow = wrapped.getAllowNull();
                if ((allow == null || allow) == getAllowNullElement()
                        && wrapped.isAllowExpression() == isAllowExpression()) {
                    wrappedElementValidator = wrapped;
                } else {
                    // re-wrap
                    toWrap = wrapped.getDelegate();
                }
            }
            if (wrappedElementValidator == null) {
                elementValidator = new NillableOrExpressionParameterValidator(toWrap, getAllowNullElement(), isAllowExpression());
            }
            return elementValidator;
        }

        /**
         * Sets the validator to use for validating list elements.
         *
         * @param elementValidator the validator
         * @return a builder that can be used to continue building the attribute definition
         *
         * @throws java.lang.IllegalArgumentException if {@code elementValidator} is {@code null}
         */
        public final BUILDER setElementValidator(ParameterValidator elementValidator) {
            if (elementValidator == null) {
                throw ControllerMessages.MESSAGES.nullVar("elementValidator");
            }
            this.elementValidator = elementValidator;
            // Setting an element validator invalidates any existing overall attribute validator
            this.validator = null;
            return (BUILDER) this;
        }

        /**
         * Overrides the superclass to simply delegate to
         * {@link #setElementValidator(org.jboss.as.controller.operations.validation.ParameterValidator)}.
         * Use {@link #setListValidator(org.jboss.as.controller.operations.validation.ParameterValidator)} to
         * set an overall validator for the list.
         *
         * @param validator the validator. Cannot be {@code null}
         * @return a builder that can be used to continue building the attribute definition
         *
         * @throws java.lang.IllegalArgumentException if {@code elementValidator} is {@code null}
         */
        @Override
        public BUILDER setValidator(ParameterValidator validator) {
            return setElementValidator(validator);
        }

        /**
         * Sets an overall validator for the list.
         *
         * @param validator the validator. {@code null} is allowed
         * @return a builder that can be used to continue building the attribute definition
         */
        public BUILDER setListValidator(ParameterValidator validator) {
            return super.setValidator(validator);
        }

        @Override
        public int getMinSize() {
            if (minSize < 0) { minSize = 0;}
            return minSize;
        }

        @Override
        public int getMaxSize() {
            if (maxSize < 1) { maxSize = Integer.MAX_VALUE; }
            return maxSize;
        }

        /**
         * Gets whether undefined list elements are valid. In the unlikely case {@link #setAllowNullElement(boolean)}
         * has been called, that value is returned; otherwise the value of {@link #isAllowNull()} is used.
         *
         * @return {@code true} if undefined list elements are valid
         */
        public boolean getAllowNullElement() {
            return allowNullElement == null ? isAllowNull() : allowNullElement;
        }

        /**
         * Sets whether undefined list elements are valid.
         * @param allowNullElement whether undefined elements are valid
         * @return a builder that can be used to continue building the attribute definition
         */
        public BUILDER setAllowNullElement(boolean allowNullElement) {
            this.allowNullElement = allowNullElement;
            return (BUILDER) this;
        }

        @Override
        public ParameterValidator getValidator() {
            ParameterValidator result = super.getValidator();
            if (result == null) {
                ParameterValidator listElementValidator = getElementValidator();
                // Subclasses must call setElementValidator before calling this
                assert listElementValidator != null;
                result = new ListValidator(getElementValidator(), isAllowNull(), getMinSize(), getMaxSize());
            }
            return result;
        }
    }
}
