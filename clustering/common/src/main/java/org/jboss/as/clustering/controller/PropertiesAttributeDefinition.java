/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.parsing.Element;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * {@link MapAttributeDefinition} for maps with keys and values of type {@link ModelType#STRING}.
 *
 * @author Paul Ferraro
 */
public class PropertiesAttributeDefinition extends MapAttributeDefinition {
    private final Consumer<ModelNode> descriptionContributor;

    PropertiesAttributeDefinition(Builder builder) {
        super(builder);
        this.descriptionContributor = node -> {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(this.isAllowExpression()));
        };
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        this.descriptionContributor.accept(node);
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        this.descriptionContributor.accept(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        this.descriptionContributor.accept(node);
    }

    static final AttributeMarshaller MARSHALLER = new AttributeMarshaller() {
        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode model, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (model.hasDefined(attribute.getName())) {
                for (Property property : model.get(attribute.getName()).asPropertyList()) {
                    writer.writeStartElement(Element.PROPERTY.getLocalName());
                    writer.writeAttribute(Element.NAME.getLocalName(), property.getName());
                    writer.writeCharacters(property.getValue().asString());
                    writer.writeEndElement();
                }
            }
        }
    };

    static final AttributeParser PARSER = new AttributeParser() {
        @Override
        public boolean isParseAsElement() {
            return true;
        }

        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof MapAttributeDefinition;
            String name = reader.getAttributeValue(null, ModelDescriptionConstants.NAME);
            ((MapAttributeDefinition) attribute).parseAndAddParameterElement(name, reader.getElementText(), operation, reader);
        }
    };

    public static class Builder extends MapAttributeDefinition.Builder<Builder, PropertiesAttributeDefinition> {

        public Builder(String name) {
            super(name);
            setRequired(false);
            this.setAllowNullElement(false);
            setAttributeMarshaller(MARSHALLER);
            setAttributeParser(PARSER);
        }

        public Builder(PropertiesAttributeDefinition basis) {
            super(basis);
        }

        @Override
        public PropertiesAttributeDefinition build() {
            if (this.elementValidator == null) {
                this.elementValidator = new ModelTypeValidator(ModelType.STRING, this.isAllowNull(), this.isAllowExpression());
            }
            return new PropertiesAttributeDefinition(this);
        }
    }
}
