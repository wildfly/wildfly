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

package org.jboss.as.server.parsing;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NATIVE_REMOTING_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
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
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.CommonXml;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.server.mgmt.HttpManagementResourceDefinition;
import org.jboss.as.server.mgmt.NativeManagementResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapper between an AS server's configuration model and XML representations, particularly {@code standalone.xml}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class StandaloneXml extends CommonXml {

    public StandaloneXml(final ModuleLoader loader, final ExecutorService executorService) {
        super(loader, executorService);
    }

    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operationList)
            throws XMLStreamException {

        long start = System.currentTimeMillis();
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

        if (ROOT_LOGGER.isDebugEnabled()) {
            long elapsed = System.currentTimeMillis() - start;
            ROOT_LOGGER.debugf("Parsed standalone configuration in [%d] ms", elapsed);
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

        if (element == Element.MANAGEMENT) {
            parseManagement(reader, address, DOMAIN_1_0, list, true, false);
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
            parseSocketBindingGroup_1_0(reader, interfaceNames, address, DOMAIN_1_0, list);
            element = nextElement(reader, DOMAIN_1_0);
        }
        if (element == Element.DEPLOYMENTS) {
            parseDeployments(reader, address, DOMAIN_1_0, list, true);
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

        if (element == Element.MANAGEMENT) {
            parseManagement(reader, address, DOMAIN_1_1, list, true, false);
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
            parseSocketBindingGroup_1_1(reader, interfaceNames, address, DOMAIN_1_1, list);
            element = nextElement(reader, DOMAIN_1_1);
        }
        if (element == Element.DEPLOYMENTS) {
            parseDeployments(reader, address, DOMAIN_1_1, list, true);
            element = nextElement(reader, DOMAIN_1_1);
        }

        if (element != null) {
            throw unexpectedElement(reader);
        }
    }


        // for (;;) {
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

    protected void parseManagementInterfaces(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {

        switch (expectedNs) {
            case DOMAIN_1_0:
                parseManagementInterfaces_1_0(reader, address, expectedNs, list);
                break;
            default:
                parseManagementInterfaces_1_1(reader, address, expectedNs, list);
        }
    }

    private void parseManagementInterfaces_1_0(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> list) throws XMLStreamException {

        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NATIVE_INTERFACE: {
                    parseNativeManagementInterface1_0(reader, address, list);
                    break;
                }
                case HTTP_INTERFACE: {
                    parseHttpManagementInterface1_0(reader, address, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
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

    private void parseManagementInterfaces_1_1(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
                                                 final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NATIVE_INTERFACE: {
                    parseManagementInterface1_1(reader, address, false, expectedNs, list);
                    break;
                }
                case HTTP_INTERFACE: {
                    parseManagementInterface1_1(reader, address, true, expectedNs, list);
                    break;
                }
                case NATIVE_REMOTING_INTERFACE: {
                    parseNativeRemotingManagementInterface(reader, address, list);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
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
                case SOCKET_BINDING:
                    if (http) {
                        parseHttpManagementSocketBinding(reader, addOp);
                    } else {
                        parseNativeManagementSocketBinding(reader, addOp);
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

    private void parseHttpManagementSocketBinding(XMLExtendedStreamReader reader, ModelNode addOp) throws XMLStreamException {

        // Handle attributes

        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case HTTP: {
                       HttpManagementResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case HTTPS: {
                       HttpManagementResourceDefinition.SECURE_SOCKET_BINDING.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        requireNoContent(reader);
    }

    private void parseNativeManagementSocketBinding(XMLExtendedStreamReader reader, ModelNode addOp) throws XMLStreamException {

        // Handle attributes
        boolean hasRef = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NATIVE: {
                        NativeManagementResourceDefinition.SOCKET_BINDING.parseAndSetParameter(value, addOp, reader);
                        hasRef = true;
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (!hasRef) {
            throw missingRequired(reader, Collections.singleton(Attribute.REF.getLocalName()));
        }

        requireNoContent(reader);
    }

    private void parseNativeRemotingManagementInterface(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> list) throws XMLStreamException {

        requireNoAttributes(reader);
        //requireNoContent(reader);

        final ModelNode connector = new ModelNode();
        connector.get(OP).set(ADD);
        ModelNode operationAddress = address.clone();
        operationAddress.add(MANAGEMENT_INTERFACE, NATIVE_REMOTING_INTERFACE);
        connector.get(OP_ADDR).set(operationAddress);
        list.add(connector);

        reader.discardRemainder();
    }

    private void parseSocketBindingGroup_1_0(final XMLExtendedStreamReader reader, final Set<String> interfaces,
            final ModelNode address, final Namespace expectedNs, final List<ModelNode> updates) throws XMLStreamException {

        // unique names socket-binding(s)
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
                        throw MESSAGES.alreadyDeclared(Element.SOCKET_BINDING.getLocalName(), bindingName,
                                Element.SOCKET_BINDING_GROUP.getLocalName(), socketBindingGroupName, reader.getLocation());
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseSocketBindingGroup_1_1(final XMLExtendedStreamReader reader, final Set<String> interfaces,
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
                        throw MESSAGES.alreadyDeclared(Element.SOCKET_BINDING.getLocalName(), Element.OUTBOUND_SOCKET_BINDING.getLocalName(),
                                bindingName, Element.SOCKET_BINDING_GROUP.getLocalName(), socketBindingGroupName, reader.getLocation());
                    }
                    break;
                }
                case OUTBOUND_SOCKET_BINDING: {
                    final String bindingName = parseOutboundSocketBinding(reader, interfaces, socketBindingGroupName, groupAddress, updates);
                    if (!uniqueBindingNames.add(bindingName)) {
                        throw MESSAGES.alreadyDeclared(Element.SOCKET_BINDING.getLocalName(), Element.OUTBOUND_SOCKET_BINDING.getLocalName(),
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
                throw MESSAGES.duplicateDeclaration("subsystem", reader.getLocation());
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

        writeNewLine(writer);

        if (modelNode.hasDefined(EXTENSION)) {
            writeExtensions(writer, modelNode.get(EXTENSION));
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(SYSTEM_PROPERTY)) {
            writeProperties(writer, modelNode.get(SYSTEM_PROPERTY), Element.SYSTEM_PROPERTIES, true);
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
            writeManagement(writer, modelNode.get(CORE_SERVICE, MANAGEMENT), true);
            writeNewLine(writer);
        }

        writeServerProfile(writer, context);
        writeNewLine(writer);

        if (modelNode.hasDefined(INTERFACE)) {
            writeInterfaces(writer, modelNode.get(INTERFACE));
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(SOCKET_BINDING_GROUP)) {
            Set<String> groups = modelNode.get(SOCKET_BINDING_GROUP).keys();
            if (groups.size() > 1) {
                throw MESSAGES.multipleModelNodes(SOCKET_BINDING_GROUP);
            }
            for (String group : groups) {
                writeSocketBindingGroup(writer, modelNode.get(SOCKET_BINDING_GROUP, group), true);
            }
            writeNewLine(writer);
        }

        if (modelNode.hasDefined(DEPLOYMENT)) {
            writeServerDeployments(writer, modelNode.get(DEPLOYMENT));
            writeNewLine(writer);
        }
        writer.writeEndElement();
        writeNewLine(writer);
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

    protected void writeNativeManagementProtocol(final XMLExtendedStreamWriter writer, final ModelNode protocol)
            throws XMLStreamException {

        writer.writeStartElement(Element.NATIVE_INTERFACE.getLocalName());
        NativeManagementResourceDefinition.SECURITY_REALM.marshallAsAttribute(protocol, writer);

        if (NativeManagementResourceDefinition.INTERFACE.isMarshallable(protocol)) {
            writer.writeEmptyElement(Element.SOCKET.getLocalName());
            NativeManagementResourceDefinition.INTERFACE.marshallAsAttribute(protocol, writer);
            NativeManagementResourceDefinition.NATIVE_PORT.marshallAsAttribute(protocol, writer);
        } else if (NativeManagementResourceDefinition.SOCKET_BINDING.isMarshallable(protocol)) {
            writer.writeEmptyElement(Element.SOCKET_BINDING.getLocalName());
            NativeManagementResourceDefinition.SOCKET_BINDING.marshallAsAttribute(protocol, writer);
        }

        writer.writeEndElement();
    }

    protected void writeHttpManagementProtocol(final XMLExtendedStreamWriter writer, final ModelNode protocol)
            throws XMLStreamException {

        writer.writeStartElement(Element.HTTP_INTERFACE.getLocalName());
        HttpManagementResourceDefinition.SECURITY_REALM.marshallAsAttribute(protocol, writer);

        if (HttpManagementResourceDefinition.INTERFACE.isMarshallable(protocol)) {
            writer.writeEmptyElement(Element.SOCKET.getLocalName());
            HttpManagementResourceDefinition.INTERFACE.marshallAsAttribute(protocol, writer);
            HttpManagementResourceDefinition.HTTP_PORT.marshallAsAttribute(protocol, writer);
            HttpManagementResourceDefinition.HTTPS_PORT.marshallAsAttribute(protocol, writer);
        } else if (HttpManagementResourceDefinition.SOCKET_BINDING.isMarshallable(protocol)
                || HttpManagementResourceDefinition.SECURE_SOCKET_BINDING.isMarshallable(protocol)) {
            writer.writeEmptyElement(Element.SOCKET_BINDING.getLocalName());
            HttpManagementResourceDefinition.SOCKET_BINDING.marshallAsAttribute(protocol, writer);
            HttpManagementResourceDefinition.SECURE_SOCKET_BINDING.marshallAsAttribute(protocol, writer);
        }

        writer.writeEndElement();
    }

}
