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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;

/**
 * Registers the JGroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsExtension implements Extension, DescriptionProvider, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    static final String SUBSYSTEM_NAME = "jgroups";

    private static final PathElement stacksPath = PathElement.pathElement(ModelKeys.STACK);
    private static final JGroupsSubsystemAdd add = new JGroupsSubsystemAdd();
    private static final JGroupsSubsystemRemove remove = new JGroupsSubsystemRemove();
    private static final JGroupsSubsystemDescribe describe = new JGroupsSubsystemDescribe();
    private static final ProtocolStackAdd stackAdd = new ProtocolStackAdd();
    private static final ProtocolStackRemove stackRemove = new ProtocolStackRemove();
    private static final DescriptionProvider stackDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JGroupsDescriptions.getProtocolStackDescription(locale);
        }
    };

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        subsystem.registerXMLElementWriter(this);

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(this);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, add, add, false);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, remove, remove, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, describe, describe, false, EntryType.PRIVATE);

        ManagementResourceRegistration stacks = registration.registerSubModel(stacksPath, stackDescription);
        stacks.registerOperationHandler(ModelDescriptionConstants.ADD, stackAdd, stackAdd, false);
        stacks.registerOperationHandler(ModelDescriptionConstants.REMOVE, stackRemove, stackRemove, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUri(), this);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return JGroupsDescriptions.getSubsystemDescription(locale);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);
        address.protect();

        ModelNode subsystem = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_STACK: {
                    subsystem.get(ModelKeys.DEFAULT_STACK).set(value);
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
                case JGROUPS_1_0: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case STACK: {
                            operations.add(this.parseStack(reader, address));
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

    private ModelNode parseStack(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

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

        final ModelNode stack = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        stack.get(ModelDescriptionConstants.OP_ADDR).set(address).add(ModelKeys.STACK, name);

        if (!reader.hasNext() || (reader.nextTag() == XMLStreamConstants.END_ELEMENT) || Element.forName(reader.getLocalName()) != Element.TRANSPORT) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.TRANSPORT));
        }

        this.parseProtocol(reader, stack.get(ModelKeys.TRANSPORT), TP.class);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROTOCOL: {
                    this.parseProtocol(reader, stack.get(ModelKeys.PROTOCOL).add(), Protocol.class);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        return stack;
    }

    private void parseProtocol(XMLExtendedStreamReader reader, ModelNode protocol, Class<? extends Protocol> targetClass) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        targetClass.getClassLoader().loadClass(org.jgroups.conf.ProtocolConfiguration.protocol_prefix + '.' + value).asSubclass(targetClass).newInstance();
                        protocol.get(ModelKeys.TYPE).set(value);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SHARED: {
                    protocol.get(ModelKeys.SHARED).set(Boolean.parseBoolean(value));
                    break;
                }
                case SOCKET_BINDING: {
                    protocol.get(ModelKeys.SOCKET_BINDING).set(value);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    protocol.get(ModelKeys.DIAGNOSTICS_SOCKET_BINDING).set(value);
                    break;
                }
                case DEFAULT_EXECUTOR: {
                    protocol.get(ModelKeys.DEFAULT_EXECUTOR).set(value);
                    break;
                }
                case OOB_EXECUTOR: {
                    protocol.get(ModelKeys.OOB_EXECUTOR).set(value);
                    break;
                }
                case TIMER_EXECUTOR: {
                    protocol.get(ModelKeys.TIMER_EXECUTOR).set(value);
                    break;
                }
                case THREAD_FACTORY: {
                    protocol.get(ModelKeys.THREAD_FACTORY).set(value);
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
            protocol.get(ModelKeys.PROPERTY).add(property, value);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementWriter#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter, java.lang.Object)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            this.writeOptional(writer, Attribute.DEFAULT_STACK, model, ModelKeys.DEFAULT_STACK);
            for (Property property: model.get(ModelKeys.STACK).asPropertyList()) {
                writer.writeStartElement(Element.STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                ModelNode stack = property.getValue();
                this.writeProtocol(writer, stack.get(ModelKeys.TRANSPORT), Element.TRANSPORT);
                if (stack.hasDefined(ModelKeys.PROTOCOL)) {
                    for (ModelNode protocol: stack.get(ModelKeys.PROTOCOL).asList()) {
                        this.writeProtocol(writer, protocol, Element.PROTOCOL);
                    }
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeProtocol(XMLExtendedStreamWriter writer, ModelNode protocol, Element element) throws XMLStreamException {
        writer.writeStartElement(element.getLocalName());
        this.writeRequired(writer, Attribute.TYPE, protocol, ModelKeys.TYPE);
        this.writeOptional(writer, Attribute.SHARED, protocol, ModelKeys.SHARED);
        this.writeOptional(writer, Attribute.SOCKET_BINDING, protocol, ModelKeys.SOCKET_BINDING);
        this.writeOptional(writer, Attribute.DIAGNOSTICS_SOCKET_BINDING, protocol, ModelKeys.DIAGNOSTICS_SOCKET_BINDING);
        this.writeOptional(writer, Attribute.DEFAULT_EXECUTOR, protocol, ModelKeys.DEFAULT_EXECUTOR);
        this.writeOptional(writer, Attribute.OOB_EXECUTOR, protocol, ModelKeys.OOB_EXECUTOR);
        this.writeOptional(writer, Attribute.TIMER_EXECUTOR, protocol, ModelKeys.TIMER_EXECUTOR);
        this.writeOptional(writer, Attribute.THREAD_FACTORY, protocol, ModelKeys.THREAD_FACTORY);
        if (protocol.has(ModelKeys.PROPERTY)) {
            for (Property property: protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                writer.writeCharacters(property.getValue().asString());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeRequired(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), model.require(key).asString());
    }

    private void writeOptional(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        if (model.hasDefined(key)) {
            writer.writeAttribute(attribute.getLocalName(), model.get(key).asString());
        }
    }
}
