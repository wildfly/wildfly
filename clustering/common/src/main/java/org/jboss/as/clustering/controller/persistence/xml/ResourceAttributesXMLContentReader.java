/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContentReader;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Reads XML content into a resource operation.
 */
public class ResourceAttributesXMLContentReader implements XMLContentReader<ModelNode> {

    private final Map<QName, Map.Entry<AttributeDefinition, AttributeParser>> attributes;

    ResourceAttributesXMLContentReader(Map<QName, AttributeDefinition> attributes, Function<AttributeDefinition, AttributeParser> parsers) {
        this.attributes = attributes.isEmpty() ? Map.of() : new HashMap<>();
        // Collect only those attributes that will parse as an XML attribute
        for (Map.Entry<QName, AttributeDefinition> entry : attributes.entrySet()) {
            AttributeDefinition attribute = entry.getValue();
            AttributeParser parser = parsers.apply(attribute);
            if (!parser.isParseAsElement()) {
                this.attributes.put(entry.getKey(), Map.entry(attribute, parser));
            }
        }
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String localName = reader.getAttributeLocalName(i);
            QName name = new QName(reader.getNamespaceURI(), localName);
            Map.Entry<AttributeDefinition, AttributeParser> entry = this.attributes.get(name);
            if (entry == null) {
                // Try matching w/out namespace (for PersistentResourceXMLDescription compatibility)
                entry = this.attributes.get(new QName(localName));
                if (entry == null) {
                    throw ParseUtils.unexpectedAttribute(reader, i, this.attributes.keySet().stream().map(QName::getLocalPart).collect(Collectors.toSet()));
                }
            }
            AttributeDefinition attribute = entry.getKey();
            AttributeParser parser = entry.getValue();
            parser.parseAndSetParameter(attribute, reader.getAttributeValue(i), operation, reader);
        }
    }

    @Override
    public XMLCardinality getCardinality() {
        return this.attributes.values().stream().<AttributeDefinition>map(Map.Entry::getKey).noneMatch(AttributeDefinition::isRequired) ? XMLCardinality.Single.OPTIONAL : XMLCardinality.Single.REQUIRED;
    }
}
