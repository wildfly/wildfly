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

package org.jboss.as.controller.parsing;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTION;
import static org.jboss.as.controller.parsing.Namespace.DOMAIN_1_0;
import static org.jboss.as.controller.parsing.Namespace.DOMAIN_1_1;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.nextElement;
import static org.jboss.as.controller.parsing.ParseUtils.parsePossibleExpression;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapper between an AS server's configuration model and XML representations, particularly {@code host.xml}
 *
 * @author Brian Stansberry
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HostXml extends CommonXml {

    public HostXml(final ModuleLoader loader) {
        super(loader);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operationList)
            throws XMLStreamException {
        final ModelNode address = new ModelNode().setEmptyList();
        if (Element.forName(reader.getLocalName()) != Element.HOST) {
            throw unexpectedElement(reader);
        }
        Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
        switch (readerNS) {
            case DOMAIN_1_0: {
                readHostElement_1_0(reader, address, operationList);
                break;
            }
            case DOMAIN_1_1: {
                readHostElement_1_1(reader, address, operationList);
                break;
            }
            default: {
                throw unexpectedElement(reader);
            }
        }
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final ModelMarshallingContext context)
            throws XMLStreamException {

        final ModelNode modelNode = context.getModelNode();

        writer.writeStartDocument();
        writer.writeStartElement(Element.HOST.getLocalName());

        if (modelNode.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, modelNode.get(NAME).asString());
        }

        writer.writeDefaultNamespace(Namespace.CURRENT.getUriString());
        writeNamespaces(writer, modelNode);
        writeSchemaLocation(writer, modelNode);

        if (modelNode.hasDefined(SYSTEM_PROPERTY)) {
            writeProperties(writer, modelNode.get(SYSTEM_PROPERTY), Element.SYSTEM_PROPERTIES, false);
        }

        if (modelNode.hasDefined(PATH)) {
            writePaths(writer, modelNode.get(PATH));
        }

        if (modelNode.hasDefined(CORE_SERVICE) && modelNode.get(CORE_SERVICE).hasDefined(VAULT)) {
            writeVault(writer, modelNode.get(CORE_SERVICE, VAULT));
        }

        if (modelNode.hasDefined(VAULT)) {
            ModelNode vault = modelNode.get(VAULT);
            writer.writeStartElement(Element.VAULT.getLocalName());
            String code = vault.get(Attribute.CODE.getLocalName()).asString();
            if (code != null && !code.isEmpty() && !code.equals("undefined")) {
                writer.writeAttribute(Attribute.CODE.getLocalName(), code);
            }

            //TODO: not sure why the vault option is coming under ADD
            ModelNode addNode = vault.get(ADD);
            ModelNode properties = addNode.get(VAULT_OPTION);
            for (Property prop : properties.asPropertyList()) {
                writer.writeEmptyElement(Element.VAULT_OPTION.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), prop.getValue().asString());
            }
            writer.writeEndElement();
        }

        if (modelNode.hasDefined(CORE_SERVICE) && modelNode.get(CORE_SERVICE).hasDefined(MANAGEMENT)) {
            writeManagement(writer, modelNode.get(CORE_SERVICE, MANAGEMENT), true);
        }

        if (modelNode.hasDefined(DOMAIN_CONTROLLER)) {
            writeDomainController(writer, modelNode.get(DOMAIN_CONTROLLER));
        }

        if (modelNode.hasDefined(INTERFACE)) {
            writeInterfaces(writer, modelNode.get(INTERFACE));
        }
        if (modelNode.hasDefined(JVM)) {
            writer.writeStartElement(Element.JVMS.getLocalName());
            for (final Property jvm : modelNode.get(JVM).asPropertyList()) {
                writeJVMElement(writer, jvm.getName(), jvm.getValue());
            }
            writer.writeEndElement();
        }

        if (modelNode.hasDefined(SERVER_CONFIG)) {
            writeServers(writer, modelNode.get(SERVER_CONFIG));
        }

        writer.writeEndElement();
        writer.writeEndDocument();
    }

    private void readHostElement_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {
        String hostName = null;

        // Deffer adding the namespaces and schema locations until after the host has been created.
        List<ModelNode> namespaceOperations = new LinkedList<ModelNode>();
        parseNamespaces(reader, address, namespaceOperations);

        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case NONE: {
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            hostName = value;
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
                            parseSchemaLocations(reader, address, namespaceOperations, i);
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

        if (hostName == null) {
            hostName = getDefaultName();
        }
        // The follownig also updates the address parameter so this address can be used for future operations
        // in the context of this host.
        addLocalHost(address, list, hostName);
        // The namespace operations were created before the host name was known, the address can now be updated
        // to the local host specific address.
        for (ModelNode operation : namespaceOperations) {
            operation.get(OP_ADDR).set(address);
            list.add(operation);
        }

        // Content
        // Handle elements: sequence

        Element element = nextElement(reader, DOMAIN_1_0);

        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, DOMAIN_1_0, list, false);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element == Element.PATHS) {
            parsePaths(reader, address, DOMAIN_1_0, list, true);
            element = nextElement(reader, DOMAIN_1_0);
        }

        if (element == Element.MANAGEMENT) {
            parseManagement(reader, address, DOMAIN_1_0, list, true);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element == Element.DOMAIN_CONTROLLER) {
            parseDomainController(reader, address, DOMAIN_1_0, list);
            element = nextElement(reader, DOMAIN_1_0);
        }
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, DOMAIN_1_0, list, true);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element == Element.JVMS) {
            parseJvms(reader, address, DOMAIN_1_0, list);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element == Element.SERVERS) {
            parseServers(reader, address, DOMAIN_1_0, list);
            element = nextElement(reader, DOMAIN_1_0);
        }

    }

    private void readHostElement_1_1(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
            throws XMLStreamException {
        String hostName = null;

        // Deffer adding the namespaces and schema locations until after the host has been created.
        List<ModelNode> namespaceOperations = new LinkedList<ModelNode>();
        parseNamespaces(reader, address, namespaceOperations);

        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case NONE: {
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            hostName = value;
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
                            parseSchemaLocations(reader, address, namespaceOperations, i);
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

        if (hostName == null) {
            hostName = getDefaultName();
        }
        // The follownig also updates the address parameter so this address can be used for future operations
        // in the context of this host.
        addLocalHost(address, list, hostName);
        // The namespace operations were created before the host name was known, the address can now be updated
        // to the local host specific address.
        for (ModelNode operation : namespaceOperations) {
            operation.get(OP_ADDR).set(address);
            list.add(operation);
        }

        // Content
        // Handle elements: sequence

        Element element = nextElement(reader, DOMAIN_1_1);

        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, DOMAIN_1_1, list, false);
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
        if (element == Element.MANAGEMENT) {
            parseManagement(reader, address, DOMAIN_1_1, list, true);
            element = nextElement(reader, DOMAIN_1_1);
        }
        if (element == Element.DOMAIN_CONTROLLER) {
            parseDomainController(reader, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, DOMAIN_1_1, list, true);
            element = nextElement(reader, DOMAIN_1_1);
        }
        if (element == Element.JVMS) {
            parseJvms(reader, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }
        if (element == Element.SERVERS) {
            parseServers(reader, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }

    }

        // for ( ;reader.hasNext(); ) {
        // switch (reader.nextTag()) {
        // case START_ELEMENT: {
        // readHeadComment(reader, address, list);
        // if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
        // throw unexpectedElement(reader);
        // }
        // switch (Element.forName(reader.getLocalName())) {
        // default: throw unexpectedElement(reader);
        // }
        // }
        // case END_ELEMENT: {
        // readTailComment(reader, address, list);
        // return;
        // }
        // default: throw new IllegalStateException();
        // }
        // }

    /**
     * Add the operation to add the local host definition.
     */
    private void addLocalHost(final ModelNode address, final List<ModelNode> operationList, final String hostName) {
        // All further operations should modify the newly added host so the address passed in is updated.
        address.add(HOST, hostName);

        final ModelNode host = new ModelNode();
        host.get(OP).set("add-host");
        host.get(OP_ADDR).set(address.clone());

        host.get(NAME).set(hostName);

        operationList.add(host);
    }

    private void parseDomainController(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {

        requireNoAttributes(reader);

        boolean hasLocal = false;
        boolean hasRemote = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOCAL: {
                    if (hasLocal) {
                        throw new XMLStreamException("Child " + element.getLocalName() + " of element "
                                + Element.DOMAIN_CONTROLLER.getLocalName() + " already declared", reader.getLocation());
                    } else if (hasRemote) {
                        throw new XMLStreamException("Child " + Element.REMOTE.getLocalName() + " of element "
                                + Element.DOMAIN_CONTROLLER.getLocalName() + " already declared", reader.getLocation());
                    }
                    requireNoAttributes(reader);
                    requireNoContent(reader);
                    hasLocal = true;
                    break;
                }
                case REMOTE: {
                    if (hasRemote) {
                        throw new XMLStreamException("Child " + element.getLocalName() + " of element "
                                + Element.DOMAIN_CONTROLLER.getLocalName() + " already declared", reader.getLocation());
                    } else if (hasLocal) {
                        throw new XMLStreamException("Child " + Element.LOCAL.getLocalName() + " of element "
                                + Element.DOMAIN_CONTROLLER.getLocalName() + " already declared", reader.getLocation());
                    }
                    parseRemoteDomainController(reader, address, list);
                    hasRemote = true;
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
        if (!hasLocal && !hasRemote) {
            throw new XMLStreamException("Either a " + Element.REMOTE.getLocalName() + " or " + Element.LOCAL.getLocalName()
                    + " domain controller configuration must be declared.", reader.getLocation());
        }

        if (hasLocal) {
            final ModelNode update = new ModelNode();
            update.get(OP_ADDR).set(address);
            update.get(OP).set("write-local-domain-controller");
            list.add(update);
        }
    }

    private void parseRemoteDomainController(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> list) throws XMLStreamException {
        // Handle attributes
        String host = null;
        Integer port = null;
        String securityRealm = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case HOST: {
                        host = value;
                        break;
                    }
                    case PORT: {
                        port = Integer.valueOf(value);
                        if (port.intValue() < 1) {
                            throw new XMLStreamException("Illegal '" + attribute.getLocalName() + "' value " + port
                                    + " -- cannot be less than one", reader.getLocation());
                        }
                        break;
                    }
                    case SECURITY_REALM: {
                        securityRealm = value;
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (host == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.HOST.getLocalName()));
        }
        if (port == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PORT.getLocalName()));
        }

        final ModelNode update = new ModelNode();
        update.get(OP_ADDR).set(address);
        update.get(OP).set("write-remote-domain-controller");
        update.get(HOST).set(parsePossibleExpression(host));
        update.get(PORT).set(port);
        if (securityRealm != null) {
            update.get(SECURITY_REALM).set(securityRealm);
        }
        list.add(update);

        reader.discardRemainder();
    }

    private void parseJvms(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {

        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case JVM:
                    parseJvm(reader, address, expectedNs, list, names);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseServers(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {

        requireNoAttributes(reader);
        // Handle elements
        final Set<String> names = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SERVER:
                    parseServer(reader, address, expectedNs, list, names);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseServer(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> list,
            final Set<String> serverNames) throws XMLStreamException {
        // Handle attributes
        String name = null;
        String group = null;
        Boolean start = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        if (!serverNames.add(value)) {
                            throw new XMLStreamException("Duplicate server declaration " + value, reader.getLocation());
                        }
                        name = value;
                        break;
                    }
                    case GROUP: {
                        group = value;
                        break;
                    }
                    case AUTO_START: {
                        start = Boolean.valueOf(value);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (group == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.GROUP));
        }

        final ModelNode address = parentAddress.clone().add(SERVER_CONFIG, name);
        final ModelNode addUpdate = Util.getEmptyOperation(ADD, address);
        addUpdate.get(GROUP).set(group);
        if (start != null) {
            addUpdate.get(AUTO_START).set(start.booleanValue());
        }
        list.add(addUpdate);

        // Handle elements
        boolean sawJvm = false;
        boolean sawSystemProperties = false;
        boolean sawSocketBinding = false;
        final Set<String> interfaceNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INTERFACE_SPECS: {
                    parseInterfaces(reader, interfaceNames, address, expectedNs, list, true);
                    break;
                }
                case JVM: {
                    if (sawJvm) {
                        throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                    }

                    parseJvm(reader, address, expectedNs, list, new HashSet<String>(), true);
                    sawJvm = true;
                    break;
                }
                case PATHS: {
                    parsePaths(reader, address, expectedNs, list, true);
                    break;
                }
                case SOCKET_BINDING_GROUP: {
                    if (sawSocketBinding) {
                        throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                    }
                    parseSocketBindingGroupRef(reader, address, list);
                    sawSocketBinding = true;
                    break;
                }
                case SYSTEM_PROPERTIES: {
                    if (sawSystemProperties) {
                        throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                    }
                    parseSystemProperties(reader, address, expectedNs, list, false);
                    sawSystemProperties = true;
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

    }

    /**
     * Overrides the CommonXML implementation of parseNativeRemotingManagementInterface_1_1 to mark unexpected in
     * host.xml parsing.
     */
    // TODO - This still needs to be properly represented in the schema.
    @Override
    protected void parseNativeRemotingManagementInterface_1_1(XMLExtendedStreamReader reader, ModelNode address, List<ModelNode> list) throws XMLStreamException {
        throw unexpectedElement(reader);
    }

    private void writeDomainController(final XMLExtendedStreamWriter writer, final ModelNode modelNode)
            throws XMLStreamException {
        writer.writeStartElement(Element.DOMAIN_CONTROLLER.getLocalName());
        if (modelNode.hasDefined(LOCAL)) {
            writer.writeEmptyElement(Element.LOCAL.getLocalName());
        } else if (modelNode.hasDefined(REMOTE)) {
            writer.writeStartElement(Element.REMOTE.getLocalName());
            final ModelNode remote = modelNode.get(REMOTE);
            if (remote.has(HOST)) {
                writeAttribute(writer, Attribute.HOST, remote.get(HOST).asString());
            }
            if (remote.has(PORT)) {
                writeAttribute(writer, Attribute.PORT, remote.get(PORT).asString());
            }
            if (remote.hasDefined(SECURITY_REALM)) {
                writeAttribute(writer, Attribute.SECURITY_REALM, remote.require(SECURITY_REALM).asString());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeServers(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        writer.writeStartElement(Element.SERVERS.getLocalName());

        for (Property prop : modelNode.asPropertyList()) {
            final ModelNode server = prop.getValue();

            writer.writeStartElement(Element.SERVER.getLocalName());

            writeAttribute(writer, Attribute.NAME, prop.getName());
            if (server.hasDefined(GROUP)) {
                writeAttribute(writer, Attribute.GROUP, server.get(GROUP).asString());
            }
            if (server.hasDefined(AUTO_START)) {
                writeAttribute(writer, Attribute.AUTO_START, server.get(AUTO_START).asString());
            }
            if (server.hasDefined(PATH)) {
                writePaths(writer, server.get(PATH));
            }
            if (server.hasDefined(SYSTEM_PROPERTY)) {
                writeProperties(writer, server.get(SYSTEM_PROPERTY), Element.SYSTEM_PROPERTIES, false);
            }
            if (server.hasDefined(INTERFACE)) {
                writeInterfaces(writer, server.get(INTERFACE));
            }
            if (server.hasDefined(JVM)) {
                for (final Property jvm : server.get(JVM).asPropertyList()) {
                    writeJVMElement(writer, jvm.getName(), jvm.getValue());
                    break; // TODO just write the first !?
                }
            }
            if (server.hasDefined(SOCKET_BINDING_GROUP)) {
                writer.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
                writeAttribute(writer, Attribute.REF, server.get(SOCKET_BINDING_GROUP).asString());
                if (server.hasDefined(SOCKET_BINDING_PORT_OFFSET) && server.get(SOCKET_BINDING_PORT_OFFSET).asInt() > 0) {
                    writeAttribute(writer, Attribute.PORT_OFFSET, server.get(SOCKET_BINDING_PORT_OFFSET).asString());
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

}
