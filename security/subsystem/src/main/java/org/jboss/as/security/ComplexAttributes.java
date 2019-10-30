/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.security;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jason T. Greene
 * @author Tomaz Cerar
 */
public class ComplexAttributes {

    static final SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(Constants.PASSWORD, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(Constants.TYPE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition URL = new SimpleAttributeDefinitionBuilder(Constants.URL, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition PROVIDER = new SimpleAttributeDefinitionBuilder(Constants.PROVIDER, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition PROVIDER_ARGUMENT = new SimpleAttributeDefinitionBuilder(Constants.PROVIDER_ARGUMENT, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition ALGORITHM = new SimpleAttributeDefinitionBuilder(Constants.ALGORITHM, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition[] KEY_STORE_FIELDS = {PASSWORD, TYPE, URL, PROVIDER, PROVIDER_ARGUMENT};

    static final SimpleAttributeDefinition[] KEY_MANAGER_FIELDS = {ALGORITHM, PROVIDER};

    protected static final class KeyStoreAttributeMarshaller extends DefaultAttributeMarshaller {

        @Override
        public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (attribute.isMarshallable(resourceModel, marshallDefault)) {
                resourceModel = resourceModel.get(attribute.getName());
                if (resourceModel.hasDefined(Constants.PASSWORD)) {
                    writer.writeAttribute(attribute.getName() + "-" + Constants.PASSWORD, resourceModel.get(Constants.PASSWORD).asString());
                }
                if (resourceModel.hasDefined(Constants.TYPE)) {
                    writer.writeAttribute(attribute.getName() + "-" + Constants.TYPE, resourceModel.get(Constants.TYPE).asString());
                }
                if (resourceModel.hasDefined(Constants.URL)) {
                    writer.writeAttribute(attribute.getName() + "-" + Constants.URL, resourceModel.get(Constants.URL).asString());
                }
                if (resourceModel.hasDefined(Constants.PROVIDER)) {
                    writer.writeAttribute(attribute.getName() + "-" + Constants.PROVIDER, resourceModel.get(Constants.PROVIDER).asString());
                }
                if (resourceModel.hasDefined(Constants.PROVIDER_ARGUMENT)) {
                    writer.writeAttribute(attribute.getName() + "-" + Constants.PROVIDER_ARGUMENT, resourceModel.get(Constants.PROVIDER_ARGUMENT).asString());
                }
            }
        }

    }

    protected static final class KeyManagerAttributeMarshaller extends DefaultAttributeMarshaller {

        @Override
        public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (attribute.isMarshallable(resourceModel, marshallDefault)) {
                resourceModel = resourceModel.get(attribute.getName());

                if (resourceModel.hasDefined(Constants.ALGORITHM)) {
                    writer.writeAttribute(attribute.getName() + "-factory-" + Constants.ALGORITHM, resourceModel.get(Constants.ALGORITHM).asString());
                }
                if (resourceModel.hasDefined(Constants.PROVIDER)) {
                    writer.writeAttribute(attribute.getName() + "-factory-" + Constants.PROVIDER, resourceModel.get(Constants.PROVIDER).asString());
                }
            }

        }
    }

    protected static final class KeyStoreAttributeValidator implements ParameterValidator {

        private String name;

        public KeyStoreAttributeValidator(String name) {
            this.name = name;
        }

        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if (name.equals(parameterName)) {
                ModelNode parameters = value.clone();
                if (isConfigured(parameters)) {
                    for (SimpleAttributeDefinition attribute : KEY_STORE_FIELDS) {
                        attribute.getValidator().validateParameter(attribute.getName(), parameters.get(attribute.getName()));
                    }
                }
            }
        }

        @Override
        public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if (name.equals(parameterName)) {
                ModelNode parameters = value.clone();
                if (isConfigured(parameters)) {
                    for (SimpleAttributeDefinition attribute : KEY_STORE_FIELDS) {
                        attribute.getValidator().validateParameter(attribute.getName(), parameters.get(attribute.getName()));
                    }
                }
            }
        }


        private boolean isConfigured(ModelNode keystore) {
            return keystore.hasDefined(Constants.TYPE) || keystore.hasDefined(Constants.URL) ||
                    keystore.hasDefined(Constants.PROVIDER) || keystore.hasDefined(Constants.PROVIDER_ARGUMENT);
        }
    }
}
