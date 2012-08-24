package org.jboss.as.controller;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class DefaultAttributeMarshaller extends AttributeMarshaller {


    public void marshallAsAttribute(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(attribute, resourceModel, marshallDefault)) {
            writer.writeAttribute(attribute.getXmlName(), resourceModel.get(attribute.getName()).asString());
        }
    }

    public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
        marshallAsElement(attribute, resourceModel, true, writer);
    }

    @Override
    public void marshallAsElement(final AttributeDefinition attribute, final ModelNode resourceModel, final boolean marshallDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (isMarshallable(attribute, resourceModel, marshallDefault)) {
            writer.writeStartElement(attribute.getXmlName());
            String content = resourceModel.get(attribute.getName()).asString();
            if (content.indexOf('\n') > -1) {
                // Multiline content. Use the overloaded variant that staxmapper will format
                writer.writeCharacters(content);
            } else {
                // Staxmapper will just output the chars without adding newlines if this is used
                char[] chars = content.toCharArray();
                writer.writeCharacters(chars, 0, chars.length);
            }
            writer.writeEndElement();
        }
    }
}
