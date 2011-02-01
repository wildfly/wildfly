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
package org.jboss.as.webservices;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.webservices.CommonAttributes.CONFIGURATION;
import static org.jboss.as.webservices.CommonAttributes.MODIFY_SOAP_ADDRESS;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_HOST;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_PORT;
import static org.jboss.as.webservices.CommonAttributes.WEBSERVICE_SECURE_PORT;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NewWSExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "webservices";

    private static final WebservicesSubsystemParser PARSER = new WebservicesSubsystemParser();

    @Override
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(WSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewWSSubsystemAdd.INSTANCE, WSSubsystemProviders.SUBSYSTEM_ADD, false);
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    static class WebservicesSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
            XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            writer.writeStartElement(Element.CONFIGURATION.getLocalName());

            ModelNode node = context.getModelNode();
            ModelNode configuration = node.require(CONFIGURATION);
            writeElement(writer, Element.WEBSERVICE_HOST, configuration.require(WEBSERVICE_HOST));
            writeElement(writer, Element.MODIFY_SOAP_ADDRESS, configuration.require(MODIFY_SOAP_ADDRESS));
            if (has(node, WEBSERVICE_SECURE_PORT)) {
                writeElement(writer, Element.WEBSERVICE_SECURE_PORT, configuration.require(WEBSERVICE_SECURE_PORT));
            }
            if (has(node, WEBSERVICE_PORT)) {
                writeElement(writer, Element.WEBSERVICE_PORT, configuration.require(WEBSERVICE_PORT));
            }

            writer.writeEndElement(); // End configuration element.
            writer.writeEndElement(); // End of subsystem element

        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeElement(final XMLExtendedStreamWriter writer, final Element element, final ModelNode value)
                throws XMLStreamException {
            writer.writeStartElement(element.getLocalName());
            writer.writeCharacters(value.asString());
            writer.writeEndElement();
        }

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

            // elements
            final EnumSet<Element> required = EnumSet.of(Element.CONFIGURATION);
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case WEBSERVICES_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        required.remove(element);
                        if (!encountered.add(element)) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        switch (element) {
                            case CONFIGURATION: {
                                final ModelNode model = parseConfigurationElement(reader);
                                subsystem.get(CONFIGURATION).set(model);
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }

            if (!required.isEmpty()) {
                throw ParseUtils.missingRequiredElement(reader, required);
            }

            list.add(subsystem);
        }

        private ModelNode parseConfigurationElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode configuration = new ModelNode();
            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }

            // elements
            final EnumSet<Element> required = EnumSet.of(Element.MODIFY_SOAP_ADDRESS, Element.WEBSERVICE_HOST);
            final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case WEBSERVICES_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        required.remove(element);
                        if (!encountered.add(element)) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        switch (element) {
                            case WEBSERVICE_HOST: {
                                configuration.get(WEBSERVICE_HOST).set(parseElementNoAttributes(reader));
                                break;
                            }
                            case MODIFY_SOAP_ADDRESS: {
                                configuration.get(MODIFY_SOAP_ADDRESS).set(parseElementNoAttributes(reader));
                                break;
                            }
                            case WEBSERVICE_SECURE_PORT: {
                                configuration.get(WEBSERVICE_SECURE_PORT).set(parseElementNoAttributes(reader));
                                break;
                            }
                            case WEBSERVICE_PORT: {
                                configuration.get(WEBSERVICE_PORT).set(parseElementNoAttributes(reader));
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }

            if (!required.isEmpty()) {
                throw ParseUtils.missingRequiredElement(reader, required);
            }

            return configuration;
        }

        private String parseElementNoAttributes(XMLExtendedStreamReader reader) throws XMLStreamException {
            // no attributes
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }

            return reader.getElementText().trim();
        }
    }

}
