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

package org.jboss.as.appclient.subsystem.parsing;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.CommonXml;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.appclient.logging.AppClientMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.parsing.Namespace.DOMAIN_1_0;
import static org.jboss.as.controller.parsing.Namespace.DOMAIN_1_1;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.nextElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * A mapper between an AS server's configuration model and XML representations, particularly {@code appclient.xml}.
 *
 * @author Stuart Douglas
 */
public class AppClientXml extends CommonXml {

    public AppClientXml(final ModuleLoader loader) {
        super(loader, null);
    }

    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operationList)
            throws XMLStreamException {
        final ModelNode address = new ModelNode().setEmptyList();

        if (Element.forName(reader.getLocalName()) != Element.SERVER) {
            throw unexpectedElement(reader);
        }

        Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
        switch (readerNS) {
            case DOMAIN_1_0: {
                readServerElement_1_0(reader, address, operationList);
                break;
            }
            case DOMAIN_1_1: {
                readServerElement_1_1(reader, address, operationList);
                break;
            }
            default: {
              throw unexpectedElement(reader);
            }
        }
    }

    /**
     * Read the <server/> element based on version 1.0 of the schema.
     */
    private void readServerElement_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {

        parseNamespaces(reader, address, list);

        String serverName = null;

        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case NONE: {
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            serverName = value;
                            break;
                        }
                        default:
                            throw unexpectedAttribute(reader, i);
                    }
                    break;
                }
                case XML_SCHEMA_INSTANCE: {
                    switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                        case SCHEMA_LOCATION: {
                            parseSchemaLocations(reader, address, list, i);
                            break;
                        }
                        case NO_NAMESPACE_SCHEMA_LOCATION: {
                            // todo, jeez
                            break;
                        }
                        default: {
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (serverName == null) {
            serverName = getDefaultName();
        }
        setServerName(address, list, serverName);

        // elements - sequence

        Element element = nextElement(reader, DOMAIN_1_0);
        if (element == Element.EXTENSIONS) {
            parseExtensions(reader, address, DOMAIN_1_0, list);
            element = nextElement(reader, DOMAIN_1_0);
        }
        // System properties
        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, DOMAIN_1_0, list, true);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element == Element.PATHS) {
            parsePaths(reader, address, DOMAIN_1_0, list, true);
            element = nextElement(reader, DOMAIN_1_0);
        }

        // Single profile
        if (element == Element.PROFILE) {
            parseServerProfile(reader, address, list);
            element = nextElement(reader, DOMAIN_1_0);
        }

        // Interfaces
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, DOMAIN_1_0, list, true);
            element = nextElement(reader, DOMAIN_1_0);
        }
        // Single socket binding group
        if (element == Element.SOCKET_BINDING_GROUP) {
            parseSocketBindingGroup(reader, interfaceNames, address, DOMAIN_1_0, list);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element != null) {
            throw unexpectedElement(reader);
        }
    }

    /**
     * Read the <server/> element based on version 1.1 of the schema.
     */
    private void readServerElement_1_1(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {

        parseNamespaces(reader, address, list);

        String serverName = null;

        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case NONE: {
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            serverName = value;
                            break;
                        }
                        default:
                            throw unexpectedAttribute(reader, i);
                    }
                    break;
                }
                case XML_SCHEMA_INSTANCE: {
                    switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                        case SCHEMA_LOCATION: {
                            parseSchemaLocations(reader, address, list, i);
                            break;
                        }
                        case NO_NAMESPACE_SCHEMA_LOCATION: {
                            // todo, jeez
                            break;
                        }
                        default: {
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (serverName == null) {
            serverName = getDefaultName();
        }
        setServerName(address, list, serverName);

        // elements - sequence

        Element element = nextElement(reader, DOMAIN_1_1);
        if (element == Element.EXTENSIONS) {
            parseExtensions(reader, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }
        // System properties
        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, DOMAIN_1_1, list, true);
            element = nextElement(reader, DOMAIN_1_1);
        }
        if (element == Element.PATHS) {
            parsePaths(reader, address, DOMAIN_1_1, list, true);
            element = nextElement(reader, DOMAIN_1_1);
        }

        if (element == Element.VAULT) {
            parseVault(reader, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }
        // Single profile
        if (element == Element.PROFILE) {
            parseServerProfile(reader, address, list);
            element = nextElement(reader, DOMAIN_1_1);
        }

        // Interfaces
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, DOMAIN_1_1, list, true);
            element = nextElement(reader, DOMAIN_1_1);
        }
        // Single socket binding group
        if (element == Element.SOCKET_BINDING_GROUP) {
            parseSocketBindingGroup(reader, interfaceNames, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }

        if (element != null) {
            throw unexpectedElement(reader);
        }
    }

    private void parseSocketBindingGroup(final XMLExtendedStreamReader reader, final Set<String> interfaces,
            final ModelNode address, final Namespace expectedNs, final List<ModelNode> updates) throws XMLStreamException {

        // unique names for both socket-binding and outbound-socket-binding(s)
        final Set<String> uniqueBindingNames = new HashSet<String>();

        ModelNode op = Util.getEmptyOperation(ADD, null);
        // Handle attributes
        String socketBindingGroupName = null;

        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.DEFAULT_INTERFACE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    socketBindingGroupName = value;
                    required.remove(attribute);
                    break;
                }
                case DEFAULT_INTERFACE: {
                    SocketBindingGroupResourceDefinition.DEFAULT_INTERFACE.parseAndSetParameter(value, op, reader);
                    required.remove(attribute);
                    break;
                }
                case PORT_OFFSET: {
                    SocketBindingGroupResourceDefinition.PORT_OFFSET.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }


        ModelNode groupAddress = address.clone().add(SOCKET_BINDING_GROUP, socketBindingGroupName);
        op.get(OP_ADDR).set(groupAddress);

        updates.add(op);

        // Handle elements
        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SOCKET_BINDING: {
                    // FIXME JBAS-8825
                    final String bindingName = parseSocketBinding(reader, interfaces, groupAddress, updates);
                    if (!uniqueBindingNames.add(bindingName)) {
                        throw ControllerMessages.MESSAGES.alreadyDeclared(Element.SOCKET_BINDING.getLocalName(), Element.OUTBOUND_SOCKET_BINDING.getLocalName(),
                                bindingName, Element.SOCKET_BINDING_GROUP.getLocalName(), socketBindingGroupName, reader.getLocation());
                    }
                    break;
                }
                case OUTBOUND_SOCKET_BINDING: {
                    final String bindingName = parseOutboundSocketBinding(reader, interfaces, socketBindingGroupName, groupAddress, updates);
                    if (!uniqueBindingNames.add(bindingName)) {
                        throw ControllerMessages.MESSAGES.alreadyDeclared(Element.SOCKET_BINDING.getLocalName(), Element.OUTBOUND_SOCKET_BINDING.getLocalName(),
                                bindingName, Element.SOCKET_BINDING_GROUP.getLocalName(), socketBindingGroupName, reader.getLocation());
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseServerProfile(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {
        // Attributes
        requireNoAttributes(reader);

        // Content
        final Set<String> configuredSubsystemTypes = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                throw unexpectedElement(reader);
            }
            if (!configuredSubsystemTypes.add(reader.getNamespaceURI())) {
                throw MESSAGES.duplicateSubsystemDeclaration(reader.getLocation());
            }
            // parse subsystem
            final List<ModelNode> subsystems = new ArrayList<ModelNode>();
            reader.handleAny(subsystems);

            // Process subsystems
            for (final ModelNode update : subsystems) {
                // Process relative subsystem path address
                final ModelNode subsystemAddress = address.clone();
                for (final Property path : update.get(OP_ADDR).asPropertyList()) {
                    subsystemAddress.add(path.getName(), path.getValue().asString());
                }
                update.get(OP_ADDR).set(subsystemAddress);
                list.add(update);
            }
        }
    }

    private void setServerName(final ModelNode address, final List<ModelNode> operationList, final String value) {
        if (value.length() > 0) {
            final ModelNode update = Util.getWriteAttributeOperation(address, NAME, value);
            operationList.add(update);
        }
    }

    public void writeContent(final XMLExtendedStreamWriter writer, final ModelMarshallingContext context)
            throws XMLStreamException {

        ModelNode modelNode = context.getModelNode();
        writer.writeStartDocument();
        writer.writeStartElement(Element.SERVER.getLocalName());

        if (modelNode.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, modelNode.get(NAME).asString());
        }

        writer.writeDefaultNamespace(Namespace.CURRENT.getUriString());
        writeNamespaces(writer, modelNode);
        writeSchemaLocation(writer, modelNode);

        if (modelNode.hasDefined(EXTENSION)) {
            writeExtensions(writer, modelNode.get(EXTENSION));
        }

        if (modelNode.hasDefined(SYSTEM_PROPERTY)) {
            writeProperties(writer, modelNode.get(SYSTEM_PROPERTY), Element.SYSTEM_PROPERTIES, true);
        }

        if (modelNode.hasDefined(PATH)) {
            writePaths(writer, modelNode.get(PATH));
        }

        if (modelNode.hasDefined(CORE_SERVICE) && modelNode.get(CORE_SERVICE).hasDefined(VAULT)) {
            writeVault(writer, modelNode.get(CORE_SERVICE, VAULT));
        }

        if (modelNode.hasDefined(CORE_SERVICE) && modelNode.get(CORE_SERVICE).hasDefined(MANAGEMENT)) {
            writeManagement(writer, modelNode.get(CORE_SERVICE, MANAGEMENT), true);
        }

        writeServerProfile(writer, context);

        if (modelNode.hasDefined(INTERFACE)) {
            writeInterfaces(writer, modelNode.get(INTERFACE));
        }

        if (modelNode.hasDefined(SOCKET_BINDING_GROUP)) {
            Set<String> groups = modelNode.get(SOCKET_BINDING_GROUP).keys();
            if (groups.size() > 1) {
                throw MESSAGES.multipleNodesFound(SOCKET_BINDING_GROUP);
            }
            for (String group : groups) {
                writeSocketBindingGroup(writer, modelNode.get(SOCKET_BINDING_GROUP, group), true);
            }
        }

        if (modelNode.hasDefined(DEPLOYMENT)) {
            writeServerDeployments(writer, modelNode.get(DEPLOYMENT));
        }
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void writeServerDeployments(final XMLExtendedStreamWriter writer, final ModelNode modelNode)
            throws XMLStreamException {

        Set<String> deploymentNames = modelNode.keys();
        if (deploymentNames.size() > 0) {
            boolean deploymentWritten = false;
            for (String uniqueName : deploymentNames) {
                final ModelNode deployment = modelNode.get(uniqueName);
                if (deployment.hasDefined(PERSISTENT) && !deployment.get(PERSISTENT).asBoolean()) {
                    continue;
                }
                if (!deploymentWritten) {
                    writer.writeStartElement(Element.DEPLOYMENTS.getLocalName());
                    deploymentWritten = true;
                }
                final String runtimeName = deployment.get(RUNTIME_NAME).asString();
                boolean enabled = deployment.get(ENABLED).asBoolean();
                writer.writeStartElement(Element.DEPLOYMENT.getLocalName());
                writeAttribute(writer, Attribute.NAME, uniqueName);
                writeAttribute(writer, Attribute.RUNTIME_NAME, runtimeName);
                if (!enabled) {
                    writeAttribute(writer, Attribute.ENABLED, "false");
                }
                final List<ModelNode> contentItems = deployment.require(CONTENT).asList();
                for (ModelNode contentItem : contentItems) {
                    writeContentItem(writer, contentItem);
                }
                writer.writeEndElement();
            }
            if (deploymentWritten) {
                writer.writeEndElement();
            }
        }
    }

    private void writeServerProfile(final XMLExtendedStreamWriter writer, final ModelMarshallingContext context)
            throws XMLStreamException {

        final ModelNode profileNode = context.getModelNode();
        // In case there are no subsystems defined
        if(! profileNode.hasDefined(SUBSYSTEM)) {
            writer.writeEmptyElement(Element.PROFILE.getLocalName());
            return;
        }

        writer.writeStartElement(Element.PROFILE.getLocalName());
        Set<String> subsystemNames = profileNode.get(SUBSYSTEM).keys();
        if (subsystemNames.size() > 0) {
            String defaultNamespace = writer.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);
            for (String subsystemName : subsystemNames) {
                try {
                    ModelNode subsystem = profileNode.get(SUBSYSTEM, subsystemName);
                    XMLElementWriter<SubsystemMarshallingContext> subsystemWriter = context.getSubsystemWriter(subsystemName);
                    if (subsystemWriter != null) { // FIXME -- remove when extensions are doing the registration
                        subsystemWriter.writeContent(writer, new SubsystemMarshallingContext(subsystem, writer));
                    }
                } finally {
                    writer.setDefaultNamespace(defaultNamespace);
                }
            }
        }
        writer.writeEndElement();
    }
}
