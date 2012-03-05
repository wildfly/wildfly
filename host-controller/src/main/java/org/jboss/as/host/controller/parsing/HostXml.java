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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DIRECTORY_GROUPING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED_RESOURCES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IGNORED_RESOURCE_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMES;
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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.CommonXml;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.domain.management.parsing.ManagementXml;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.as.host.controller.ignored.IgnoredDomainTypeResourceDefinition;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapper between an AS server's configuration model and XML representations, particularly {@code host.xml}
 *
 * @author Brian Stansberry
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jperkins@jboss.com">James R. Perkins</a>
 */
public class HostXml extends CommonXml implements ManagementXml.Delegate {

    private final String defaultHostControllerName;

    public HostXml(String defaultHostControllerName) {
        this.defaultHostControllerName = defaultHostControllerName;
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
            case DOMAIN_1_1:
            case DOMAIN_1_2: {
                readHostElement_1_1(readerNS, reader, address, operationList);
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

        boolean hasCoreServices = modelNode.hasDefined(CORE_SERVICE);
        if (hasCoreServices && modelNode.get(CORE_SERVICE).hasDefined(VAULT)) {
            writeVault(writer, modelNode.get(CORE_SERVICE, VAULT));
            writeNewLine(writer);
        }

        if (hasCoreServices && modelNode.get(CORE_SERVICE).hasDefined(MANAGEMENT)) {
            ManagementXml managementXml = new ManagementXml(this);
            managementXml.writeManagement(writer, modelNode.get(CORE_SERVICE, MANAGEMENT), true);
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(DOMAIN_CONTROLLER)) {
            ModelNode ignoredResources = null;
            if (hasCoreServices && modelNode.get(CORE_SERVICE).hasDefined(IGNORED_RESOURCES)
                    && modelNode.get(CORE_SERVICE, IGNORED_RESOURCES).hasDefined(IGNORED_RESOURCE_TYPE)) {
                ignoredResources = modelNode.get(CORE_SERVICE, IGNORED_RESOURCES, IGNORED_RESOURCE_TYPE);
            }
            writeDomainController(writer, modelNode.get(DOMAIN_CONTROLLER), ignoredResources);
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(INTERFACE)) {
            writeInterfaces(writer, modelNode.get(INTERFACE));
            writeNewLine(writer);
        }
        if (modelNode.hasDefined(JVM)) {
            writer.writeStartElement(Element.JVMS.getLocalName());
            for (final Property jvm : modelNode.get(JVM).asPropertyList()) {
                JvmXml.writeJVMElement(writer, jvm.getName(), jvm.getValue());
            }
            writer.writeEndElement();
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(SERVER_CONFIG)) {
            writer.writeStartElement(Element.SERVERS.getLocalName());
            // Write the directory grouping
            HostRootDescription.DIRECTORY_GROUPING.marshallAsAttribute(modelNode, writer);
            writeServers(writer, modelNode.get(SERVER_CONFIG));
            writeNewLine(writer);
            writer.writeEndElement();
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

        // The following also updates the address parameter so this address can be used for future operations
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
        if (element != null) {
            throw unexpectedElement(reader);
        }

    }

    private void readHostElement_1_1(final Namespace namespace, final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list)
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

        // The following also updates the address parameter so this address can be used for future operations
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

        Element element = nextElement(reader, namespace);

        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, namespace, list, false);
            element = nextElement(reader, namespace);
        }
        if (element == Element.PATHS) {
            parsePaths(reader, address, namespace, list, true);
            element = nextElement(reader, namespace);
        }
        if (element == Element.VAULT) {
            parseVault(reader, address, namespace, list);
            element = nextElement(reader, namespace);
        }
        if (element == Element.MANAGEMENT) {
            ManagementXml managementXml = new ManagementXml(this);
            managementXml.parseManagement(reader, address, namespace, list, true, true);
            element = nextElement(reader, namespace);
        } else {
            throw missingRequiredElement(reader, EnumSet.of(Element.MANAGEMENT));
        }
        if (element == Element.DOMAIN_CONTROLLER) {
            parseDomainController(reader, address, namespace, list);
            element = nextElement(reader, namespace);
        }
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, namespace, list, true);
            element = nextElement(reader, namespace);
        }
        if (element == Element.JVMS) {
            parseJvms(reader, address, namespace, list);
            element = nextElement(reader, namespace);
        }
        if (element == Element.SERVERS) {
            parseServers(reader, address, namespace, list);
            element = nextElement(reader, namespace);
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
        address.add(HOST, hostName != null ? hostName : defaultHostControllerName);

        final ModelNode host = new ModelNode();
        host.get(OP).set("add-host");
        host.get(OP_ADDR).set(address.clone());

        if (hostName != null) {
            host.get(NAME).set(hostName);
        }

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
                    switch (expectedNs) {
                        case DOMAIN_1_0:
                            parseRemoteDomainController1_0(reader, address, list);
                            break;
                        default:
                            parseRemoteDomainController1_1(reader, address, expectedNs, list);
                            break;
                    }

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

    private void parseRemoteDomainController1_0(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> list) throws XMLStreamException {
        parseRemoteDomainControllerAttributes(reader, address, list);
        requireNoContent(reader);
    }

    private void parseRemoteDomainController1_1(final XMLExtendedStreamReader reader, final ModelNode address,
            Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {

        parseRemoteDomainControllerAttributes(reader, address, list);

        Set<String> types = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case IGNORED_RESOURCE: {
                    parseIgnoredResource(reader, address, expectedNs, list, types);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseRemoteDomainControllerAttributes(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> list) throws XMLStreamException {
        // Handle attributes
        String host = null;
        ModelNode port = null;
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
                        port = parsePossibleExpression(value);
                        if(port.getType() != ModelType.EXPRESSION) {
                            try {
                                Integer portNo = Integer.valueOf(value);
                                if (portNo.intValue() < 1) {
                                    throw MESSAGES.invalidPort(attribute.getLocalName(), value, reader.getLocation());
                                }
                            }catch(NumberFormatException e) {
                                throw MESSAGES.invalidPort(attribute.getLocalName(), value, reader.getLocation());
                            }
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
    }

    private void parseIgnoredResource(final XMLExtendedStreamReader reader, final ModelNode address,
            Namespace expectedNs, final List<ModelNode> list, final Set<String> foundTypes) throws XMLStreamException {

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);

        String type = null;

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    if (!foundTypes.add(value)) {
                        throw HostControllerMessages.MESSAGES.duplicateIgnoredResourceType(Element.IGNORED_RESOURCE.getLocalName(), value, reader.getLocation());
                    }
                    type = value;
                    break;
                }
                case WILDCARD: {
                    IgnoredDomainTypeResourceDefinition.WILDCARD.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (type == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE.getLocalName()));
        }

        ModelNode addr = op.get(OP_ADDR).set(address);
        addr.add(CORE_SERVICE, IGNORED_RESOURCES);
        addr.add(IGNORED_RESOURCE_TYPE, type);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case INSTANCE: {
                    String name = ParseUtils.readStringAttributeElement(reader, NAME);
                    IgnoredDomainTypeResourceDefinition.NAMES.parseAndAddParameterElement(name, op, reader);
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }

        list.add(op);
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
                    JvmXml.parseJvm(reader, address, expectedNs, list, names);
                    break;
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseServersAttributes(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DIRECTORY_GROUPING: {
                        final ModelNode address = parentAddress.clone();
                        list.add(Util.getWriteAttributeOperation(address, DIRECTORY_GROUPING, value));
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
    }

    private void parseServers(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {
        if (expectedNs != Namespace.DOMAIN_1_1) {
            parseServersAttributes(reader, address, list);
        }
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

                    JvmXml.parseJvm(reader, serverAddress, expectedNs, list, new HashSet<String>(), true);
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

                    JvmXml.parseJvm(reader, serverAddress, expectedNs, list, new HashSet<String>(), true);
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

    private void writeDomainController(final XMLExtendedStreamWriter writer, final ModelNode modelNode, ModelNode ignoredResources)
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
            if (ignoredResources != null) {
                writeIgnoredResources(writer, ignoredResources);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeIgnoredResources(XMLExtendedStreamWriter writer, ModelNode ignoredTypes) throws XMLStreamException {
        for (Property property : ignoredTypes.asPropertyList()) {

            ModelNode ignored = property.getValue();

            ModelNode names = ignored.hasDefined(NAMES) ? ignored.get(NAMES) : null;
            boolean hasNames = names != null && names.asInt() > 0;
            if (hasNames) {
                writer.writeStartElement(Element.IGNORED_RESOURCE.getLocalName());
            } else {
                writer.writeEmptyElement(Element.IGNORED_RESOURCE.getLocalName());
            }

            writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
            IgnoredDomainTypeResourceDefinition.WILDCARD.marshallAsAttribute(ignored, writer);

            if (hasNames) {
                for (ModelNode name : names.asList()) {
                    writer.writeEmptyElement(Element.INSTANCE.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name.asString());
                }
                writer.writeEndElement();
            }
        }
    }

    private void writeServers(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {

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
                    JvmXml.writeJVMElement(writer, jvm.getName(), jvm.getValue());
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
    }

}
