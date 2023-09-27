/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class AttributeMarshallers {
    public static final AttributeMarshaller NAMED = new AttributeMarshaller() {

        @Override
        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                writer.writeStartElement(attribute.getName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), resourceModel.get(attribute.getName()).asString());
                writer.writeEndElement();
            }
        }
    };

    public static final AttributeMarshaller VALUE = new AttributeMarshaller() {

        @Override
        public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.hasDefined(attribute.getName())) {
                writer.writeStartElement(attribute.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), resourceModel.get(attribute.getName()).asString());
                writer.writeEndElement();
            }
        }
    };
}
