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

package org.jboss.as.host.controller.parsing;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSOLE_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
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
import static org.jboss.as.controller.parsing.Namespace.DOMAIN_1_0;
import static org.jboss.as.controller.parsing.Namespace.DOMAIN_1_1;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.nextElement;
import static org.jboss.as.controller.parsing.ParseUtils.parsePossibleExpression;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.CommonXml;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.domain.management.parsing.ManagementXml;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
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
public class HostXml extends CommonXml implements ManagementXml.Delegate {

    public HostXml(final ModuleLoader loader, ExecutorService executorService) {
        super();
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

        writeNewLine(writer);

        if (modelNode.hasDefined(SYSTEM_PROPERTY)) {
            writeProperties(writer, modelNode.get(SYSTEM_PROPERTY), Element.SYSTEM_PROPERTIES, false);
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(PATH)) {
            writePaths(writer, modelNode.get(PATH));
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(CORE_SERVICE) && modelNode.get(CORE_SERVICE).hasDefined(VAULT)) {
            writeVault(writer, modelNode.get(CORE_SERVICE, VAULT));
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(CORE_SERVICE) && modelNode.get(CORE_SERVICE).hasDefined(MANAGEMENT)) {
            ManagementXml managementXml = new ManagementXml(this);
            managementXml.writeManagement(writer, modelNode.get(CORE_SERVICE, MANAGEMENT), true);
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(DOMAIN_CONTROLLER)) {
            writeDomainController(writer, modelNode.get(DOMAIN_CONTROLLER));
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(INTERFACE)) {
            writeInterfaces(writer, modelNode.get(INTERFACE));
            writeNewLine(writer);
        }
        if (modelNode.hasDefined(JVM)) {
            writer.writeStartElement(Element.JVMS.getLocalName());
            for (final Property jvm : modelNode.get(JVM).asPropertyList()) {
                writeJVMElement(writer, jvm.getName(), jvm.getValue());
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(SERVER_CONFIG)) {
            writeServers(writer, modelNode.get(SERVER_CONFIG));
            writeNewLine(writer);
        }

        writer.writeEndElement();
        writeNewLine(writer);
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
            ManagementXml managementXml = new ManagementXml(this);
            managementXml.parseManagement(reader, address, DOMAIN_1_0, list, true, false);
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
            ManagementXml managementXml = new ManagementXml(this);
            managementXml.parseManagement(reader, address, DOMAIN_1_1, list, true, true);
            element = nextElement(reader, DOMAIN_1_1);
        } else {
            throw missingRequiredElement(reader, EnumSet.of(Element.MANAGEMENT));
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
        if (element != null) {
            throw unexpectedElement(reader);
        }

    }

    @Override
    public void parseManagementInterfaces(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {

        requireNoAttributes(reader);
        Set<Element> required = EnumSet.of(Element.NATIVE_INTERFACE);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            switch (element) {
                case NATIVE_INTERFACE: {
                    switch (expectedNs) {
                        case DOMAIN_1_0:
                            parseNativeManagementInterface1_0(reader, address, list);
                            break;
                        default:
                            parseManagementInterface1_1(reader, address, false, expectedNs, list);
                    }
                    break;
                }
                case HTTP_INTERFACE: {
                    switch (expectedNs) {
                        case DOMAIN_1_0:
                            parseHttpManagementInterface1_0(reader, address, list);
                            break;
                        default:
                            parseManagementInterface1_1(reader, address, true, expectedNs, list);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
    }

    private void parseHttpManagementInterface1_0(final XMLExtendedStreamReader reader, final ModelNode address,
                                                   final List<ModelNode> list) throws XMLStreamException {

        final ModelNode mgmtSocket = new ModelNode();
        mgmtSocket.get(OP).set(ADD);
        ModelNode operationAddress = address.clone();
        operationAddress.add(MANAGEMENT_INTERFACE, HTTP_INTERFACE);
        mgmtSocket.get(OP_ADDR).set(operationAddress);

        // Handle attributes
        boolean hasInterfaceName = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case INTERFACE: {
                        HttpManagementResourceDefinition.INTERFACE.parseAndSetParameter(value, mgmtSocket, reader);
                        hasInterfaceName = true;
                        break;
                    }
                    case PORT: {
                        HttpManagementResourceDefinition.HTTP_PORT.parseAndSetParameter(value, mgmtSocket, reader);
                        break;
                    }
                    case SECURE_PORT: {
                        HttpManagementResourceDefinition.HTTPS_PORT.parseAndSetParameter(value, mgmtSocket, reader);
                        break;
                    }
                    case MAX_THREADS: {
                        // ignore xsd mistake
                        break;
                    }
                    case SECURITY_REALM: {
                        HttpManagementResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, mgmtSocket, reader);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        if (!hasInterfaceName) {
            throw missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }

        list.add(mgmtSocket);
    }

    private void parseNativeManagementInterface1_0(final XMLExtendedStreamReader reader, final ModelNode address,
                                                   final List<ModelNode> list) throws XMLStreamException {

        final ModelNode mgmtSocket = new ModelNode();
        mgmtSocket.get(OP).set(ADD);
        ModelNode operationAddress = address.clone();
        operationAddress.add(MANAGEMENT_INTERFACE, NATIVE_INTERFACE);
        mgmtSocket.get(OP_ADDR).set(operationAddress);

        // Handle attributes
        boolean hasInterface = false;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case INTERFACE: {
                        NativeManagementResourceDefinition.INTERFACE.parseAndSetParameter(value, mgmtSocket, reader);
                        hasInterface = true;
                        break;
                    }
                    case PORT: {
                        NativeManagementResourceDefinition.NATIVE_PORT.parseAndSetParameter(value, mgmtSocket, reader);
                        break;
                    }
                    case SECURE_PORT:
                        // ignore -- this was a bug in the xsd
                        break;
                    case SECURITY_REALM: {
                        NativeManagementResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, mgmtSocket, reader);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        if (!hasInterface) {
            throw missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }

        list.add(mgmtSocket);
    }

    private void parseManagementInterface1_1(XMLExtendedStreamReader reader, ModelNode address, boolean http, Namespace expectedNs, List<ModelNode> list)  throws XMLStreamException {

        final ModelNode operationAddress = address.clone();
        operationAddress.add(MANAGEMENT_INTERFACE, http ? HTTP_INTERFACE : NATIVE_INTERFACE);
        final ModelNode addOp = Util.getEmptyOperation(ADD, operationAddress);

        // Handle attributes

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SECURITY_REALM: {
                        if (http) {
                            HttpManagementResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, addOp, reader);
                        } else {
                            NativeManagementResourceDefinition.SECURITY_REALM.parseAndSetParameter(value, addOp, reader);
                        }
                        break;
                    }
                    case CONSOLE_ENABLED:{
                        if (http){
                            org.jboss.as.server.mgmt.HttpManagementResourceDefinition.CONSOLE_ENABLED.parseAndSetParameter(value,addOp,reader);
                        }
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case SOCKET:
                    if (http) {
                        parseHttpManagementSocket(reader, addOp);
                    } else {
                        parseNativeManagementSocket(reader, addOp);
                    }
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }

        list.add(addOp);
    }

    private void parseNativeManagementSocket(XMLExtendedStreamReader reader, ModelNode addOp) throws XMLStreamException {
        // Handle attributes
        boolean hasInterface = false;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case INTERFACE: {
                        NativeManagementResourceDefinition.INTERFACE.parseAndSetParameter(value, addOp, reader);
                        hasInterface = true;
                        break;
                    }
                    case PORT: {
                        NativeManagementResourceDefinition.NATIVE_PORT.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        if (!hasInterface) {
            throw missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }
    }

    private void parseHttpManagementSocket(XMLExtendedStreamReader reader, ModelNode addOp) throws XMLStreamException {
        // Handle attributes
        boolean hasInterface = false;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case INTERFACE: {
                        HttpManagementResourceDefinition.INTERFACE.parseAndSetParameter(value, addOp, reader);
                        hasInterface = true;
                        break;
                    }
                    case PORT: {
                        HttpManagementResourceDefinition.HTTP_PORT.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case SECURE_PORT: {
                        HttpManagementResourceDefinition.HTTPS_PORT.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);

        if (!hasInterface) {
            throw missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }
    }

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
                        throw MESSAGES.childAlreadyDeclared(element.getLocalName(),
                                Element.DOMAIN_CONTROLLER.getLocalName(), reader.getLocation());
                    } else if (hasRemote) {
                        throw MESSAGES.childAlreadyDeclared(Element.REMOTE.getLocalName(),
                                Element.DOMAIN_CONTROLLER.getLocalName(), reader.getLocation());
                    }
                    requireNoAttributes(reader);
                    requireNoContent(reader);
                    hasLocal = true;
                    break;
                }
                case REMOTE: {
                    if (hasRemote) {
                        throw MESSAGES.childAlreadyDeclared(element.getLocalName(),
                                Element.DOMAIN_CONTROLLER.getLocalName(), reader.getLocation());
                    } else if (hasLocal) {
                        throw MESSAGES.childAlreadyDeclared(Element.LOCAL.getLocalName(),
                                Element.DOMAIN_CONTROLLER.getLocalName(), reader.getLocation());
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
            throw MESSAGES.domainControllerMustBeDeclared(Element.REMOTE.getLocalName(), Element.LOCAL.getLocalName(),
                    reader.getLocation());
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
                            throw MESSAGES.invalidValueGreaterThan(attribute.getLocalName(), port, 0, reader.getLocation());
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

    private void parseServer(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
                             final Namespace expectedNs, final List<ModelNode> list,
            final Set<String> serverNames) throws XMLStreamException {

        // Handle attributes
        final ModelNode addUpdate = parseServerAttributes(reader, parentAddress, serverNames);
        final ModelNode address = addUpdate.require(OP_ADDR);
        list.add(addUpdate);

        // Handle elements
        switch (expectedNs) {
            case DOMAIN_1_0:
                parseServerContent1_0(reader, address, expectedNs, list);
                break;
            default:
                parseServerContent1_1(reader, address, expectedNs, list);
        }

    }

    private void parseServerContent1_0(final XMLExtendedStreamReader reader, final ModelNode serverAddress,
                                       final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        boolean sawJvm = false;
        boolean sawSystemProperties = false;
        boolean sawSocketBinding = false;
        final Set<String> interfaceNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INTERFACE_SPECS: {
                    parseInterfaces(reader, interfaceNames, serverAddress, expectedNs, list, true);
                    break;
                }
                case JVM: {
                    if (sawJvm) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }

                    parseJvm(reader, serverAddress, expectedNs, list, new HashSet<String>(), true);
                    sawJvm = true;
                    break;
                }
                case PATHS: {
                    parsePaths(reader, serverAddress, expectedNs, list, true);
                    break;
                }
                case SOCKET_BINDING_GROUP: {
                    if (sawSocketBinding) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseSocketBindingGroupRef(reader, serverAddress, list);
                    sawSocketBinding = true;
                    break;
                }
                case SYSTEM_PROPERTIES: {
                    if (sawSystemProperties) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseSystemProperties(reader, serverAddress, expectedNs, list, false);
                    sawSystemProperties = true;
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

    }

    private void parseServerContent1_1(final XMLExtendedStreamReader reader, final ModelNode serverAddress,
                                       final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        boolean sawJvm = false;
        boolean sawSystemProperties = false;
        boolean sawSocketBinding = false;
        final Set<String> interfaceNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INTERFACES: { // THIS IS DIFFERENT FROM 1.0
                    parseInterfaces(reader, interfaceNames, serverAddress, expectedNs, list, true);
                    break;
                }
                case JVM: {
                    if (sawJvm) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }

                    parseJvm(reader, serverAddress, expectedNs, list, new HashSet<String>(), true);
                    sawJvm = true;
                    break;
                }
                case PATHS: {
                    parsePaths(reader, serverAddress, expectedNs, list, true);
                    break;
                }
                case SOCKET_BINDINGS: {
                    if (sawSocketBinding) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseServerSocketBindings(reader, serverAddress, list);
                    sawSocketBinding = true;
                    break;
                }
                case SYSTEM_PROPERTIES: {
                    if (sawSystemProperties) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseSystemProperties(reader, serverAddress, expectedNs, list, false);
                    sawSystemProperties = true;
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

    }

    private ModelNode parseServerAttributes(final XMLExtendedStreamReader reader, final ModelNode parentAddress,
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
                            throw MESSAGES.duplicateDeclaration("server", value, reader.getLocation());
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
        return addUpdate;
    }

    private void parseServerSocketBindings(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> updates) throws XMLStreamException {
        // Handle attributes
        String name = null;
        Integer offset = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SOCKET_BINDING_GROUP: {
                        if (name != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        name = value;
                        break;
                    }
                    case PORT_OFFSET: {
                        try {
                            if (offset != null)
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            offset = Integer.parseInt(value);
                        } catch (final NumberFormatException e) {
                            throw MESSAGES.invalid(e, offset, attribute.getLocalName(), reader.getLocation());
                        }
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        // Handle elements
        requireNoContent(reader);

        if (name != null) {
            ModelNode update = Util.getWriteAttributeOperation(address, SOCKET_BINDING_GROUP, name);
            updates.add(update);
        }

        if (offset != 0) {
            ModelNode update = Util.getWriteAttributeOperation(address, SOCKET_BINDING_PORT_OFFSET, offset.intValue());
            updates.add(update);
        }

    }

    @Override
    public void writeNativeManagementProtocol(final XMLExtendedStreamWriter writer, final ModelNode protocol)
            throws XMLStreamException {

        writer.writeStartElement(Element.NATIVE_INTERFACE.getLocalName());
        NativeManagementResourceDefinition.SECURITY_REALM.marshallAsAttribute(protocol, writer);

        writer.writeEmptyElement(Element.SOCKET.getLocalName());
        NativeManagementResourceDefinition.INTERFACE.marshallAsAttribute(protocol, writer);
        NativeManagementResourceDefinition.NATIVE_PORT.marshallAsAttribute(protocol, writer);

        writer.writeEndElement();
    }

    @Override
    public void writeHttpManagementProtocol(final XMLExtendedStreamWriter writer, final ModelNode protocol)
            throws XMLStreamException {

        writer.writeStartElement(Element.HTTP_INTERFACE.getLocalName());
        HttpManagementResourceDefinition.SECURITY_REALM.marshallAsAttribute(protocol, writer);
        HttpManagementResourceDefinition.CONSOLE_ENABLED.marshallAsAttribute(protocol, writer);

        writer.writeEmptyElement(Element.SOCKET.getLocalName());
        HttpManagementResourceDefinition.INTERFACE.marshallAsAttribute(protocol, writer);
        HttpManagementResourceDefinition.HTTP_PORT.marshallAsAttribute(protocol, writer);
        HttpManagementResourceDefinition.HTTPS_PORT.marshallAsAttribute(protocol, writer);

        writer.writeEndElement();
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
            if (remote.get(CONSOLE_ENABLED).asBoolean(false)){
                writeAttribute(writer, Attribute.CONSOLE_ENABLED, remote.require(CONSOLE_ENABLED).asString());
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
            if (server.hasDefined(SOCKET_BINDING_GROUP) || server.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
                writer.writeStartElement(Element.SOCKET_BINDINGS.getLocalName());
                if (server.hasDefined(SOCKET_BINDING_GROUP)) {
                    writeAttribute(writer, Attribute.SOCKET_BINDING_GROUP, server.get(SOCKET_BINDING_GROUP).asString());
                }
                if (server.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
                    writeAttribute(writer, Attribute.PORT_OFFSET, server.get(SOCKET_BINDING_PORT_OFFSET).asString());
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

}
