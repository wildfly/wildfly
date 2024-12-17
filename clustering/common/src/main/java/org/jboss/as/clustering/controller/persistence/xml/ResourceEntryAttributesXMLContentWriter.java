/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Writer of XML content for a resource entry.
 */
public class ResourceEntryAttributesXMLContentWriter implements XMLContentWriter<Property> {

    private final QName pathValueAttributeName;
    private final XMLContentWriter<ModelNode> attributesWriter;

    /**
     * Constructs a content writer that writes the path value attribute before writing attributes via the specified writer.
     * @param pathValueAttributeName the qualified name of the path value, of null if no path value attribute should be written.
     * @param attributesWriter a writer of an element's attributes
     */
    ResourceEntryAttributesXMLContentWriter(QName pathValueAttributeName, XMLContentWriter<ModelNode> attributesWriter) {
        this.pathValueAttributeName = pathValueAttributeName;
        this.attributesWriter = attributesWriter;
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, Property property) throws XMLStreamException {
        if (this.pathValueAttributeName != null) {
            String value = property.getName();
            String localName = this.pathValueAttributeName.getLocalPart();
            String namespaceURI = this.pathValueAttributeName.getNamespaceURI();
            if (namespaceURI != XMLConstants.NULL_NS_URI) {
                writer.writeAttribute(namespaceURI, localName, value);
            } else {
                // For PersistentResourceXMLDescription compatibility
                writer.writeAttribute(localName, value);
            }
        }
        this.attributesWriter.writeContent(writer, property.getValue());
    }

    @Override
    public boolean isEmpty(Property property) {
        return (this.pathValueAttributeName == null) && this.attributesWriter.isEmpty(property.getValue());
    }
}
