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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Tristan Tarrant
 */
public class JGroupsSubsystemXMLReader_1_1 implements XMLElementReader<List<ModelNode>> {
    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode subsystemAddress = new ModelNode();
        subsystemAddress.add(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);
        subsystemAddress.protect();
        ModelNode subsystem = Util.getEmptyOperation(ADD, subsystemAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_STACK: {
                    CommonAttributes.DEFAULT_STACK.parseAndSetParameter(value, subsystem, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!subsystem.hasDefined(ModelKeys.DEFAULT_STACK)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.DEFAULT_STACK));
        }

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JGROUPS_1_1: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case STACK: {
                            this.parseStack(reader, subsystemAddress, operations);
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
    }

    private void parseStack(XMLExtendedStreamReader reader, ModelNode subsystemAddress, List<ModelNode> operations) throws XMLStreamException {

        final ModelNode stack = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        ModelNode stackAddress = subsystemAddress.clone();
        stackAddress.add(ModelKeys.STACK, name);
        stackAddress.protect();
        stack.get(OP_ADDR).set(stackAddress);

        if (!reader.hasNext() || (reader.nextTag() == XMLStreamConstants.END_ELEMENT) || Element.forName(reader.getLocalName()) != Element.TRANSPORT) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.TRANSPORT));
        }

        this.parseTransport(reader, stackAddress, additionalConfigurationOperations);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROTOCOL: {
                    this.parseProtocol(reader, stackAddress, additionalConfigurationOperations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        operations.add(stack);
        // add operations to create configuration resources
        for (ModelNode additionalOperation : additionalConfigurationOperations) {
            operations.add(additionalOperation);
        }
    }

    private void parseTransport(XMLExtendedStreamReader reader, ModelNode stackAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode transportAddress = stackAddress.clone();
        transportAddress.add(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
        transportAddress.protect();
        ModelNode transport = Util.getEmptyOperation(ModelDescriptionConstants.ADD, transportAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        TP.class.getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + value).asSubclass(TP.class).newInstance();
                        CommonAttributes.TYPE.parseAndSetParameter(value, transport, reader);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SHARED: {
                    CommonAttributes.SHARED.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case SOCKET_BINDING: {
                    CommonAttributes.SOCKET_BINDING.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    CommonAttributes.DIAGNOSTICS_SOCKET_BINDING.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case DEFAULT_EXECUTOR: {
                    CommonAttributes.DEFAULT_EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case OOB_EXECUTOR: {
                    CommonAttributes.OOB_EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case TIMER_EXECUTOR: {
                    CommonAttributes.TIMER_EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case THREAD_FACTORY: {
                    CommonAttributes.THREAD_FACTORY.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case SITE: {
                    CommonAttributes.SITE.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case RACK: {
                    CommonAttributes.RACK.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case MACHINE: {
                    CommonAttributes.MACHINE.parseAndSetParameter(value, transport, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!transport.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw ParseUtils.unexpectedElement(reader);
            }
            int attributes = reader.getAttributeCount();
            String property = null;
            for (int i = 0; i < attributes; i++) {
                String value = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        property = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (property == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            String value = reader.getElementText();
            transport.get(ModelKeys.PROPERTIES).add(property, value);
        }
        operations.add(transport);
    }

    private void parseProtocol(XMLExtendedStreamReader reader, ModelNode stackAddress, List<ModelNode> operations) throws XMLStreamException {

        ModelNode protocol = Util.getEmptyOperation(ModelKeys.ADD_PROTOCOL, null);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        Protocol.class.getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + value).asSubclass(Protocol.class).newInstance();
                        CommonAttributes.TYPE.parseAndSetParameter(value, protocol, reader);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SOCKET_BINDING: {
                    CommonAttributes.SOCKET_BINDING.parseAndSetParameter(value, protocol, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!protocol.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        protocol.get(OP_ADDR).set(stackAddress);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw ParseUtils.unexpectedElement(reader);
            }
            int attributes = reader.getAttributeCount();
            String property = null;
            for (int i = 0; i < attributes; i++) {
                String value = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        property = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (property == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            String value = reader.getElementText();
            protocol.get(ModelKeys.PROPERTIES).add(property, value);
        }

        operations.add(protocol);
    }
}
