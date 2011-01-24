/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.model.ParseUtils.missingRequired;
import static org.jboss.as.model.ParseUtils.readProperty;
import static org.jboss.as.model.ParseUtils.readStringAttributeElement;
import static org.jboss.as.model.ParseUtils.unexpectedAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedElement;
import static org.jboss.as.remoting.CommonAttributes.ADD_CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.AUTHENTICATION_PROVIDER;
import static org.jboss.as.remoting.CommonAttributes.CONNECTOR;
import static org.jboss.as.remoting.CommonAttributes.FORWARD_SECRECY;
import static org.jboss.as.remoting.CommonAttributes.INCLUDE_MECHANISMS;
import static org.jboss.as.remoting.CommonAttributes.NO_ACTIVE;
import static org.jboss.as.remoting.CommonAttributes.NO_ANONYMOUS;
import static org.jboss.as.remoting.CommonAttributes.NO_DICTIONARY;
import static org.jboss.as.remoting.CommonAttributes.NO_PLAINTEXT;
import static org.jboss.as.remoting.CommonAttributes.PASS_CREDENTIALS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.PROPERTIES;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;
import static org.jboss.as.remoting.CommonAttributes.THREAD_POOL;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.as.model.Property;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.xnio.SaslQop;
import org.jboss.xnio.SaslStrength;

/**
 * @author Emanuel Muckenhuber
 */
public class NewRemotingExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "remoting";

    public void initialize(NewExtensionContext context) {
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME);

        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(NewRemotingSubsystemProviders.SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, NewRemotingSubsystemAdd.INSTANCE, NewRemotingSubsystemProviders.SUBSYSTEM_ADD, false);

        // Remoting connectors
        final ModelNodeRegistration connectors = subsystem.registerSubModel(PathElement.pathElement(CONNECTOR), NewRemotingSubsystemProviders.CONNECTOR_SPEC);
        connectors.registerOperationHandler(ADD, NewConnectorAdd.INSTANCE, NewRemotingSubsystemProviders.CONNECTOR_ADD, false);
        connectors.registerOperationHandler(REMOVE, NewConnectorRemove.INSTANCE, NewRemotingSubsystemProviders.CONNECTOR_REMOVE, false);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewRemotingSubsystemParser.INSTANCE, NewRemotingSubsystemParser.INSTANCE);
    }

    static final class NewRemotingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

        private static final NewRemotingSubsystemParser INSTANCE = new NewRemotingSubsystemParser();

        /** {@inheritDoc} */
        public void writeContent(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {

        }

        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();

            String threadPoolName = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case THREAD_POOL: {
                            threadPoolName = value;
                            break;
                        }
                        default:
                            throw unexpectedAttribute(reader, i);
                    }
                }
            }
            if (threadPoolName == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.THREAD_POOL));
            }

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            subsystem.get(THREAD_POOL).set(threadPoolName);
            list.add(subsystem);

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case REMOTING_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case CONNECTOR: {
                                // Add connector updates
                                parseConnector(reader, address, list);
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }

        }

        void parseConnector(XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

            String name = null;
            String socketBinding = null;
            final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.SOCKET_BINDING);
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            name = value;
                            break;
                        }
                        case SOCKET_BINDING: {
                            socketBinding = value;
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    required.remove(attribute);
                }
            }
            if (! required.isEmpty()) {
                throw ParseUtils.missingRequired(reader, required);
            }
            assert name != null;
            assert socketBinding != null;

            final ModelNode connector = new ModelNode();
            connector.get(OP).set(ADD_CONNECTOR);
            connector.get(OP_ADDR).set(address).add(CONNECTOR, name);
            // requestProperties.get(NAME).set(name); // Name is part of the address
            connector.get(SOCKET_BINDING).set(socketBinding);

            // Handle nested elements.
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case REMOTING_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (visited.contains(element)) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        visited.add(element);
                        switch (element) {
                            case SASL: {
                                connector.get(SASL).set(parseSaslElement(reader));
                                break;
                            }
                            case PROPERTIES: {
                                parseProperties(reader, connector.get(PROPERTIES));
                                break;
                            }
                            case AUTHENTICATION_PROVIDER: {
                                connector.get(AUTHENTICATION_PROVIDER).set(readStringAttributeElement(reader, "name"));
                                break;
                            }
                            default: {
                                throw unexpectedElement(reader);
                            }
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }

            list.add(connector);
        }

        ModelNode parseSaslElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode saslElement = new ModelNode();

            // No attributes
            final int count = reader.getAttributeCount();
            if (count > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }
            // Nested elements
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case REMOTING_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (visited.contains(element)) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        visited.add(element);
                        switch (element) {
                            case INCLUDE_MECHANISMS: {
                                final ModelNode includes = saslElement.get(INCLUDE_MECHANISMS);
                                for(final String s : ParseUtils.readArrayAttributeElement(reader, "value", String.class)) {
                                    includes.add().set(s);
                                }
                                break;
                            }
                            case POLICY: {
                                saslElement.get(POLICY).set(parsePolicyElement(reader));
                                break;
                            }
                            case PROPERTIES: {
                                parseProperties(reader, saslElement.get(PROPERTIES));
                                break;
                            }
                            case QOP: {
                                saslElement.get(QOP).set(ParseUtils.readArrayAttributeElement(reader, "value", SaslQop.class).toString());
                                break;
                            }
                            case REUSE_SESSION: {
                                saslElement.get(REUSE_SESSION).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case SERVER_AUTH: {
                                saslElement.get(SERVER_AUTH).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case STRENGTH: {
                                saslElement.get(STRENGTH).set(ParseUtils.readArrayAttributeElement(reader, "value", SaslStrength.class).toString());
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
            return saslElement;
        }

        ModelNode parsePolicyElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            final ModelNode policy = new ModelNode();
            if (reader.getAttributeCount() > 0) {
                throw ParseUtils.unexpectedAttribute(reader, 0);
            }
            // Handle nested elements.
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case REMOTING_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (visited.contains(element)) {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                        visited.add(element);
                        switch (element) {
                            case FORWARD_SECRECY: {
                                policy.get(FORWARD_SECRECY).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_ACTIVE: {
                                policy.get(NO_ACTIVE).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_ANONYMOUS: {
                                policy.set(NO_ANONYMOUS).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_DICTIONARY: {
                                policy.get(NO_DICTIONARY).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_PLAINTEXT: {
                                policy.get(NO_PLAINTEXT).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case PASS_CREDENTIALS: {
                                policy.get(PASS_CREDENTIALS).set(ParseUtils.readBooleanAttributeElement(reader, "value"));
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
            return policy;
        }

        void parseProperties(XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {
            while (reader.nextTag() != END_ELEMENT) {
                reader.require(START_ELEMENT, Namespace.CURRENT.getUriString(), Element.PROPERTY.getLocalName());
                final Property property = readProperty(reader);
                node.set(property.getName(), property.getValue());
            }
        }

    }
}
