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
package org.jboss.as.osgi.parser;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import static org.jboss.as.osgi.parser.CommonAttributes.ACTIVATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION_PROPERTIES;
import static org.jboss.as.osgi.parser.CommonAttributes.MODULES;
import static org.jboss.as.osgi.parser.CommonAttributes.PID;
import static org.jboss.as.osgi.parser.CommonAttributes.PROPERTIES;
import static org.jboss.as.osgi.parser.CommonAttributes.STARTLEVEL;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the OSGi subsystem element handlers.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class OSGiExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "osgi";

    private static final OSGiSubsystemParser PARSER = new OSGiSubsystemParser();

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(OSGiSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, OSGiSubsystemAdd.INSTANCE, OSGiSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, OSGiSubsystemDescribeHandler.INSTANCE, OSGiSubsystemDescribeHandler.INSTANCE, false,
                OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), PARSER);
    }

    static class OSGiSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
            final ModelNode addSubsystemOp = new ModelNode();
            addSubsystemOp.get(OP).set(ADD);
            addSubsystemOp.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);

            // Handle attributes
            parseActivationAttribute(reader, addSubsystemOp);

            // Elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case CONFIGURATION: {
                                ModelNode configuration = parseConfigurationElement(reader);
                                addSubsystemOp.get(CONFIGURATION).set(configuration);
                                break;
                            }
                            case PROPERTIES: {
                                ModelNode properties = parsePropertiesElement(reader);
                                if (properties != null) {
                                    addSubsystemOp.get(PROPERTIES).set(properties);
                                }
                                break;
                            }
                            case MODULES: {
                                ModelNode modules = parseModulesElement(reader);
                                if (modules != null) {
                                    addSubsystemOp.get(MODULES).set(modules);
                                }
                                break;
                            }
                            default:
                                throw unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }

            operations.add(addSubsystemOp);
        }

        private void parseActivationAttribute(XMLExtendedStreamReader reader, ModelNode addOperation) throws XMLStreamException {

            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case OSGI_1_0: {
                    // Handle attributes
                    int count = reader.getAttributeCount();
                    for (int i = 0; i < count; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String attrValue = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case ACTIVATION: {
                                addOperation.get(ACTIVATION).set(attrValue);
                                break;
                            }
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        ModelNode parseConfigurationElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode configuration = new ModelNode();

            // Handle attributes
            String pid = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PID: {
                        pid = attrValue;
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }

            if (pid == null)
                throw missingRequired(reader, Collections.singleton(Attribute.PID));

            configuration.get(PID).set(pid);

            // Handle elements
            ModelNode configurationProperties = new ModelNode();
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (element == Element.PROPERTY) {
                            // Handle attributes
                            String name = null;
                            count = reader.getAttributeCount();
                            for (int i = 0; i < count; i++) {
                                requireNoNamespaceAttribute(reader, i);
                                final String attrValue = reader.getAttributeValue(i);

                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case NAME: {
                                        name = attrValue;
                                        if (configurationProperties.has(name))
                                            throw new XMLStreamException("Property " + name + " already exists", reader.getLocation());
                                        break;
                                    }
                                    default:
                                        throw unexpectedAttribute(reader, i);
                                }
                            }
                            if (name == null)
                                throw missingRequired(reader, Collections.singleton(Attribute.NAME));

                            String value = reader.getElementText().trim();
                            if (value == null || value.length() == 0)
                                throw new XMLStreamException("Value for property " + name + " is null", reader.getLocation());

                            configurationProperties.get(name).set(value);
                            break;
                        } else {
                            throw unexpectedElement(reader);
                        }
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }

            if (configurationProperties.asList().size() > 0)
                configuration.get(CONFIGURATION_PROPERTIES).set(configurationProperties);

            return configuration;
        }

        ModelNode parsePropertiesElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            // Handle attributes
            requireNoAttributes(reader);

            ModelNode properties = null;

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (element == Element.PROPERTY) {
                            if (properties == null)
                                properties = new ModelNode();

                            // Handle attributes
                            String name = null;
                            String value = null;
                            int count = reader.getAttributeCount();
                            for (int i = 0; i < count; i++) {
                                requireNoNamespaceAttribute(reader, i);
                                final String attrValue = reader.getAttributeValue(i);
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case NAME: {
                                        name = attrValue;
                                        if (properties.has(name)) {
                                            throw new XMLStreamException("Property " + name + " already exists", reader.getLocation());
                                        }
                                        break;
                                    }
                                    default:
                                        throw unexpectedAttribute(reader, i);
                                }
                            }
                            if (name == null) {
                                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
                            }
                            value = reader.getElementText().trim();
                            properties.get(name).set(value);
                            break;
                        } else {
                            throw unexpectedElement(reader);
                        }
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }

            return properties;
        }

        ModelNode parseModulesElement(XMLExtendedStreamReader reader) throws XMLStreamException {

            // Handle attributes
            requireNoAttributes(reader);

            ModelNode modules = null;

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case OSGI_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (element == Element.MODULE) {
                            if (modules == null)
                                modules = new ModelNode();
                            String identifier = null;
                            String start = null;
                            final int count = reader.getAttributeCount();
                            for (int i = 0; i < count; i++) {
                                requireNoNamespaceAttribute(reader, i);
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case IDENTIFIER: {
                                        identifier = reader.getAttributeValue(i);
                                        break;
                                    }
                                    case STARTLEVEL: {
                                        start = reader.getAttributeValue(i);
                                        break;
                                    }
                                    default:
                                        throw unexpectedAttribute(reader, i);
                                }
                            }
                            if (identifier == null)
                                throw missingRequired(reader, Collections.singleton(Attribute.IDENTIFIER));
                            if (modules.has(identifier))
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());

                            ModelNode module = new ModelNode();
                            if (start != null) {
                                module.get(STARTLEVEL).set(start);
                            }
                            modules.get(identifier).set(module);

                            requireNoContent(reader);
                        } else {
                            throw unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw unexpectedElement(reader);
                }
            }

            return modules;
        }

        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode node = context.getModelNode();

            if (has(node, ACTIVATION)) {
                writeAttribute(writer, Attribute.ACTIVATION, node.get(ACTIVATION));
            }

            if (has(node, CONFIGURATION)) {
                ModelNode configuration = node.get(CONFIGURATION);
                writer.writeStartElement(Element.CONFIGURATION.getLocalName());
                writeAttribute(writer, Attribute.PID, configuration.require(PID));
                if (has(configuration, CONFIGURATION_PROPERTIES)) {
                    ModelNode configurationProperties = configuration.get(CONFIGURATION_PROPERTIES);
                    Set<String> keys = configurationProperties.keys();
                    for (String current : keys) {
                        String value = configurationProperties.get(current).asString();
                        writer.writeStartElement(Element.PROPERTY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), current);
                        writer.writeCharacters(value);
                        writer.writeEndElement();
                    }
                }
                writer.writeEndElement();
            }

            if (has(node, PROPERTIES)) {
                ModelNode properties = node.get(PROPERTIES);
                writer.writeStartElement(Element.PROPERTIES.getLocalName());
                Set<String> keys = properties.keys();
                for (String current : keys) {
                    String value = properties.get(current).asString();
                    writer.writeStartElement(Element.PROPERTY.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), current);
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }

            if (has(node, MODULES)) {
                ModelNode modules = node.get(MODULES);
                writer.writeStartElement(Element.MODULES.getLocalName());
                Set<String> keys = modules.keys();
                for (String current : keys) {
                    ModelNode currentModule = modules.get(current);
                    writer.writeEmptyElement(Element.MODULE.getLocalName());
                    writer.writeAttribute(Attribute.IDENTIFIER.getLocalName(), current);
                    if (has(currentModule, STARTLEVEL)) {
                        writeAttribute(writer, Attribute.STARTLEVEL, currentModule.require(STARTLEVEL));
                    }
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }

    }

    private static class OSGiSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final OSGiSubsystemDescribeHandler INSTANCE = new OSGiSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(rootAddress.toModelNode());
            if (model.has(ACTIVATION)) {
                subsystem.get(ACTIVATION).set(model.get(ACTIVATION));
            }
            if (model.has(CONFIGURATION)) {
                subsystem.get(CONFIGURATION).set(model.get(CONFIGURATION));
            }
            if (model.has(PROPERTIES)) {
                subsystem.get(PROPERTIES).set(model.get(PROPERTIES));
            }
            if (model.has(MODULES)) {
                subsystem.get(MODULES).set(model.get(MODULES));
            }
            context.getResult().add(subsystem);
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
