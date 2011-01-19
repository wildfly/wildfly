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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapper between {@code host.xml} and a model.
 *
 * @author Brian Stansberry
 */
public class HostXml extends CommonXml {

    public HostXml(final ModuleLoader loader, final NewExtensionContext context) {
        super(loader, context);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> operationList) throws XMLStreamException {
        final ModelNode address = new ModelNode().setEmptyList();
        if (reader.nextTag() != START_ELEMENT) {
            throw unexpectedEndElement(reader);
        }
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0 || Element.forName(reader.getLocalName()) != Element.HOST) {
            throw unexpectedElement(reader);
        }
        readHostElement(reader, address, operationList);
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        writeNamespaces(writer, modelNode);
        writeSchemaLocation(writer, modelNode);
        writeExtensions(writer, modelNode.get(EXTENSION));
        if(modelNode.has("path")) {
            writePaths(writer, modelNode.get("path"));
        }
    }

    private void readHostElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        parseNamespaces(reader, address, list);

        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case NONE: {
                    final String value = reader.getAttributeValue(i);
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            setHostName(address, list, value);
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

        // Content
        // Handle elements: sequence

        Element element = nextElement(reader);

        if (element == Element.EXTENSIONS) {
            parseExtensions(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.PATHS) {
            parsePaths(reader, address, list, true);
            element = nextElement(reader);
        }
        if (element == Element.SYSTEM_PROPERTIES) {
            list.add(getWriteAttributeOperation(address, "system-properties", parseProperties(reader)));
            element = nextElement(reader);
        }
        if (element == Element.MANAGEMENT) {
            parseManagementSocket(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.DOMAIN_CONTROLLER) {
            parseDomainController(reader, address, list);
            element = nextElement(reader);
        }
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, list, true);
            element = nextElement(reader);
        }
        if (element == Element.JVMS) {
            parseJvms(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.SERVERS) {
            parseServers(reader, address, list);
            element = nextElement(reader);
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

    private void setHostName(final ModelNode address, final List<ModelNode> operationList, final String value) {
        final ModelNode update = getWriteAttributeOperation(address, NAME, value);
        operationList.add(update);
    }

    private void parseDomainController(final XMLExtendedStreamReader reader,
            final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        requireNoAttributes(reader);

        boolean hasLocal = false;
        boolean hasRemote = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOCAL: {
                            if (hasLocal) {
                                throw new XMLStreamException("Child " + element.getLocalName() +
                                        " of element " + Element.DOMAIN_CONTROLLER.getLocalName() +
                                        " already declared", reader.getLocation());
                            }
                            else if (hasRemote) {
                                throw new XMLStreamException("Child " + Element.REMOTE.getLocalName() +
                                        " of element " + Element.DOMAIN_CONTROLLER.getLocalName() +
                                        " already declared", reader.getLocation());
                            }
                            requireNoAttributes(reader);
                            requireNoContent(reader);
                            hasLocal = true;
                            break;
                        }
                        case REMOTE: {
                            if (hasRemote) {
                                throw new XMLStreamException("Child " + element.getLocalName() +
                                        " of element " + Element.DOMAIN_CONTROLLER.getLocalName() +
                                        " already declared", reader.getLocation());
                            }
                            else if (hasLocal) {
                                throw new XMLStreamException("Child " + Element.LOCAL.getLocalName() +
                                        " of element " + Element.DOMAIN_CONTROLLER.getLocalName() +
                                        " already declared", reader.getLocation());
                            }
                            parseRemoteDomainController(reader, address, list);
                            hasRemote = true;
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        if (!hasLocal && !hasRemote) {
            throw new XMLStreamException("Either a " + Element.REMOTE.getLocalName() + " or " +
                    Element.LOCAL.getLocalName() + " domain controller configuration must be declared.", reader.getLocation());
        }

        if (hasLocal) {
            final ModelNode update = new ModelNode();
            update.get(OP_ADDR).set(address);
            update.get(OP).set("write-local-domain-controller");
            list.add(update);
        }
    }

    private void parseRemoteDomainController(final XMLExtendedStreamReader reader,
            final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        // Handle attributes
        String host = null;
        Integer port = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
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
                            throw new XMLStreamException("Illegal '" + attribute.getLocalName() +
                                    "' value " + port + " -- cannot be less than one",
                                    reader.getLocation());
                        }
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if(host == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.HOST.getLocalName()));
        }
        if(port == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PORT.getLocalName()));
        }

        final ModelNode update = new ModelNode();
        update.get(OP_ADDR).set(address);
        update.get(OP).set("write-remote-domain-controller");
        update.get("host-name").set(host);
        update.get("port").set(port);
        list.add(update);

        reader.discardRemainder();
    }

    private void parseJvms(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case JVM:
                            parseJvm(reader, address, list, names);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }


    private void parseServers(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        requireNoAttributes(reader);
        // Handle elements
        final Set<String> names = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SERVER:
                            parseServer(reader, address, list, names);
                            break;
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    private void parseServer(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list, final Set<String> serverNames) throws XMLStreamException {
        // Handle attributes
        String name = null;
        String group = null;
        Boolean start = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
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
                    case START: {
                        start = Boolean.valueOf(value);
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (group == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.GROUP));
        }

        final ModelNode address = parentAddress.clone().add(ModelDescriptionConstants.SERVER, name);
        final ModelNode addUpdate = new ModelNode();
        addUpdate.get(OP_ADDR).set(address);
        addUpdate.get(OP).set(ADD);
        addUpdate.get("name").set(name);
        addUpdate.get("group").set(group);
        list.add(addUpdate);

        // Handle elements
        boolean sawJvm = false;
        boolean sawSystemProperties = false;
        boolean sawSocketBinding = false;
        final Set<String> interfaceNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INTERFACE_SPECS: {
                            parseInterfaces(reader, interfaceNames, address, list, true);
                            break;
                        }
                        case JVM: {
                            if (sawJvm) {
                                throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                            }

                            parseJvm(reader, address, list, new HashSet<String>());
                            sawJvm = true;
                            break;
                        }
                        case PATHS : {
                            parsePaths(reader, address, list, true);
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
                            list.add(getWriteAttributeOperation(address, "system-properties", parseProperties(reader)));
                            sawSystemProperties = true;
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }

        final boolean isStart = start == null ? true : start.booleanValue();
        final ModelNode startUpdate = getWriteAttributeOperation(address, "start", isStart);
        list.add(startUpdate);
    }
}
