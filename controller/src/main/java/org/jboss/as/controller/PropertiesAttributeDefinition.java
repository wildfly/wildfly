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

package org.jboss.as.controller;

import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Represents simple key=value map equivalent of java.util.Map<String,String>()
 *
 * @author Jason T. Greene
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
//todo maybe replace with SimpleMapAttributeDefinition?
public final class PropertiesAttributeDefinition extends MapAttributeDefinition {

    private PropertiesAttributeDefinition(final String name, final String xmlName, final boolean allowNull, boolean allowExpression,
                                          final int minSize, final int maxSize, final ParameterCorrector corrector, final ParameterValidator elementValidator,
                                          final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly,
                                          final DeprecationData deprecated, final AccessConstraintDefinition[] accessConstraints,
                                          final Boolean nullSignificant, final AttributeParser parser,  final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, allowExpression, minSize, maxSize, corrector, elementValidator, alternatives, requires, attributeMarshaller,
                resourceOnly, deprecated, accessConstraints, nullSignificant, parser, flags);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        setValueType(node);
    }

    void setValueType(ModelNode node) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        if (isAllowExpression()) {
            node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(true));
        }
    }

    public Map<String, String> unwrap(final OperationContext context, final ModelNode model) throws OperationFailedException {
        if (!model.hasDefined(getName())) {
            return new HashMap<String, String>();
        }
        ModelNode modelProps = model.get(getName());
        Map<String, String> props = new HashMap<String, String>();
        for (Property p : modelProps.asPropertyList()) {
            props.put(p.getName(), context.resolveExpressions(p.getValue()).asString());
        }
        return props;
    }

    public void parse(final XMLExtendedStreamReader reader,final ModelNode operation) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (reader.getLocalName().equals(getXmlName())) {
                final String[] array = requireAttributes(reader, org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName());
                parseAndAddParameterElement(array[0], array[1], operation, reader);
                ParseUtils.requireNoContent(reader);
            } else {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private static class PropertiesAttributeMarshaller extends AttributeMarshaller {
        private final boolean wrapElement;
        private final String wrapperElement;

        protected PropertiesAttributeMarshaller(final boolean wrapElement, String wrapperElement) {
            this.wrapElement = wrapElement;
            this.wrapperElement = wrapperElement;
        }

        @Override
        public boolean isMarshallable(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault) {
            return resourceModel.isDefined() && resourceModel.hasDefined(attribute.getName());
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            resourceModel = resourceModel.get(attribute.getName());
            if (wrapElement) {
                writer.writeStartElement(wrapperElement == null ? attribute.getName() : wrapperElement);
            }
            for (ModelNode property : resourceModel.asList()) {
                writer.writeEmptyElement(attribute.getXmlName());
                writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.NAME.getLocalName(), property.asProperty().getName());
                writer.writeAttribute(org.jboss.as.controller.parsing.Attribute.VALUE.getLocalName(), property.asProperty().getValue().asString());
            }
            if (wrapElement) {
                writer.writeEndElement();
            }
        }

        @Override
        public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            marshallAsElement(attribute, resourceModel, marshallDefault, writer);
        }

        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, PropertiesAttributeDefinition> {
        private boolean wrapXmlElement = true;
        private String wrapperElement = null;

        public Builder(final String name, boolean allowNull) {
            super(name, ModelType.OBJECT, allowNull);
        }

        public Builder(final PropertiesAttributeDefinition basis) {
            super(basis);
        }

        public Builder(final MapAttributeDefinition basis) {
            super(basis);
        }

        public Builder setWrapXmlElement(boolean wrap) {
            this.wrapXmlElement = wrap;
            return this;
        }

        public Builder setWrapperElement(String name) {
            this.wrapperElement = name;
            return this;
        }

        @Override
        public PropertiesAttributeDefinition build() {
            if (validator == null) {
                validator = new ModelTypeValidator(ModelType.STRING, allowNull, allowExpression);
            }
            if (attributeMarshaller == null) {
                attributeMarshaller = new PropertiesAttributeMarshaller(wrapXmlElement, wrapperElement);
            }
            return new PropertiesAttributeDefinition(name, xmlName, allowNull, allowExpression, minSize, maxSize, corrector, validator, alternatives,
                    requires, attributeMarshaller, resourceOnly, deprecated, accessConstraints, nullSignficant, parser, flags);
        }
    }
}
