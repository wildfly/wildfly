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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.NillableOrExpressionParameterValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
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
public abstract class AttributeDefinition {

    /** The {@link ModelType} types that reflect complex DMR structures -- {@code LIST}, {@code OBJECT}, {@code PROPERTY}} */
    protected static final Set<ModelType> COMPLEX_TYPES = Collections.unmodifiableSet(EnumSet.of(ModelType.LIST, ModelType.OBJECT, ModelType.PROPERTY));

    private final String name;
    private final String xmlName;
    private final ModelType type;
    private final boolean allowNull;
    private final boolean allowExpression;
    private final ModelNode defaultValue;
    private final MeasurementUnit measurementUnit;
    private final String[] alternatives;
    private final String[] requires;
    private final ParameterCorrector valueCorrector;
    private final ParameterValidator validator;
    private final EnumSet<AttributeAccess.Flag> flags;
    protected final AttributeMarshaller attributeMarshaller;
    private final boolean resourceOnly;
    private final DeprecationData deprecationData;

    protected AttributeDefinition(String name, String xmlName, final ModelNode defaultValue, final ModelType type,
                               final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
                               final ParameterValidator validator, final String[] alternatives, final String[] requires,
                               final AttributeAccess.Flag... flags) {
        this(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                null, validator, true, alternatives, requires, null, false, null, flags);
    }

    protected AttributeDefinition(String name, String xmlName, final ModelNode defaultValue, final ModelType type,
                                  final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
                                  final ParameterCorrector valueCorrector, final ParameterValidator validator,
                                  boolean validateNull, final String[] alternatives, final String[] requires, AttributeMarshaller attributeMarshaller,
                                  boolean resourceOnly, DeprecationData deprecationData, final AttributeAccess.Flag... flags) {

        this.name = name;
        this.xmlName = xmlName;
        this.type = type;
        this.allowNull = allowNull;
        this.allowExpression = allowExpression;
        this.defaultValue = new ModelNode();
        if (defaultValue != null) {
            this.defaultValue.set(defaultValue);
        }
        this.defaultValue.protect();
        this.measurementUnit = measurementUnit;
        this.alternatives = alternatives;
        this.requires = requires;
        this.valueCorrector = valueCorrector;
        if (validator == null) {
            this.validator = null;
        } else {
            Boolean nullCheck = validateNull ? allowNull : null;
            this.validator = new NillableOrExpressionParameterValidator(validator, nullCheck, allowExpression);
        }
        if (flags == null || flags.length == 0) {
            this.flags = EnumSet.noneOf(AttributeAccess.Flag.class);
        } else if (flags.length == 1) {
            this.flags = EnumSet.of(flags[0]);
        } else {
            this.flags = EnumSet.of(flags[0], flags);
        }
        if (attributeMarshaller != null) {
            this.attributeMarshaller = attributeMarshaller;
        } else {
            this.attributeMarshaller = new DefaultAttributeMarshaller();
        }
        this.resourceOnly = resourceOnly;
        this.deprecationData = deprecationData;
    }

    public String getName() {
        return name;
    }

    public String getXmlName() {
        return xmlName;
    }

    public ModelType getType() {
        return type;
    }

    public boolean isAllowNull() {
        return allowNull;
    }

    public boolean isAllowExpression() {
        return allowExpression;
    }

    public ModelNode getDefaultValue() {
        return defaultValue.isDefined() ? defaultValue : null;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public ParameterValidator getValidator() {
        return validator;
    }

    public String[] getAlternatives() {
        return alternatives;
    }

    public String[] getRequires() {
        return requires;
    }

    public EnumSet<AttributeAccess.Flag> getFlags() {
        return EnumSet.copyOf(flags);
    }

    /**
     * Gets whether the given {@code resourceModel} has a value for this attribute that should be marshalled to XML.
     * <p>
     * This is the same as {@code isMarshallable(resourceModel, true)}.
     * </p>
     *
     * @param resourceModel the model, a non-null node of {@link ModelType#OBJECT}.
     *
     * @return {@code true} if the given {@code resourceModel} has a defined value under this attribute's {@link #getName()} () name}.
     */
    public boolean isMarshallable(final ModelNode resourceModel) {
        return attributeMarshaller.isMarshallable(this, resourceModel, true);
    }

    /**
     * Gets whether the given {@code resourceModel} has a value for this attribute that should be marshalled to XML.
     *
     * @param resourceModel the model, a non-null node of {@link ModelType#OBJECT}.
     * @param marshallDefault {@code true} if the value should be marshalled even if it matches the default value
     *
     * @return {@code true} if the given {@code resourceModel} has a defined value under this attribute's {@link #getName()} () name}
     * and {@code marshallDefault} is {@code true} or that value differs from this attribute's {@link #getDefaultValue() default value}.
     */
    public boolean isMarshallable(final ModelNode resourceModel, final boolean marshallDefault) {
        return attributeMarshaller.isMarshallable(this, resourceModel, marshallDefault);
    }

    /**
     * Finds a value in the given {@code operationObject} whose key matches this attribute's {@link #getName() name} and
     * validates it using this attribute's {@link #getValidator() validator}.
     *
     * @param operationObject model node of type {@link ModelType#OBJECT}, typically representing an operation request
     *
     * @return the value
     * @throws OperationFailedException if the value is not valid
     */
    public ModelNode validateOperation(final ModelNode operationObject) throws OperationFailedException {
        return validateOperation(operationObject, false);
    }

    /**
     * Finds a value in the given {@code operationObject} whose key matches this attribute's {@link #getName() name},
     * validates it using this attribute's {@link #getValidator() validator}, and, stores it under this attribute's name in the given {@code model}.
     *
     * @param operationObject model node of type {@link ModelType#OBJECT}, typically representing an operation request
     * @param model model node in which the value should be stored
     *
     * @throws OperationFailedException if the value is not valid
     */
    public final void validateAndSet(ModelNode operationObject, final ModelNode model) throws OperationFailedException {
        if (operationObject.hasDefined(name) && isDeprecated()) {
            ControllerLogger.DEPRECATED_LOGGER.attributeDeprecated(getName());
        }
        // AS7-6224 -- convert expression strings to ModelType.EXPRESSION *before* correcting
        ModelNode newValue = convertParameterExpressions(operationObject.get(name));
        final ModelNode correctedValue = correctValue(newValue, model.get(name));
        if (!correctedValue.equals(operationObject.get(name))) {
            operationObject.get(name).set(correctedValue);
        }
        ModelNode node = validateOperation(operationObject, true);
        model.get(name).set(node);
    }

    /**
     *
     * @deprecated Use {@link #resolveModelAttribute(OperationContext, ModelNode)} instead
     */
    @Deprecated
    public ModelNode validateResolvedOperation(final ModelNode operationObject) throws OperationFailedException {
        return resolveModelAttribute(NO_OPERATION_CONTEXT_FOR_RESOLVING_MODEL_PARAMETERS, operationObject);
    }

    /**
     * Finds a value in the given {@code model} whose key matches this attribute's {@link #getName() name},
     * resolves it and validates it using this attribute's {@link #getValidator() validator}. If the value is
     * undefined and a {@link #getDefaultValue() default value} is available, the default value is used.
     *
     * @param context the operation context
     * @param model model node of type {@link ModelType#OBJECT}, typically representing a model resource
     *
     * @return the resolved value, possibly the default value if the model does not have a defined value matching
     *              this attribute's name
     * @throws OperationFailedException if the value is not valid
     */
    public ModelNode resolveModelAttribute(final OperationContext context, final ModelNode model) throws OperationFailedException {
        return resolveModelAttribute(new ExpressionResolver() {
            @Override
            public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
                return context.resolveExpressions(node);
            }
        }, model);
    }

    public ModelNode resolveModelAttribute(final ExpressionResolver resolver, final ModelNode model) throws OperationFailedException {
        final ModelNode node = new ModelNode();
        if(model.has(name)) {
            node.set(model.get(name));
        }
        if (!node.isDefined() && defaultValue.isDefined()) {
            node.set(defaultValue);
        }
        final ModelNode resolved = resolver.resolveExpressions(node);
        validator.validateParameter(name, resolved);
        return resolved;
    }

    public boolean isAllowed(final ModelNode operationObject) {
        if(alternatives != null) {
            for(final String alternative : alternatives) {
                if(operationObject.hasDefined(alternative)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isRequired(final ModelNode operationObject) {
        final boolean required = ! allowNull;
        return required ? ! hasAlternative(operationObject) : required;
    }

    public boolean hasAlternative(final ModelNode operationObject) {
        if(alternatives != null) {
            for(final String alternative : alternatives) {
                if(operationObject.hasDefined(alternative)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Marshalls the value from the given {@code resourceModel} as an xml attribute, if it
     * {@link #isMarshallable(org.jboss.dmr.ModelNode, boolean) is marshallable}.
     *
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param writer stream writer to use for writing the attribute
     * @throws javax.xml.stream.XMLStreamException if thrown by {@code writer}
     */
    public void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException{
        marshallAsElement(resourceModel,true,writer);
    }

    /**
     * Marshalls the value from the given {@code resourceModel} as an xml element, if it
     * {@link #isMarshallable(org.jboss.dmr.ModelNode, boolean) is marshallable}.
     *
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param writer        stream writer to use for writing the attribute
     * @throws javax.xml.stream.XMLStreamException
     *          if thrown by {@code writer}
     */
    public void marshallAsElement(final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException{
        throw ControllerMessages.MESSAGES.couldNotMarshalAttributeAsElement(getName());
    }

    /**
     * Creates a returns a basic model node describing the attribute, after attaching it to the given overall resource
     * description model node.  The node describing the attribute is returned to make it easy to perform further
     * modification.
     *
     * @param bundle resource bundle to use for text descriptions
     * @param prefix prefix to prepend to the attribute name key when looking up descriptions
     * @param resourceDescription  the overall resource description
     * @return  the attribute description node
     */
    public ModelNode addResourceAttributeDescription(final ResourceBundle bundle, final String prefix, final ModelNode resourceDescription) {
        final ModelNode attr = getNoTextDescription(false);
        attr.get(ModelDescriptionConstants.DESCRIPTION).set(getAttributeTextDescription(bundle, prefix));
        final ModelNode result = resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES, getName()).set(attr);
        ModelNode deprecated = addDeprecatedInfo(result);
        if (deprecated != null) {
            deprecated.get(ModelDescriptionConstants.REASON).set(getAttributeDeprecatedDescription(bundle, prefix));
        }
        return result;
    }

    /**
     * Creates a returns a basic model node describing the attribute, after attaching it to the given overall resource
     * description model node.  The node describing the attribute is returned to make it easy to perform further
     * modification.
     *
     * @param resourceDescription  the overall resource description
     * @param resolver provider of localized text descriptions
     * @param locale locale to pass to the resolver
     * @param bundle bundle to pass to the resolver
     * @return  the attribute description node
     */
    public ModelNode addResourceAttributeDescription(final ModelNode resourceDescription, final ResourceDescriptionResolver resolver,
                                                     final Locale locale, final ResourceBundle bundle) {
        final ModelNode attr = getNoTextDescription(false);
        final String description = resolver.getResourceAttributeDescription(getName(), locale, bundle);
        attr.get(ModelDescriptionConstants.DESCRIPTION).set(description);
        final ModelNode result = resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES, getName()).set(attr);
        ModelNode deprecated = addDeprecatedInfo(result);
        if (deprecated != null) {
            deprecated.get(ModelDescriptionConstants.REASON).set(resolver.getResourceAttributeDeprecatedDescription(getName(), locale, bundle));
        }
        return result;
    }

    /**
     * Creates a returns a basic model node describing a parameter that sets this attribute, after attaching it to the
     * given overall operation description model node.  The node describing the parameter is returned to make it easy
     * to perform further modification.
     *
     * @param bundle resource bundle to use for text descriptions
     * @param prefix prefix to prepend to the attribute name key when looking up descriptions
     * @param operationDescription  the overall resource description
     * @return  the attribute description node
     */
    public ModelNode addOperationParameterDescription(final ResourceBundle bundle, final String prefix, final ModelNode operationDescription) {
        final ModelNode param = getNoTextDescription(true);
        param.get(ModelDescriptionConstants.DESCRIPTION).set(getAttributeTextDescription(bundle, prefix));
        final ModelNode result = operationDescription.get(ModelDescriptionConstants.REQUEST_PROPERTIES, getName()).set(param);
        ModelNode deprecated = addDeprecatedInfo(result);
        if (deprecated != null) {
            deprecated.get(ModelDescriptionConstants.REASON).set(getAttributeDeprecatedDescription(bundle, prefix));
        }
        return result;
    }

    /**
     * Creates a returns a basic model node describing a parameter that sets this attribute, after attaching it to the
     * given overall operation description model node.  The node describing the parameter is returned to make it easy
     * to perform further modification.
     *
     * @param resourceDescription  the overall resource description
     * @param operationName the operation name
     * @param resolver provider of localized text descriptions
     * @param locale locale to pass to the resolver
     * @param bundle bundle to pass to the resolver
     * @return  the attribute description node
     */
    public ModelNode addOperationParameterDescription(final ModelNode resourceDescription, final String operationName,
                                                      final ResourceDescriptionResolver resolver,
                                                      final Locale locale, final ResourceBundle bundle) {
        final ModelNode param = getNoTextDescription(true);
        final String description = resolver.getOperationParameterDescription(operationName, getName(), locale, bundle);
        param.get(ModelDescriptionConstants.DESCRIPTION).set(description);
        final ModelNode result = resourceDescription.get(ModelDescriptionConstants.REQUEST_PROPERTIES, getName()).set(param);
        ModelNode deprecated = addDeprecatedInfo(result);
        if (deprecated != null) {
            deprecated.get(ModelDescriptionConstants.REASON).set(resolver.getOperationParameterDeprecatedDescription(operationName, getName(), locale, bundle));
        }
        return result;
    }

    public String getAttributeTextDescription(final ResourceBundle bundle, final String prefix) {
        final String bundleKey = prefix == null ? name : (prefix + "." + name);
        return bundle.getString(bundleKey);
    }

    public String getAttributeDeprecatedDescription(final ResourceBundle bundle, final String prefix) {
        String bundleKey = prefix == null ? name : (prefix + "." + name);
        bundleKey += "." + ModelDescriptionConstants.DEPRECATED;
        return bundle.getString(bundleKey);
    }

    public ModelNode addDeprecatedInfo(final ModelNode model) {
        if (deprecationData == null) { return null; }
        ModelNode deprecated = model.get(ModelDescriptionConstants.DEPRECATED);
        deprecated.get(ModelDescriptionConstants.SINCE).set(deprecationData.getSince().toString());
        /*String bundleKey = prefix == null ? name : (prefix + "." + name);
        bundleKey+="."+ModelDescriptionConstants.DEPRECATED;*/
        //deprecated.get(ModelDescriptionConstants.REASON).set(bundle.getString(bundleKey));
        deprecated.get(ModelDescriptionConstants.REASON);
        return deprecated;
    }

    public ModelNode getNoTextDescription(boolean forOperation) {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.TYPE).set(type);
        result.get(ModelDescriptionConstants.DESCRIPTION); // placeholder
        result.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(isAllowExpression());
        if (forOperation) {
            result.get(ModelDescriptionConstants.REQUIRED).set(!isAllowNull());
        }
        result.get(ModelDescriptionConstants.NILLABLE).set(isAllowNull());
        if (defaultValue != null && defaultValue.isDefined()) {
            result.get(ModelDescriptionConstants.DEFAULT).set(defaultValue);
        }
        if (measurementUnit != null && measurementUnit != MeasurementUnit.NONE) {
            result.get(ModelDescriptionConstants.UNIT).set(measurementUnit.getName());
        }
        if (alternatives != null) {
            for(final String alternative : alternatives) {
                result.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
            }
        }
        if (requires != null) {
            for(final String required : requires) {
                result.get(ModelDescriptionConstants.REQUIRES).add(required);
            }
        }
        if (validator instanceof MinMaxValidator) {
            MinMaxValidator minMax = (MinMaxValidator) validator;
            Long min = minMax.getMin();
            if (min != null) {
                switch (this.type) {
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
                switch (this.type) {
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

    /**
     * Adds the allowed values. Override for attributes who should not use the allowed values.
     *
     * @param result the node to add the allowed values to
     * @param validator the validator to get the allowed values from
     */
    protected void addAllowedValuesToDescription(ModelNode result, ParameterValidator validator) {
        if (validator instanceof AllowedValuesValidator) {
            AllowedValuesValidator avv = (AllowedValuesValidator) validator;
            List<ModelNode> allowed = avv.getAllowedValues();
            if (allowed != null) {
                for (ModelNode ok : allowed) {
                    result.get(ModelDescriptionConstants.ALLOWED).add(ok);
                }
            }
        }
    }

    /**
     * Corrects the value if the {@link ParameterCorrector value corrector} is not {@code null}. If the {@link
     * ParameterCorrector value corrector} is {@code null}, the {@code newValue} parameter is returned.
     *
     * @param newValue the new value.
     * @param oldValue the old value.
     *
     * @return the corrected value or the {@code newValue} if the {@link ParameterCorrector value corrector} is {@code
     *         null}.
     */
    protected final ModelNode correctValue(final ModelNode newValue, final ModelNode oldValue) {
        if (valueCorrector != null) {
            return valueCorrector.correct(newValue, oldValue);
        }
        return newValue;
    }

    /**
     * Examine the given operation parameter value for any expression syntax, converting the relevant node to
     * {@link ModelType#EXPRESSION} if such is supported.
     * <p>
     * This implementation checks if {@link #isAllowExpression() expressions are allowed} and if so, calls
     * {@link #convertStringExpression(ModelNode)} to convert a {@link ModelType#STRING} to a {@link ModelType#EXPRESSION}.
     * No other conversions are performed. For use cases requiring more complex behavior, a subclass that overrides
     * this method should be used.
     * </p>
     * <p>
     * If expressions are supported this implementation also checks if the {@link #getType() attribute type} is one of
     * the {@link #COMPLEX_TYPES complex DMR types}. If it is, an {@link IllegalStateException} is thrown, as this
     * implementation cannot properly handle such a combination, and a subclass that overrides this method should be used.
     * </p>
     *
     * @param parameter the node to examine. Cannot not be {@code null}
     * @return a node matching {@code parameter} but with expressions converted, or the original parameter if no
     *         conversion was performed. Will not return {@code null}
     *
     * @throws IllegalStateException if expressions are supported, but the {@link #getType() attribute type} is {@link #COMPLEX_TYPES complex}
     */
    protected ModelNode convertParameterExpressions(final ModelNode parameter) {
        if (isAllowExpression() && COMPLEX_TYPES.contains(type)) {
            // They need to subclass and override
            throw new IllegalStateException();
        }
        return isAllowExpression() ? convertStringExpression(parameter) : parameter;
    }

    /**
     * Checks if the given node is of {@link ModelType#STRING} with a string value that includes expression syntax.
     * If so returns a node of {@link ModelType#EXPRESSION}, else simply returns {@code node} unchanged
     *
     * @param node the node to examine. Will not be {@code null}
     * @return the node with expressions converted, or the original node if no conversion was performed
     *         Cannot return {@code null}
     */
    protected static ModelNode convertStringExpression(ModelNode node) {
        if (node.getType() == ModelType.STRING) {
            return ParseUtils.parsePossibleExpression(node.asString());
        }
        return node;
    }

    private ModelNode validateOperation(final ModelNode operationObject, final boolean immutableValue) throws OperationFailedException {

        ModelNode node = new ModelNode();
        if(operationObject.has(name)) {
            node.set(operationObject.get(name));
        }

        if (!immutableValue) {
            node = convertParameterExpressions(node);
        }

        if (!node.isDefined() && defaultValue.isDefined()) {
            if (!immutableValue) correctValue(node, node);
            validator.validateParameter(name, defaultValue);
        } else {
            if (!immutableValue) correctValue(node, node);
            validator.validateParameter(name, node);
        }

        return node;
    }

    public AttributeMarshaller getAttributeMarshaller() {
        return attributeMarshaller;
    }

    /**
     * Show if attribute is resource only which means it wont be part of add operations but only present on resource
     * @return true is attribute is resource only
     */
    public boolean isResourceOnly() {
        return resourceOnly;
    }

    /**
     *
     * @return true if attribute is deprecated
     */
    public boolean isDeprecated() {
        return deprecationData != null;
    }

    /**
     * return deprecation data if there is any
     * @return {@link DeprecationData}
     */
    public DeprecationData getDeprecationData() {
        return deprecationData;
    }

    private static final ExpressionResolver NO_OPERATION_CONTEXT_FOR_RESOLVING_MODEL_PARAMETERS = new ExpressionResolver() {
        @Override
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return ExpressionResolver.DEFAULT.resolveExpressions(node);
        }
    };
}
