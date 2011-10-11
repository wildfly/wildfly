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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readArrayAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.readBooleanAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.readProperty;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.xnio.sasl.SaslQop;
import org.xnio.sasl.SaslStrength;

/**
 * The implementation of the Remoting extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class RemotingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "remoting";

    //This is given by ServerEnvironment.NODE_NAME
    private static final String NODE_NAME = "jboss.node.name";

    @Override
    public void initialize(ExtensionContext context) {

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME);
        registration.registerXMLElementWriter(NewRemotingSubsystemParser.INSTANCE);
        // Remoting subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(RemotingSubsystemProviders.SUBSYSTEM);
        subsystem.registerOperationHandler(ADD, new RemotingSubsystemAdd(SecurityActions.getSystemProperty(NODE_NAME)), RemotingSubsystemProviders.SUBSYSTEM_ADD, false);
        subsystem.registerOperationHandler(DESCRIBE, RemotingSubsystemDescribeHandler.INSTANCE, RemotingSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // Remoting connectors
        final ManagementResourceRegistration connectors = subsystem.registerSubModel(PathElement.pathElement(CONNECTOR), RemotingSubsystemProviders.CONNECTOR_SPEC);
        connectors.registerOperationHandler(ADD, ConnectorAdd.INSTANCE, ConnectorAdd.INSTANCE, false);
        connectors.registerOperationHandler(REMOVE, ConnectorRemove.INSTANCE, ConnectorRemove.INSTANCE, false);

        connectors.registerSubModel(PathElement.pathElement(SASL), RemotingSubsystemProviders.SASL_SPEC);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewRemotingSubsystemParser.INSTANCE);
    }

    /**
     * The root element parser for the Remoting subsystem.
     */
    static final class NewRemotingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        private static final NewRemotingSubsystemParser INSTANCE = new NewRemotingSubsystemParser();

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case THREAD_POOL: {
                        // TODO this unused attribute is allowed in the file solely so the TCK
                        // config would not fail right before CR1
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }

            final ModelNode subsystem = Util.getEmptyOperation(ADD, address);
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
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
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
                        throw unexpectedAttribute(reader, i);
                }
            }
            if (! required.isEmpty()) {
                throw missingRequired(reader, required);
            }
            assert name != null;
            assert socketBinding != null;

            final ModelNode connector = new ModelNode();
            connector.get(OP).set(ADD);
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
                            throw unexpectedElement(reader);
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
                throw unexpectedAttribute(reader, 0);
            }
            // Nested elements
            final EnumSet<Element> visited = EnumSet.noneOf(Element.class);
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case REMOTING_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        if (visited.contains(element)) {
                            throw unexpectedElement(reader);
                        }
                        visited.add(element);
                        switch (element) {
                            case INCLUDE_MECHANISMS: {
                                final ModelNode includes = saslElement.get(INCLUDE_MECHANISMS);
                                for(final String s : readArrayAttributeElement(reader, "value", String.class)) {
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
                                //FIXME is this really an attribute?
                                saslElement.get(QOP).set(readArrayAttributeElement(reader, "value", SaslQop.class).toString());
                                break;
                            }
                            case REUSE_SESSION: {
                                saslElement.get(REUSE_SESSION).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case SERVER_AUTH: {
                                saslElement.get(SERVER_AUTH).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case STRENGTH: {
                                //FIXME is this really an xml attribute?
                                saslElement.get(STRENGTH).set(readArrayAttributeElement(reader, "value", SaslStrength.class).toString());
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
                                policy.get(FORWARD_SECRECY).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_ACTIVE: {
                                policy.get(NO_ACTIVE).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_ANONYMOUS: {
                                policy.set(NO_ANONYMOUS).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_DICTIONARY: {
                                policy.get(NO_DICTIONARY).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_PLAINTEXT: {
                                policy.get(NO_PLAINTEXT).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case PASS_CREDENTIALS: {
                                policy.get(PASS_CREDENTIALS).set(readBooleanAttributeElement(reader, "value"));
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
            return policy;
        }

        void parseProperties(XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {
            while (reader.nextTag() != END_ELEMENT) {
                reader.require(START_ELEMENT, Namespace.CURRENT.getUriString(), Element.PROPERTY.getLocalName());
                final Property property = readProperty(reader);
                node.set(property);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            final ModelNode node = context.getModelNode();

            if (node.hasDefined(CONNECTOR)) {
                final ModelNode connector = node.get(CONNECTOR);
                for (String name : connector.keys()) {
                    if (connector.hasDefined(name)) {
                        writeConnector(writer, connector.get(name), name);
                    }
                }
            }
            writer.writeEndElement();

        }

        private void writeConnector(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
            writer.writeStartElement(Element.CONNECTOR.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), name);
            if (node.hasDefined(SOCKET_BINDING)) {
                writeAttribute(writer, Attribute.SOCKET_BINDING, node.get(SOCKET_BINDING));
            }
            if (node.hasDefined(AUTHENTICATION_PROVIDER)) {
                writeSimpleChild(writer, Element.AUTHENTICATION_PROVIDER, Attribute.NAME, node.get(AUTHENTICATION_PROVIDER));
            }
            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }
            if (node.hasDefined(SASL)) {
                writeSasl(writer, node.get(SASL));
            }
            writer.writeEndElement();
        }

        private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());
            for (String name : node.keys()) {
                if (node.hasDefined(name)) {
                    final ModelNode prop = node.get(name);
                    if (prop.getType() == ModelType.PROPERTY) {
                        writer.writeStartElement(Element.PROPERTY.getLocalName());
                        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                        writeAttribute(writer, Attribute.VALUE, prop.asProperty().getValue());
                        writer.writeEndElement();
                    }
                }
            }
            writer.writeEndElement();
        }

        private void writeSasl(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.SASL.getLocalName());
            if (node.hasDefined(INCLUDE_MECHANISMS)) {

            }
            if (node.hasDefined(QOP)) {
                //FIXME is this really an xml attribute?
                writeSimpleChild(writer, Element.QOP, Attribute.VALUE, node.get(QOP));
            }
            if (node.hasDefined(STRENGTH)) {
                //FIXME is this really an xml attribute?
                writeSimpleChild(writer, Element.STRENGTH, Attribute.VALUE, node.get(STRENGTH));
            }
            if (node.hasDefined(REUSE_SESSION)) {
                writeSimpleChild(writer, Element.REUSE_SESSION, Attribute.VALUE, node.get(REUSE_SESSION));
            }
            if (node.hasDefined(SERVER_AUTH)) {
                writeSimpleChild(writer, Element.SERVER_AUTH, Attribute.VALUE, node.get(SERVER_AUTH));
            }
            if (node.hasDefined(POLICY)) {
                writePolicy(writer, node.get(POLICY));
            }
            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }

            writer.writeEndElement();
        }

        private void writePolicy(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.POLICY.getLocalName());

            if (node.hasDefined(FORWARD_SECRECY)) {
                writeSimpleChild(writer, Element.FORWARD_SECRECY, Attribute.VALUE, node.get(FORWARD_SECRECY));
            }
            if (node.hasDefined(NO_ACTIVE)) {
                writeSimpleChild(writer, Element.NO_ACTIVE, Attribute.VALUE, node.get(NO_ACTIVE));
            }
            if (node.hasDefined(NO_ANONYMOUS)) {
                writeSimpleChild(writer, Element.NO_ANONYMOUS, Attribute.VALUE, node.get(NO_ANONYMOUS));
            }
            if (node.hasDefined(NO_DICTIONARY)) {
                writeSimpleChild(writer, Element.NO_DICTIONARY, Attribute.VALUE, node.get(NO_DICTIONARY));
            }
            if (node.hasDefined(NO_PLAINTEXT)) {
                writeSimpleChild(writer, Element.NO_PLAINTEXT, Attribute.VALUE, node.get(NO_PLAINTEXT));
            }
            if (node.hasDefined(PASS_CREDENTIALS)) {
                writeSimpleChild(writer, Element.PASS_CREDENTIALS, Attribute.VALUE, node.get(PASS_CREDENTIALS));
            }

            writer.writeEndElement();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }

        private void writeSimpleChild(final XMLExtendedStreamWriter writer, final Element element, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeStartElement(element.getLocalName());
            writeAttribute(writer, attr, value);
            writer.writeEndElement();
        }

    }

    private static class RemotingSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final RemotingSubsystemDescribeHandler INSTANCE = new RemotingSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            final ModelNode result = context.getResult();
            final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME));
            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address.toModelNode());
            subsystem.get(THREAD_POOL).set(model.get(THREAD_POOL));

            result.add(subsystem);

            if (model.hasDefined(CONNECTOR)) {
                for (org.jboss.dmr.Property prop : model.get(CONNECTOR).asPropertyList()) {
                    final ModelNode connector = prop.getValue();
                    final ModelNode add = Util.getEmptyOperation(ADD, address.append(PathElement.pathElement(CONNECTOR, prop.getName())).toModelNode());
                    if (connector.hasDefined(SOCKET_BINDING)) {
                        add.get(SOCKET_BINDING).set(connector.get(SOCKET_BINDING));
                    }
                    if (connector.hasDefined(AUTHENTICATION_PROVIDER)) {
                        add.get(AUTHENTICATION_PROVIDER).set(AUTHENTICATION_PROVIDER);
                    }
                    if (connector.hasDefined(SASL)) {
                        add.get(SASL);
                    }
                    result.add(add);
                }
            }
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }
}
