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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_REF;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * XML marshallers for messaging custom attributes.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public final class AttributeMarshallers {

    public static final AttributeMarshaller DISCOVERY_GROUP_MARSHALLER = new AttributeInsideElementMarshaller(DISCOVERY_GROUP_REF, DISCOVERY_GROUP_NAME);

    private static final class AttributeInsideElementMarshaller extends AttributeMarshaller {
        private final String elementName;
        private final String attributeName;

        public AttributeInsideElementMarshaller(final String elementName, final String attributeName) {
            this.elementName = elementName;
            this.attributeName = attributeName;
        }

        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, javax.xml.stream.XMLStreamWriter writer) throws javax.xml.stream.XMLStreamException {
            if (isMarshallable(attribute, resourceModel)) {
                writer.writeStartElement(elementName);
                writer.writeAttribute(attributeName, resourceModel.get(attributeName).asString());
                writer.writeEndElement();
            }
        }
    }

    public static final AttributeMarshaller NOOP_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            // do nothing
        };

        public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            // do nothing
        };
    };

    public static final AttributeMarshaller CONNECTORS_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(Element.CONNECTOR.getLocalName())) {
                writer.writeStartElement(Element.CONNECTORS.getLocalName());
                for (Property connProp : resourceModel.get(Element.CONNECTOR.getLocalName()).asPropertyList()) {
                    writer.writeStartElement(Element.CONNECTOR_REF.getLocalName());
                    writer.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), connProp.getName());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        };
    };

    public static final AttributeMarshaller SELECTOR_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                String selector = resourceModel.get(attribute.getName()).asString();
                writer.writeEmptyElement(Element.SELECTOR.getLocalName());
                writer.writeAttribute(Attribute.STRING.getLocalName(), selector);
            }
        }
    };

    public static final AttributeMarshaller JNDI_CONTEXT_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                ModelNode context = resourceModel.get(attribute.getName());

                writer.writeStartElement(attribute.getXmlName());
                for (Property property : context.asPropertyList()) {
                    writer.writeStartElement(Element.PROPERTY.getLocalName());
                    writer.writeAttribute(Attribute.KEY.getLocalName(), property.getName());
                    writer.writeAttribute(Attribute.VALUE.getLocalName(), property.getValue().asString());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    };

    public static final AttributeMarshaller JNDI_RESOURCE_MARSHALLER = new AttributeMarshaller() {
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                String name = resourceModel.get(attribute.getName()).asString();
                writer.writeEmptyElement(attribute.getXmlName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), name);
            }
        }
    };


    public static final AttributeMarshaller INTERCEPTOR_MARSHALLER = new AttributeMarshaller() {
        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                if (list.size() > 0) {
                    writer.writeStartElement(attribute.getXmlName());

                    for (ModelNode child : list) {
                        writer.writeStartElement(Element.CLASS_NAME.getLocalName());
                        writer.writeCharacters(child.asString());
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }
            }
        }
    };

    /**
     * XML marshaller for connector attribute to wrap a list of attributes in an optional XML element.
     */
    public static final class WrappedListAttributeMarshaller extends AttributeMarshaller {

        private final String wrappingElementName;

        /**
         * @param wrappingElementName @null if the list of connector must not be wrapper
         *         or the name of the XML element that wraps the list.
         */
        public WrappedListAttributeMarshaller(final String wrappingElementName) {
            this.wrappingElementName = wrappingElementName;
        }

        @Override
        public void marshallAsElement(final AttributeDefinition attribute,
                final ModelNode resourceModel,
                final boolean marshallDefault,
                final XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                if (list.size() > 0) {

                    if (wrappingElementName != null) {
                        writer.writeStartElement(wrappingElementName);
                    }

                    for (ModelNode child : list) {
                        writer.writeStartElement(attribute.getXmlName());
                        writer.writeCharacters(child.asString());
                        writer.writeEndElement();
                    }

                    if (wrappingElementName != null) {
                        writer.writeEndElement();
                    }
                }
            }
        }
    }

    public static final class JndiEntriesAttributeMarshaller extends AttributeMarshaller {

        private final boolean forDestination;

        public JndiEntriesAttributeMarshaller(final boolean forDestination) {
            this.forDestination = forDestination;
        }

        public void marshallAsElement(final AttributeDefinition attribute,
                final ModelNode resourceModel,
                final boolean marshallDefault,
                final XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                List<ModelNode> list = resourceModel.get(attribute.getName()).asList();
                if (list.size() > 0) {
                    // This is a bit of a hack, using allowNull to distinguish the connection factory case
                    // from the jms destination case
                    if (!forDestination) {
                        writer.writeStartElement(attribute.getXmlName());
                    }

                    for (ModelNode child : list) {
                        writer.writeEmptyElement(Element.ENTRY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), child.asString());
                    }

                    if (!forDestination) {
                        writer.writeEndElement();
                    }
                }
            }
        }
    }
}
