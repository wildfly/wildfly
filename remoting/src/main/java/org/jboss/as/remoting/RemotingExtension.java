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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readArrayAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.readBooleanAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.readProperty;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
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
import static org.jboss.as.remoting.CommonAttributes.NO_PLAIN_TEXT;
import static org.jboss.as.remoting.CommonAttributes.PASS_CREDENTIALS;
import static org.jboss.as.remoting.CommonAttributes.POLICY;
import static org.jboss.as.remoting.CommonAttributes.PROPERTY;
import static org.jboss.as.remoting.CommonAttributes.QOP;
import static org.jboss.as.remoting.CommonAttributes.REUSE_SESSION;
import static org.jboss.as.remoting.CommonAttributes.SASL;
import static org.jboss.as.remoting.CommonAttributes.SASL_POLICY;
import static org.jboss.as.remoting.CommonAttributes.SECURITY;
import static org.jboss.as.remoting.CommonAttributes.SERVER_AUTH;
import static org.jboss.as.remoting.CommonAttributes.SOCKET_BINDING;
import static org.jboss.as.remoting.CommonAttributes.STRENGTH;
import static org.jboss.as.remoting.CommonAttributes.VALUE;
import static org.jboss.as.remoting.RemotingMessages.MESSAGES;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
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

    private static final String RESOURCE_NAME = RemotingExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final String NODE_NAME_PROPERTY = "jboss.node.name";
    static final String NODE_NAME;
    static {
        NODE_NAME = SecurityActions.getSystemProperty(RemotingExtension.NODE_NAME_PROPERTY);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, RemotingExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(ExtensionContext context) {

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        registration.registerXMLElementWriter(RemotingSubsystem11Parser.INSTANCE);

        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(new RemotingSubsystemRootResource(context.getProcessType()));
        subsystem.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ManagementResourceRegistration connector = subsystem.registerSubModel(ConnectorResource.INSTANCE);
        connector.registerSubModel(PropertyResource.INSTANCE_CONNECTOR);
        final ManagementResourceRegistration sasl = connector.registerSubModel(SaslResource.INSTANCE);
        sasl.registerSubModel(SaslPolicyResource.INSTANCE);
        sasl.registerSubModel(PropertyResource.INSTANCE_CONNECTOR);

        // remote outbound connection
        subsystem.registerSubModel(RemoteOutboundConnnectionResourceDefinition.INSTANCE);
        // local outbound connection
        subsystem.registerSubModel(LocalOutboundConnectionResourceDefinition.INSTANCE);
        // (generic) outbound connection
        subsystem.registerSubModel(GenericOutboundConnectionResourceDefinition.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.REMOTING_1_0.getUriString(), RemotingSubsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.REMOTING_1_1.getUriString(), RemotingSubsystem11Parser.INSTANCE);
    }

    /**
     * The root element parser for the Remoting subsystem.
     */
    static final class RemotingSubsystem10Parser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        private static final RemotingSubsystem10Parser INSTANCE = new RemotingSubsystem10Parser();

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();
            final ModelNode subsystem = Util.getEmptyOperation(ADD, address);
            list.add(subsystem);

            requireNoAttributes(reader);

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                boolean doneWorkerThreadPool = false;
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case REMOTING_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case WORKER_THREAD_POOL:
                                if (doneWorkerThreadPool) {
                                    throw duplicateNamedElement(reader, Element.WORKER_THREAD_POOL.getLocalName());
                                }
                                doneWorkerThreadPool = true;
                                parseWorkerThreadPool(reader, subsystem);
                                break;
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


        /**
         * Adds the worker thread pool attributes to the subysystem add method
         */
        void parseWorkerThreadPool(final XMLExtendedStreamReader reader, final ModelNode subsystemAdd) throws XMLStreamException {
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case WORKER_READ_THREADS:
                        if (subsystemAdd.hasDefined(CommonAttributes.WORKER_READ_THREADS)) {
                            throw duplicateAttribute(reader, CommonAttributes.WORKER_READ_THREADS);
                        }
                        RemotingSubsystemRootResource.WORKER_READ_THREADS.parseAndSetParameter(value, subsystemAdd, reader);
                        break;
                    case WORKER_TASK_CORE_THREADS:
                        if (subsystemAdd.hasDefined(CommonAttributes.WORKER_TASK_CORE_THREADS)) {
                            throw duplicateAttribute(reader, CommonAttributes.WORKER_TASK_CORE_THREADS);
                        }
                        RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS.parseAndSetParameter(value, subsystemAdd, reader);
                        break;
                    case WORKER_TASK_KEEPALIVE:
                        if (subsystemAdd.hasDefined(CommonAttributes.WORKER_TASK_KEEPALIVE)) {
                            throw duplicateAttribute(reader, CommonAttributes.WORKER_TASK_KEEPALIVE);
                        }
                        RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE.parseAndSetParameter(value, subsystemAdd, reader);
                        break;
                    case WORKER_TASK_LIMIT:
                        if (subsystemAdd.hasDefined(CommonAttributes.WORKER_TASK_LIMIT)) {
                            throw duplicateAttribute(reader, CommonAttributes.WORKER_TASK_LIMIT);
                        }
                        RemotingSubsystemRootResource.WORKER_TASK_LIMIT.parseAndSetParameter(value, subsystemAdd, reader);
                        break;
                    case WORKER_TASK_MAX_THREADS:
                        if (subsystemAdd.hasDefined(CommonAttributes.WORKER_TASK_MAX_THREADS)) {
                            throw duplicateAttribute(reader, CommonAttributes.WORKER_TASK_MAX_THREADS);
                        }
                        RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS.parseAndSetParameter(value, subsystemAdd, reader);
                        break;
                    case WORKER_WRITE_THREADS:
                        if (subsystemAdd.hasDefined(CommonAttributes.WORKER_WRITE_THREADS)) {
                            throw duplicateAttribute(reader, CommonAttributes.WORKER_WRITE_THREADS);
                        }
                        RemotingSubsystemRootResource.WORKER_WRITE_THREADS.parseAndSetParameter(value, subsystemAdd, reader);
                        break;
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            requireNoContent(reader);
        }

        void parseConnector(XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

            String name = null;
            String socketBinding = null;
            final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.SOCKET_BINDING);
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
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
            if (!required.isEmpty()) {
                throw missingRequired(reader, required);
            }
            assert name != null;
            assert socketBinding != null;

            final ModelNode connector = new ModelNode();
            connector.get(OP).set(ADD);
            connector.get(OP_ADDR).set(address).add(CONNECTOR, name);
            // requestProperties.get(NAME).set(name); // Name is part of the address
            connector.get(SOCKET_BINDING).set(socketBinding);
            list.add(connector);

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
                                parseSaslElement(reader, connector.get(OP_ADDR), list);
                                break;
                            }
                            case PROPERTIES: {
                                parseProperties(reader, connector.get(OP_ADDR), list);
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
        }

        void parseSaslElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
            final ModelNode saslElement = new ModelNode();
            saslElement.get(OP).set(ADD);
            saslElement.get(OP_ADDR).set(address).add(SaslResource.SASL_CONFIG_PATH.getKey(), SaslResource.SASL_CONFIG_PATH.getValue());
            list.add(saslElement);

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
                                for (final String s : readArrayAttributeElement(reader, "value", String.class)) {
                                    includes.add().set(s);
                                }
                                break;
                            }
                            case POLICY: {
                                parsePolicyElement(reader, saslElement.get(OP_ADDR), list);
                                break;
                            }
                            case PROPERTIES: {
                                parseProperties(reader, saslElement.get(OP_ADDR), list);
                                break;
                            }
                            case QOP: {
                                String[] qop = readArrayAttributeElement(reader, "value", String.class);
                                for (String q : qop) {
                                    try {
                                        saslElement.get(QOP).add(SaslQop.fromString(q).getString().toLowerCase());
                                    } catch (IllegalArgumentException e) {
                                        throw MESSAGES.invalidQOPV(q);
                                    }
                                }
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
                                String[] strength = readArrayAttributeElement(reader, "value", String.class);
                                for (String s : strength) {
                                    try {
                                        saslElement.get(STRENGTH).add(SaslStrength.valueOf(s.toUpperCase()).name().toLowerCase());
                                    } catch (IllegalArgumentException e) {
                                        throw MESSAGES.invalidStrength(s);
                                    }
                                }
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

        ModelNode parsePolicyElement(XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
            final ModelNode policy = new ModelNode();
            policy.get(OP).set(ADD);
            policy.get(OP_ADDR).set(address).add(SaslPolicyResource.SASL_POLICY_CONFIG_PATH.getKey(), SaslPolicyResource.SASL_POLICY_CONFIG_PATH.getValue());
            list.add(policy);

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
                                policy.get(NO_ANONYMOUS).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_DICTIONARY: {
                                policy.get(NO_DICTIONARY).set(readBooleanAttributeElement(reader, "value"));
                                break;
                            }
                            case NO_PLAIN_TEXT: {
                                policy.get(NO_PLAIN_TEXT).set(readBooleanAttributeElement(reader, "value"));
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

        private void parseProperties(XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
            while (reader.nextTag() != END_ELEMENT) {
                reader.require(START_ELEMENT, Namespace.REMOTING_1_0.getUriString(), Element.PROPERTY.getLocalName());
                final Property property = readProperty(reader);
                ModelNode propertyOp = new ModelNode();
                propertyOp.get(OP).set(ADD);
                propertyOp.get(OP_ADDR).set(address).add(PROPERTY, property.getName());
                propertyOp.get(VALUE).set(property.getValue());
                list.add(propertyOp);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            final ModelNode model = context.getModelNode();

            writeWorkerThreadPoolIfAttributesSet(writer, model);

            if (model.hasDefined(CONNECTOR)) {
                final ModelNode connector = model.get(CONNECTOR);
                for (String name : connector.keys()) {
                    writeConnector(writer, connector.require(name), name);
                }
            }

            writer.writeEndElement();
        }

        private void writeWorkerThreadPoolIfAttributesSet(final XMLExtendedStreamWriter writer, final ModelNode model) throws XMLStreamException {
            if (model.hasDefined(CommonAttributes.WORKER_READ_THREADS) || model.hasDefined(CommonAttributes.WORKER_TASK_CORE_THREADS) || model.hasDefined(CommonAttributes.WORKER_TASK_KEEPALIVE) ||
                    model.hasDefined(CommonAttributes.WORKER_TASK_LIMIT) || model.hasDefined(CommonAttributes.WORKER_TASK_MAX_THREADS) || model.hasDefined(CommonAttributes.WORKER_WRITE_THREADS)) {

                    writer.writeStartElement(Element.WORKER_THREAD_POOL.getLocalName());

                    RemotingSubsystemRootResource.WORKER_READ_THREADS.marshallAsAttribute(model, false, writer);
                    RemotingSubsystemRootResource.WORKER_TASK_CORE_THREADS.marshallAsAttribute(model, false, writer);
                    RemotingSubsystemRootResource.WORKER_TASK_KEEPALIVE.marshallAsAttribute(model, false, writer);
                    RemotingSubsystemRootResource.WORKER_TASK_LIMIT.marshallAsAttribute(model, false, writer);
                    RemotingSubsystemRootResource.WORKER_TASK_MAX_THREADS.marshallAsAttribute(model, false, writer);
                    RemotingSubsystemRootResource.WORKER_WRITE_THREADS.marshallAsAttribute(model, false, writer);

                    writer.writeEndElement();
                }

        }

        private void writeConnector(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
            writer.writeStartElement(Element.CONNECTOR.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), name);

            ConnectorResource.SOCKET_BINDING.marshallAsAttribute(node, writer);
            ConnectorResource.AUTHENTICATION_PROVIDER.marshallAsElement(node, writer);

            if (node.hasDefined(PROPERTY)) {
                writeProperties(writer, node.get(PROPERTY));
            }
            if (node.hasDefined(SECURITY) && node.get(SECURITY).hasDefined(SASL)) {
                writeSasl(writer, node.get(SECURITY, SASL));
            }
            writer.writeEndElement();
        }

        private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());
            for (Property prop : node.asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                PropertyResource.VALUE.marshallAsAttribute(prop.getValue(), writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        private void writeSasl(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.SASL.getLocalName());
            SaslResource.INCLUDE_MECHANISMS_ATTRIBUTE.marshallAsElement(node, writer);
            SaslResource.QOP_ATTRIBUTE.marshallAsElement(node, writer);
            SaslResource.STRENGTH_ATTRIBUTE.marshallAsElement(node, writer);
            SaslResource.SERVER_AUTH_ATTRIBUTE.marshallAsElement(node, writer);
            SaslResource.REUSE_SESSION_ATTRIBUTE.marshallAsElement(node, writer);

            if (node.hasDefined(SASL_POLICY)) {
                writePolicy(writer, node.get(SASL_POLICY));
            }
            if (node.hasDefined(PROPERTY)) {
                writeProperties(writer, node.get(PROPERTY));
            }

            writer.writeEndElement();
        }

        private void writePolicy(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.POLICY.getLocalName());
            final ModelNode policy = node.get(POLICY);
            SaslPolicyResource.FORWARD_SECRECY.marshallAsElement(policy, writer);
            SaslPolicyResource.NO_ACTIVE.marshallAsElement(policy, writer);
            SaslPolicyResource.NO_ANONYMOUS.marshallAsElement(policy, writer);
            SaslPolicyResource.NO_DICTIONARY.marshallAsElement(policy, writer);
            SaslPolicyResource.NO_PLAIN_TEXT.marshallAsElement(policy, writer);
            SaslPolicyResource.PASS_CREDENTIALS.marshallAsElement(policy, writer);
            writer.writeEndElement();
        }
    }
}
