/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContent;
import org.jboss.as.clustering.controller.xml.XMLContentReader;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A readable/writable XML element of a resource.
 * @author Paul Ferraro
 */
public class ResourceOperationXMLElement<C> extends XMLElement.AbstractXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, C> {

    private final QName name;
    private final XMLContentReader<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>> attributesReader;
    private final XMLContentWriter<C> attributesWriter;
    private final Function<C, ModelNode> model;
    private final XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> childContent;

    ResourceOperationXMLElement(QName name, XMLContentReader<ModelNode> attributesReader, XMLContentWriter<C> attributesWriter, Function<C, ModelNode> model, XMLContent<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> childContent) {
        this.name = name;
        this.attributesReader = new ResourceOperationXMLContentReader(attributesReader);
        this.attributesWriter = attributesWriter;
        this.model = model;
        this.childContent = childContent;
    }

    @Override
    public QName getName() {
        return this.name;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, Map.Entry<PathAddress, Map<PathAddress, ModelNode>> context) throws XMLStreamException {
        // Validate entry criteria
        // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
        if (!reader.isStartElement() || (!reader.getName().equals(this.name) && !reader.getLocalName().equals(this.name.getLocalPart()))) {
            throw ParseUtils.unexpectedElement(reader, Set.of(this.name.toString()));
        }
        this.attributesReader.readElement(reader, context);
        this.childContent.readElement(reader, context);
        // Validate exit criteria
        // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
        if (!reader.isEndElement() || (!reader.getName().equals(this.name) && !reader.getLocalName().equals(this.name.getLocalPart()))) {
            throw ParseUtils.unexpectedElement(reader);
        }
    }

    @Override
    public XMLCardinality getCardinality() {
        return this.attributesReader.getCardinality();
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, C content) throws XMLStreamException {
        String namespaceURI = this.name.getNamespaceURI();
        // If namespace is not yet bound to any prefix, bind it
        if (writer.getNamespaceContext().getPrefix(namespaceURI) == null) {
            writer.writeStartElement(this.name.getLocalPart());
            // Bind and write namespace
            if (namespaceURI != XMLConstants.NULL_NS_URI) { // For PersistentResourceXMLDescription compatibility
                writer.setPrefix(this.name.getPrefix(), namespaceURI);
                writer.writeNamespace(this.name.getPrefix(), namespaceURI);
            }
        } else {
            writer.writeStartElement(namespaceURI, this.name.getLocalPart());
        }
        this.attributesWriter.writeContent(writer, content);

        this.childContent.writeContent(writer, this.model.apply(content));

        writer.writeEndElement();
    }

    @Override
    public boolean isEmpty(C content) {
        return this.attributesWriter.isEmpty(content) && this.childContent.isEmpty(this.model.apply(content));
    }
}
