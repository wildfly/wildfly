/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.ParametersOfValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 */
public class KeyManagerAttributeDefinition extends AttributeDefinition {
    private static final ParameterValidator keyManagerValidator;
    private static final ParameterValidator fieldValidator;
    private static final String[] FIELDS = { Constants.ALGORITHM, Constants.PROVIDER };

    static {
        final ParametersValidator delegate = new ParametersValidator();
        for (String field : FIELDS) {
            delegate.registerValidator(field, new ModelTypeValidator(ModelType.STRING, true, true));
        }
        keyManagerValidator = new ParametersOfValidator(delegate);
        fieldValidator = delegate;
    }

    protected KeyManagerAttributeDefinition(String name) {
        super(name, null, null, ModelType.OBJECT, true, false, null, keyManagerValidator, null, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    public void marshallAsAttribute(ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(resourceModel, marshallDefault)) {
            resourceModel = resourceModel.get(getName());

            if (resourceModel.hasDefined(Constants.ALGORITHM))
                writer.writeAttribute(getName() + "-factory-" + Constants.ALGORITHM, resourceModel.get(Constants.ALGORITHM).asString());
            if (resourceModel.hasDefined(Constants.PROVIDER))
                writer.writeAttribute(getName() + "-factory-" + Constants.PROVIDER, resourceModel.get(Constants.PROVIDER).asString());
        }
    }

   public static ModelNode parseField(String name, String value, XMLStreamReader reader) throws XMLStreamException {
        final String trimmed = value == null ? null : value.trim();
        ModelNode node;
        if (trimmed != null ) {
            node = new ModelNode().set(ParseUtils.parsePossibleExpression(trimmed));
        } else {
            node = new ModelNode();
        }

        try {
            fieldValidator.validateParameter(name, node);
        } catch (OperationFailedException e) {
            throw SecurityMessages.MESSAGES.xmlStreamException(e.getFailureDescription().toString(), reader.getLocation());
        }
        return node;
    }

    @Override
    public ModelNode validateOperation(ModelNode operationObject) throws OperationFailedException {
        ModelNode validateOp = operationObject;
        if (operationObject.hasDefined(getName())) {
            // Convert any expression strings into ModelType.EXPRESSION
            validateOp = operationObject.clone();
            ModelNode attr = validateOp.get(getName());
            for (String field : FIELDS) {
                ModelNode fieldNode = attr.get(field);
                if (fieldNode.getType() == ModelType.STRING) {
                    fieldNode.set(ParseUtils.parsePossibleExpression(fieldNode.asString()));
                }
            }
        }
        return super.validateOperation(validateOp);
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

    private void addAttributeValueTypeDescription(ModelNode result, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(result    );
        valueType.get(Constants.ALGORITHM, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.ALGORITHM));
        valueType.get(Constants.PROVIDER, DESCRIPTION).set(resolver.getResourceAttributeValueTypeDescription(getName(), locale, bundle, Constants.PROVIDER));
    }

    private void addOperationParameterValueTypeDescription(ModelNode result, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        final ModelNode valueType = getNoTextValueTypeDescription(result);
        valueType.get(Constants.ALGORITHM, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.ALGORITHM));
        valueType.get(Constants.PROVIDER, DESCRIPTION).set(resolver.getOperationParameterValueTypeDescription(operationName, getName(), locale, bundle, Constants.PROVIDER));
    }

    private ModelNode getNoTextValueTypeDescription(final ModelNode parent) {
        final ModelNode valueType = parent.get(VALUE_TYPE);
        final ModelNode password = valueType.get(Constants.ALGORITHM);
        password.get(DESCRIPTION); // placeholder
        password.get(TYPE).set(ModelType.STRING);
        password.get(NILLABLE).set(true);
        password.get(EXPRESSIONS_ALLOWED).set(true);

        final ModelNode provider = valueType.get(Constants.PROVIDER);
        provider.get(DESCRIPTION);  // placeholder
        provider.get(TYPE).set(ModelType.STRING);
        provider.get(NILLABLE).set(true);
        provider.get(EXPRESSIONS_ALLOWED).set(true);

        return valueType;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
       throw SecurityMessages.MESSAGES.unsupportedOperationExceptionUseResourceDesc();
    }

    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
       throw SecurityMessages.MESSAGES.unsupportedOperationExceptionUseResourceDesc();
    }

    @Override
    public void marshallAsElement(ModelNode resourceModel,final boolean marshalDefault, XMLStreamWriter writer) throws XMLStreamException {
        throw SecurityMessages.MESSAGES.unsupportedOperation();
    }
}
