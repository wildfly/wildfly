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

package org.jboss.as.logging;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ResourceBundle;

/**
 * Date: 22.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PropertiesAttributeDefinition extends MapAttributeDefinition {
    private final AttributeDefinition childElement;
    private final AttributeDefinition keyAttribute;
    private final AttributeDefinition valueAttribute;

    public PropertiesAttributeDefinition(final String name, final String xmlName, final AttributeDefinition childElement, final AttributeDefinition keyAttribute, final AttributeDefinition valueAttribute, final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, minSize, maxSize, childElement.getValidator(), alternatives, requires, flags);
        this.childElement = childElement;
        this.keyAttribute = keyAttribute;
        this.valueAttribute = valueAttribute;
    }

    @Override
    protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
        //TODO jrp: - return proper value
    }

    @Override
    public void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            writer.writeStartElement(getXmlName());
            for (Property property : resourceModel.get(getName()).asPropertyList()) {
                writer.writeStartElement(childElement.getXmlName());
                writer.writeAttribute(keyAttribute.getXmlName(), property.getName());
                writer.writeAttribute(valueAttribute.getXmlName(), property.getValue().asString());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    public static class Builder {
        private final String name;
        private final AttributeDefinition childElement;
        private final AttributeDefinition keyAttribute;
        private final AttributeDefinition valueAttribute;
        private String xmlName;
        private boolean allowNull;
        private boolean allowExpression;
        private int minSize;
        private int maxSize;
        private String[] alternatives;
        private String[] requires;
        private AttributeAccess.Flag[] flags;

        public Builder(final String name, final AttributeDefinition childElement, final AttributeDefinition keyAttribute, final AttributeDefinition valueAttribute) {
            this.name = name;
            this.childElement = childElement;
            this.keyAttribute = keyAttribute;
            this.valueAttribute = valueAttribute;
        }

        public static Builder of(final String name, final AttributeDefinition parent, final AttributeDefinition key, final AttributeDefinition value) {
            return new Builder(name, parent, key, value);
        }

        public PropertiesAttributeDefinition build() {
            if (xmlName == null) xmlName = name;
            return new PropertiesAttributeDefinition(name, xmlName, childElement, keyAttribute, valueAttribute, allowNull, minSize, maxSize, alternatives, requires, flags);
        }

        public Builder setAllowExpression(final boolean allowExpression) {
            this.allowExpression = allowExpression;
            return this;
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
