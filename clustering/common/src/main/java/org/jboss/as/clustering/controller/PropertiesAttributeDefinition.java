/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * {@link MapAttributeDefinition} for maps with keys and values of type {@link ModelType#STRING}.
 *
 * @author Paul Ferraro
 */
public class PropertiesAttributeDefinition extends MapAttributeDefinition implements ResourceModelResolver<Map<String, String>> {
    private final Consumer<ModelNode> descriptionContributor;

    PropertiesAttributeDefinition(Builder builder) {
        super(builder);
        this.descriptionContributor = node -> {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
            node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(this.isAllowExpression()));
        };
    }

    @Override
    public Map<String, String> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        List<Property> properties = this.resolveModelAttribute(context, model).asPropertyListOrEmpty();
        if (properties.isEmpty()) return Map.of();
        Map<String, String> result = new TreeMap<>();
        for (Property property : properties) {
            result.put(property.getName(), property.getValue().asString());
        }
        return result;
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
                    AttributeMarshaller.marshallElementContent(property.getValue().asString(), writer);
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
        public XMLCardinality getCardinality(AttributeDefinition attribute) {
            return attribute.isRequired() ? XMLCardinality.Unbounded.REQUIRED : XMLCardinality.Unbounded.OPTIONAL;
        }

        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof MapAttributeDefinition;
            String name = reader.getAttributeValue(null, ModelDescriptionConstants.NAME);
            ((MapAttributeDefinition) attribute).parseAndAddParameterElement(name, reader.getElementText(), operation, reader);
        }
    };

    public static class Builder extends MapAttributeDefinition.Builder<Builder, PropertiesAttributeDefinition> {

        public Builder() {
            this(ModelDescriptionConstants.PROPERTIES);
        }

        public Builder(String name) {
            super(name);
            this.setXmlName(Element.PROPERTY.getLocalName());
            this.setRequired(false);
            this.setAllowExpression(true);
            this.setAllowNullElement(false);
            this.setAttributeMarshaller(MARSHALLER);
            this.setAttributeParser(PARSER);
            this.setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        }

        public Builder(String name, PropertiesAttributeDefinition basis) {
            super(name, basis);
        }

        @Override
        public PropertiesAttributeDefinition build() {
            if (this.elementValidator == null) {
                this.elementValidator = new ModelTypeValidator(ModelType.STRING, this.isNillable(), this.isAllowExpression());
            }
            return new PropertiesAttributeDefinition(this);
        }
    }
}
