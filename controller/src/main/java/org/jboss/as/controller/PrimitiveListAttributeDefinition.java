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

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 */
public class PrimitiveListAttributeDefinition extends ListAttributeDefinition {
    private final ModelType valueType;

    public PrimitiveListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final ModelType valueType, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, minSize, maxSize, new ModelTypeValidator(valueType), alternatives, requires, flags);
        this.valueType = valueType;
    }

    public PrimitiveListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final ModelType valueType, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, ParameterValidator elementValidator, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, minSize, maxSize, elementValidator, alternatives, requires, flags);
        this.valueType = valueType;
    }

    public PrimitiveListAttributeDefinition(String name, String xmlName, boolean allowNull, final ModelType valueType) {
        super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new ModelTypeValidator(valueType));
        this.valueType = valueType;
    }

    public PrimitiveListAttributeDefinition(String name, String xmlName, boolean allowNull, final ModelType valueType, ParameterValidator elementValidator) {
        super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, elementValidator);
        this.valueType = valueType;
    }

    public PrimitiveListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final ModelType valueType, final int minSize, final int maxSize, ParameterValidator elementValidator) {
        super(name, xmlName, allowNull, minSize, maxSize, elementValidator);
        this.valueType = valueType;
    }

    public PrimitiveListAttributeDefinition(final String name, final boolean allowNull, final ModelType valueType, ParameterValidator elementValidator, final AttributeAccess.Flag... flags) {
        super(name, allowNull, elementValidator, flags);
        this.valueType = valueType;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
        final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
        addValueTypeDescription(result);
        return result;
    }

    @Override
    protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
        addValueTypeDescription(node);
    }


    protected void addValueTypeDescription(final ModelNode node) {
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(valueType);
    }

    @Override
    protected void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        addValueTypeDescription(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(final ModelNode node, final String operationName, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        addValueTypeDescription(node);
    }

    @Override
    public void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            writer.writeStartElement(getXmlName());
            for (ModelNode handler : resourceModel.get(getName()).asList()) {
                writer.writeStartElement("element");
                writer.writeCharacters(handler.asString());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }


    public static class Builder {
        private final String name;
        private final ModelType valueType;
        private String xmlName;
        private boolean allowNull;
        private int minSize;
        private int maxSize;
        private String[] alternatives;
        private String[] requires;
        private AttributeAccess.Flag[] flags;

        public Builder(final String name, final ModelType valueType) {
            this.name = name;
            this.valueType = valueType;
        }

        public static Builder of(final String name, final ModelType valueType) {
            return new Builder(name, valueType);
        }

        public PrimitiveListAttributeDefinition build() {
            if (xmlName == null) { xmlName = name; }
            if (maxSize < 1) { maxSize = Integer.MAX_VALUE; }
            return new PrimitiveListAttributeDefinition(name, xmlName, allowNull, valueType, minSize, maxSize, alternatives, requires, flags);
        }

        public Builder setAllowNull(final boolean allowNull) {
            this.allowNull = allowNull;
            return this;
        }

        public Builder setAlternates(final String... alternates) {
            this.alternatives = alternates;
            return this;
        }

        public Builder setFlags(final AttributeAccess.Flag... flags) {
            this.flags = flags;
            return this;
        }

        public Builder setMaxSize(final int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder setMinSize(final int minSize) {
            this.minSize = minSize;
            return this;
        }

        public Builder setRequires(final String... requires) {
            this.requires = requires;
            return this;
        }

        public Builder setXmlName(final String xmlName) {
            this.xmlName = xmlName;
            return this;
        }
    }
}
