/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2012, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.controller;

import static org.jboss.as.controller.parsing.Attribute.NAME;
import static org.jboss.as.controller.parsing.Element.PROPERTY;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * {@link MapAttributeDefinition} for maps with keys of {@link ModelType#STRING}.
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @since 7.2
 */
public class SimpleMapAttributeDefinition extends MapAttributeDefinition {
    /**
     *
     * @param name
     * @param xmlName
     * @param allowNull
     * @param expressionAllowed
     * @deprecated use {@link Builder}
     */
    @Deprecated
    public SimpleMapAttributeDefinition(final String name, final String xmlName, boolean allowNull, boolean expressionAllowed) {
        super(name, xmlName, allowNull, expressionAllowed, 0, Integer.MAX_VALUE, null, new ModelTypeValidator(ModelType.STRING, allowNull, expressionAllowed), null, null, null, false, null, AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    private SimpleMapAttributeDefinition(final String name, final String xmlName, final boolean allowNull, boolean allowExpression,
                                         final int minSize, final int maxSize, final ParameterCorrector corrector, final ParameterValidator elementValidator,
                                         final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller,final boolean resourceOnly,final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, allowExpression, minSize, maxSize, corrector, elementValidator, alternatives, requires, attributeMarshaller, resourceOnly,deprecated, flags);
    }

    @Override
    protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(isAllowExpression()));
    }

    @Override
    protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(isAllowExpression()));
    }

    @Override
    protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(new ModelNode(isAllowExpression()));
    }

    private static class MapAttributeMarshaller extends AttributeMarshaller {
        @Override
        public boolean isMarshallable(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault) {
            return resourceModel.isDefined() && resourceModel.hasDefined(attribute.getName());
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            resourceModel = resourceModel.get(attribute.getXmlName());
            writer.writeStartElement(attribute.getName());
            marshalToElement(resourceModel, writer);
            writer.writeEndElement();
        }

        private void marshalToElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            if (!resourceModel.isDefined()) { return; }
            for (Property property : resourceModel.asPropertyList()) {
                writer.writeStartElement(PROPERTY.getLocalName());
                writer.writeAttribute(NAME.getLocalName(), property.getName());
                writer.writeCharacters(property.getValue().asString());
                writer.writeEndElement();
            }
        }
    }

    public static final class Builder extends AbstractAttributeDefinitionBuilder<Builder, SimpleMapAttributeDefinition> {
        public Builder(final String name, boolean allowNull) {
            super(name, ModelType.OBJECT, allowNull);
        }

        public Builder(final SimpleMapAttributeDefinition basis) {
            super(basis);
        }

        public Builder(final PropertiesAttributeDefinition basis) {
            super(basis);
        }

        @Override
        public SimpleMapAttributeDefinition build() {
            if (validator == null) {
                validator = new ModelTypeValidator(ModelType.STRING, allowNull, allowExpression);
            }
            if (attributeMarshaller == null) {
                attributeMarshaller = new MapAttributeMarshaller();
            }
            return new SimpleMapAttributeDefinition(name, xmlName, allowNull, allowExpression, minSize, maxSize, corrector, validator, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        }
    }
}
