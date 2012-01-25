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

import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Defining characteristics of an attribute in a {@link org.jboss.as.controller.registry.Resource}, with utility
 * methods for conversion to and from xml and for validation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AttributeDefinition {

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

    protected AttributeDefinition(String name, String xmlName, final ModelNode defaultValue, final ModelType type,
                               final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
                               final ParameterValidator validator, final String[] alternatives, final String[] requires,
                               final AttributeAccess.Flag... flags) {
        this(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                null, validator, alternatives, requires, flags);
    }

    protected AttributeDefinition(String name, String xmlName, final ModelNode defaultValue, final ModelType type,
            final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit,
            final ParameterCorrector valueCorrector, final ParameterValidator validator,
            final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {

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
        this.validator = validator;
        if (flags == null || flags.length == 0) {
            this.flags = EnumSet.noneOf(AttributeAccess.Flag.class);
        } else if (flags.length == 0) {
            this.flags = EnumSet.of(flags[0]);
        } else {
            this.flags = EnumSet.of(flags[0], flags);
        }
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
        return isMarshallable(resourceModel, true);
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
        return resourceModel.hasDefined(name) && (marshallDefault || !resourceModel.get(name).equals(defaultValue));
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

        ModelNode node = new ModelNode();
        if(operationObject.has(name)) {
            node.set(operationObject.get(name));
        }
        if (isAllowExpression() && node.getType() == ModelType.STRING) {
            node = ParseUtils.parsePossibleExpression(node.asString());
        }
        if (!node.isDefined() && defaultValue.isDefined()) {
            validator.validateParameter(name, defaultValue);
        } else {
            validator.validateParameter(name, node);
        }

        return node;
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
        if(this.valueCorrector != null) {
            valueCorrector.correct(operationObject.get(name), model.get(name));
        }
        ModelNode node = validateOperation(operationObject);
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
    public ModelNode resolveModelAttribute(OperationContext context, final ModelNode model) throws OperationFailedException {
        final ModelNode node = new ModelNode();
        if(model.has(name)) {
            node.set(model.get(name));
        }
        if (!node.isDefined() && defaultValue.isDefined()) {
            node.set(defaultValue);
        }
        final ModelNode resolved = context.resolveExpressions(node);
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
     * Marshalls the value from the given {@code resourceModel} as an xml element, if it
     * {@link #isMarshallable(org.jboss.dmr.ModelNode, boolean) is marshallable}.
     *
     * @param resourceModel the model, a non-null node of {@link org.jboss.dmr.ModelType#OBJECT}.
     * @param writer stream writer to use for writing the attribute
     * @throws javax.xml.stream.XMLStreamException if thrown by {@code writer}
     */
    public abstract void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException;

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
        return result;
    }

    public String getAttributeTextDescription(final ResourceBundle bundle, final String prefix) {
        final String bundleKey = prefix == null ? name : (prefix + "." + name);
        return bundle.getString(bundleKey);
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
        if (!forOperation && defaultValue != null && defaultValue.isDefined()) {
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

    private final OperationContext NO_OPERATION_CONTEXT_FOR_RESOLVING_MODEL_PARAMETERS = new OperationContext() {

        @Override
        public void setRollbackOnly() {
        }

        @Override
        public void runtimeUpdateSkipped() {
        }

        @Override
        public void revertRestartRequired() {
        }

        @Override
        public void revertReloadRequired() {
        }

        @Override
        public void restartRequired() {
        }

        @Override
        public void report(MessageSeverity severity, String message) {
        }

        @Override
        public void removeService(ServiceController<?> controller) throws UnsupportedOperationException {
        }

        @Override
        public ServiceController<?> removeService(ServiceName name) throws UnsupportedOperationException {
            return null;
        }

        @Override
        public Resource removeResource(PathAddress address) throws UnsupportedOperationException {
            return null;
        }

        @Override
        public void reloadRequired() {
        }

        @Override
        public Resource readResourceForUpdate(PathAddress address) {
            return null;
        }

        @Override
        public Resource readResource(PathAddress address) {
            return null;
        }

        @Override
        public ModelNode readModelForUpdate(PathAddress address) {
            return null;
        }

        @Override
        public ModelNode readModel(PathAddress address) {
            return null;
        }

        @Override
        public final boolean isNormalServer() {
            return false;
        }

        @Override
        public boolean isRuntimeAffected() {
            return false;
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
        }

        @Override
        public boolean isRollbackOnRuntimeFailure() {
            return false;
        }

        @Override
        public boolean isResourceServiceRestartAllowed() {
            return false;
        }

        @Override
        public boolean isResourceRegistryAffected() {
            return false;
        }

        @Override
        public boolean isModelAffected() {
            return false;
        }

        @Override
        public boolean isBooting() {
            return false;
        }

        @Override
        public boolean hasResult() {
            return false;
        }

        @Override
        public boolean hasFailureDescription() {
            return false;
        }

        @Override
        public ProcessType getProcessType() {
            return null;
        }

        @Override
        public RunningMode getRunningMode() {
            return null;
        }

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public ServiceTarget getServiceTarget() throws UnsupportedOperationException {
            return null;
        }

        @Override
        public ServiceRegistry getServiceRegistry(boolean modify) throws UnsupportedOperationException {
            return null;
        }

        @Override
        public Resource getRootResource() {
            return null;
        }

        @Override
        public ModelNode getResult() {
            return null;
        }

        @Override
        public ManagementResourceRegistration getResourceRegistrationForUpdate() {
            return null;
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration() {
            return null;
        }

        @Override
        public ModelNode getFailureDescription() {
            return null;
        }

        @Override
        public Stage getCurrentStage() {
            return null;
        }

        @Override
        public int getAttachmentStreamCount() {
            return 0;
        }

        @Override
        public InputStream getAttachmentStream(int index) {
            return null;
        }

        @Override
        public Resource createResource(PathAddress address) throws UnsupportedOperationException {
            return null;
        }

        @Override
        public void completeStep(RollbackHandler rollbackHandler) {
        }

        @Override
        public ResultAction completeStep() {
            return null;
        }

        @Override
        public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage)
                throws IllegalArgumentException {
        }

        @Override
        public void addStep(ModelNode operation, OperationStepHandler step, Stage stage) throws IllegalArgumentException {
        }

        @Override
        public void addStep(OperationStepHandler step, Stage stage) throws IllegalArgumentException {
        }

        @Override
        public void addResource(PathAddress address, Resource toAdd) {
        }

        @Override
        public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            //
        }

        @Override
        public void addStep(OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
            //
        }

        @Override
        public void acquireControllerLock() {
        }

        @Override
        public ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
            return ExpressionResolver.DEFAULT.resolveExpressions(node);
        }

        @Override
        public <T> T getAttachment(final AttachmentKey<T> key) {
            return null;
        }

        @Override
        public <T> T attach(final AttachmentKey<T> key, final T value) {
            return null;
        }

        @Override
        public <T> T attachIfAbsent(final AttachmentKey<T> key, final T value) {
            return null;
        }

        @Override
        public <T> T detach(final AttachmentKey<T> key) {
            return null;
        }

        @Override
        public Resource getOriginalRootResource() {
            return null;
        }

        @Override
        public boolean markResourceRestarted(PathAddress resource, Object owner) {
            return false;
        }

        @Override
        public boolean revertResourceRestarted(PathAddress resource, Object owner) {
            return false;
        }
    };
}
