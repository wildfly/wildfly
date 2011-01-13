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
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.hexStringToByteArray;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.nextElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedEndElement;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtensionContext;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapper between {@code standalone.xml} and a model.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class StandaloneXml extends CommonXml {

    public StandaloneXml(final ModuleLoader loader, final NewExtensionContext context) {
        super(loader, context);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operationList) throws XMLStreamException {
        final ModelNode address = new ModelNode().setEmptyList();
        if (reader.nextTag() != START_ELEMENT) {
            throw unexpectedEndElement(reader);
        }
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0 || Element.forName(reader.getLocalName()) != Element.SERVER) {
            throw unexpectedElement(reader);
        }
        readServerElement(reader, address, operationList);
    }

    private void readServerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case NONE: {
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            setServerName(address, list, value);
                            break;
                        }
                        default: throw unexpectedAttribute(reader, i);
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
                default: throw unexpectedAttribute(reader, i);
            }
        }

        // elements - sequence

        Element element = nextElement(reader);
        if (element == Element.EXTENSIONS) {
            parseExtensions(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.PATHS) {
            parsePaths(reader, address, list, true);
            element = nextElement(reader);
        }
        if (element == Element.MANAGEMENT) {
            parseServerManagement(reader, address, list);
            element = nextElement(reader);
        }
        // Single profile
        if (element == Element.PROFILE) {
            parseServerProfile(reader, list);
            element = nextElement(reader);
        }
        // Interfaces
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, list, true);
            element = nextElement(reader);
        }
        // Single socket binding group
        if (element == Element.SOCKET_BINDING_GROUP) {
            parseSocketBindingGroup(reader, interfaceNames, address, list, false);
            element = nextElement(reader);
        }
        // System properties
        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.DEPLOYMENTS) {
            parseServerDeployments(reader, list);
            element = nextElement(reader);
        }
        if (element != null) {
            throw unexpectedElement(reader);
        }

        for (;;) {
            switch (reader.nextTag()) {
                case START_ELEMENT: {
                    readHeadComment(reader, address, list);
                    if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
                        throw unexpectedElement(reader);
                    }
                    switch (Element.forName(reader.getLocalName())) {
                        default: throw unexpectedElement(reader);
                    }
                }
                case END_ELEMENT: {
                    readTailComment(reader, address, list);
                    return;
                }
                default: throw new IllegalStateException();
            }
        }
    }

    private void parseServerDeployments(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            // Handle attributes
            String uniqueName = null;
            String runtimeName = null;
            byte[] hash = null;
            String startInput = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            if (!names.add(value)) {
                                throw duplicateNamedElement(reader, value);
                            }
                            uniqueName = value;
                            break;
                        }
                        case RUNTIME_NAME: {
                            runtimeName = value;
                            break;
                        }
                        case SHA1: {
                            try {
                                hash = hexStringToByteArray(value);
                            }
                            catch (final Exception e) {
                               throw new XMLStreamException("Value " + value +
                                       " for attribute " + attribute.getLocalName() +
                                       " does not represent a properly hex-encoded SHA1 hash",
                                       reader.getLocation(), e);
                            }
                            break;
                        }
                        case ALLOWED: {
                            if (!Boolean.parseBoolean(value)) {
                                throw new XMLStreamException("Attribute '" + attribute.getLocalName() + "' is not allowed", reader.getLocation());
                            }
                            break;
                        }
                        case START: {
                            startInput = value;
                            break;
                        }
                        default:
                            throw unexpectedAttribute(reader, i);
                    }
                }
            }
            if (uniqueName == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            if (runtimeName == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.RUNTIME_NAME));
            }
            if (hash == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.SHA1));
            }
            final boolean toStart = startInput == null ? true : Boolean.parseBoolean(startInput);

            // Handle elements
            requireNoContent(reader);

            final ModelNode deploymentAdd = new ModelNode();
            deploymentAdd.get("address").setEmptyList();
            deploymentAdd.get("operation").set("add-deployment");
            deploymentAdd.get("unique-name").set(uniqueName);
            deploymentAdd.get("runtime-name").set(runtimeName);
            deploymentAdd.get("sha1").set(hash);
            deploymentAdd.get("start").set(toStart);
            list.add(deploymentAdd);
        }
    }

    private void parseServerProfile(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // Attributes
        requireNoAttributes(reader);

        // Content
        final Set<String> configuredSubsystemTypes = new HashSet<String>();
        while (reader.nextTag() != END_ELEMENT) {
            if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.UNKNOWN) {
                throw unexpectedElement(reader);
            }
            if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                throw unexpectedElement(reader);
            }
            if (!configuredSubsystemTypes.add(reader.getNamespaceURI())) {
                throw new XMLStreamException("Duplicate subsystem declaration", reader.getLocation());
            }
            // parse content
            reader.handleAny(list);
        }
    }

    private void parseServerManagement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        // Handle attributes
        String interfaceName = null;
        int port = 0;
        int maxThreads = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case INTERFACE: {
                        interfaceName = value;
                        break;
                    }
                    case PORT: {
                        port = Integer.parseInt(value);
                        if (port < 0) {
                            throw new XMLStreamException("Illegal '" + attribute.getLocalName() +
                                    "' value " + port + " -- cannot be negative",
                                    reader.getLocation());
                        }
                        break;
                    }
                    case MAX_THREADS: {
                        maxThreads = Integer.parseInt(value);
                        if (maxThreads < 1) {
                            throw new XMLStreamException("Illegal '" + attribute.getLocalName() +
                                    "' value " + maxThreads + " -- must be greater than 0",
                                    reader.getLocation());
                        }
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (interfaceName == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.INTERFACE.getLocalName()));
        }
        final ModelNode addServerMgmt = new ModelNode();
        addServerMgmt.get("address").set(address);
        addServerMgmt.get("operation").set("add-server-management");
        addServerMgmt.get("interface-name").set(interfaceName);
        addServerMgmt.get("port").set(port);
        list.add(addServerMgmt);

        if (maxThreads > 0) {
            // TODO - this is non-optimal.
            final ModelNode setSocketThreads = new ModelNode();
            setSocketThreads.get("address").set(address);
            setSocketThreads.get("operation").set("set-server-management-socket-threads");
            setSocketThreads.get("max-threads").set(maxThreads);
            list.add(setSocketThreads);
        }
        reader.discardRemainder();

    }

    private void setServerName(final ModelNode address, final List<ModelNode> operationList, final String value) {
        final ModelNode update = new ModelNode();
        update.get("address").set(address);
        update.get("operation").set("set-server-name");
        update.get("server-name").set(value);
        operationList.add(update);
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        writeNamespaces(writer, modelNode);
        writeSchemaLocation(writer, modelNode);
    }
}
