/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.persistence.xml;

import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.xml.XMLCardinality;
import org.jboss.as.clustering.controller.xml.XMLContentReader;
import org.jboss.as.clustering.controller.xml.XMLContentWriter;
import org.jboss.as.clustering.controller.xml.XMLElement;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.common.Assert;

/**
 * An XML element for a resource attribute.
 */
public class ResourceAttributeXMLElement extends XMLElement.DefaultXMLElement<Map.Entry<PathAddress, Map<PathAddress, ModelNode>>, ModelNode> {

    /**
     * Returns an XML element for a resource attribute that should parse and/or marshal as an element, using the specified parser and marshaller.
     * @param name the name of the element
     * @param attribute the attribute to read/write
     * @param parser an attribute parser
     * @param marshaller an attribute marshaller
     */
    ResourceAttributeXMLElement(QName name, AttributeDefinition attribute, AttributeParser parser, AttributeMarshaller marshaller) {
        super(name, new ResourceOperationXMLContentReader(new XMLContentReader<>() {
            @Override
            public void readElement(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
                if (parser.isParseAsElement()) {
                    parser.parseElement(attribute, reader, operation);
                } else {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

            @Override
            public XMLCardinality getCardinality() {
                // TODO Replace with AttributeParser.getCardinality(AttributeDefinition)
                return Set.of(ModelType.OBJECT, ModelType.LIST).contains(attribute.getType()) ? (attribute.isNillable() ? XMLCardinality.Unbounded.OPTIONAL : XMLCardinality.Unbounded.REQUIRED) : (attribute.isNillable() ? XMLCardinality.Single.OPTIONAL : XMLCardinality.Single.REQUIRED);
            }
        }), new XMLContentWriter<>() {
            @Override
            public void writeContent(XMLExtendedStreamWriter writer, ModelNode model) throws XMLStreamException {
                if (marshaller.isMarshallableAsElement()) {
                    marshaller.marshallAsElement(attribute, model, true, writer);
                }
            }

            @Override
            public boolean isEmpty(ModelNode model) {
                return !marshaller.isMarshallableAsElement() || !model.hasDefined(attribute.getName());
            }
        });
        Assert.assertTrue(parser.isParseAsElement() || marshaller.isMarshallableAsElement());
    }
}
