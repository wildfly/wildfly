/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Collections;
/**
 * @author <a href="mailto:prpaul@redhat.com">Prarthona Paul</a>
 */

public class ElytronOidcClientSubsystemRegistrar {

    static final String NAME = "elytron-oidc-client";
    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(NAME);
    static final AttributeParser SIMPLE_ATTRIBUTE_PARSER = new AttributeElementParser();
    static final AttributeMarshaller SIMPLE_ATTRIBUTE_MARSHALLER = new AttributeElementMarshaller();

     static class AttributeElementMarshaller extends AttributeMarshaller.AttributeElementMarshaller {
        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            writer.writeStartElement(attribute.getXmlName());
            marshallElementContent(resourceModel.get(attribute.getName()).asString(), writer);
            writer.writeEndElement();
        }
    }

    static class AttributeElementParser extends AttributeParser {

        @Override
        public boolean isParseAsElement() {
            return true;
        }

        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof SimpleAttributeDefinition;
            if (operation.hasDefined(attribute.getName())) {
                throw ParseUtils.unexpectedElement(reader);
            } else if (attribute.getXmlName().equals(reader.getLocalName())) {
                ((SimpleAttributeDefinition) attribute).parseAndSetParameter(reader.getElementText(), operation, reader);
            } else {
                throw ParseUtils.unexpectedElement(reader, Collections.singleton(attribute.getXmlName()));
            }
        }
    }
}
