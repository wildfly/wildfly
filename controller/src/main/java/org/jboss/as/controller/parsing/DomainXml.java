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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.parsing.ParseUtils.nextElement;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * A mapper between {@code domain.xml} and a model.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainXml extends CommonXml {

    public DomainXml(final ModuleLoader loader) {
        super(loader);
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> nodes) throws XMLStreamException {
        readDomainElement(reader, new ModelNode(), nodes);
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

    void readDomainElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {

        parseNamespaces(reader, address, list);

        // attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            switch (Namespace.forUri(reader.getAttributeNamespace(i))) {
                case DOMAIN_1_0: {
                    throw unexpectedAttribute(reader, i);
                } case XML_SCHEMA_INSTANCE: {
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
        if (element == Element.PROFILES) {
            parseProfiles(reader, address, list);
            element = nextElement(reader);
        }
        final Set<String> interfaceNames = new HashSet<String>();
        if (element == Element.INTERFACES) {
            parseInterfaces(reader, interfaceNames, address, list, false);
            element = nextElement(reader);
        }
        if (element == Element.SOCKET_BINDING_GROUPS) {
            parseDomainSocketBindingGroups(reader, address, list, interfaceNames);
            element = nextElement(reader);
        }
        if (element == Element.SYSTEM_PROPERTIES) {
            parseSystemProperties(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.DEPLOYMENTS) {
            parseDeployments(reader, address, list);
            element = nextElement(reader);
        }
        if (element == Element.SERVER_GROUPS) {
            parseServerGroups(reader, address, list);
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

    void parseDomainSocketBindingGroups(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> interfaces) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case SOCKET_BINDING_GROUP: {
                            // parse binding-group
                            parseSocketBindingGroup(reader, interfaces, address, list);
                            break;
                        } default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    void parseSocketBindingGroup(final XMLExtendedStreamReader reader, final Set<String> interfaces, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        final Set<String> includedGroups = new HashSet<String>();
        final Set<String> socketBindings = new HashSet<String>();

        // Handle attributes
        final String[] attrValues = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.DEFAULT_INTERFACE.getLocalName());
        final String name = attrValues[0];
        final String defaultInterface = attrValues[1];

        final ModelNode bindingGroupUpdate = new ModelNode();
        bindingGroupUpdate.get(OP_ADDR).set(address).add(SOCKET_BINDING_GROUP, name);
        bindingGroupUpdate.get(OP).set(ADD);
        bindingGroupUpdate.get(DEFAULT_INTERFACE).set(defaultInterface);
        final ModelNode includes = bindingGroupUpdate.get(INCLUDE);
        includes.setEmptyList();

        // Handle elements
        while (reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INCLUDE: {
                            final String includedGroup = readStringAttributeElement(reader, Attribute.SOCKET_BINDING_GROUP.getLocalName());
                            if (!includedGroups.add(includedGroup)) {
                                throw new XMLStreamException("Included socket-binding-group " + includedGroup + " already declared", reader.getLocation());
                            }
                            includes.add(includedGroup);
                            break;
                        }
                        case SOCKET_BINDING: {
                            final String bindingName = parseSocketBinding(reader, interfaces, address, defaultInterface, updates);
                            if (!socketBindings.add(bindingName)) {
                                throw new XMLStreamException("socket-binding " + bindingName + " already declared", reader.getLocation());
                            }
                            break;
                        }
                        default:
                            throw unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
        updates.add(bindingGroupUpdate);
    }

    void parseServerGroups(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {

            String name = null;
            String profile = null;

            // Handle attributes
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            if (name != null) {
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            }
                            if (!names.add(value)) {
                                throw ParseUtils.duplicateNamedElement(reader, value);
                            }
                            name = value;
                            break;
                        }
                        case PROFILE: {
                            if (profile != null) {
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            }
                            profile = value;
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            if (profile == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PROFILE));
            }

            final ModelNode group = new ModelNode();
            group.get(OP).set(ADD);
            group.get(OP_ADDR).set(address).add(ModelDescriptionConstants.SERVER_GROUP, name);
            group.get(REQUEST_PROPERTIES, PROFILE).set(profile);
            list.add(group);
            // list.add(new DomainServerGroupAdd(name, profile));

            // Handle elements

            boolean sawDeployments = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DOMAIN_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case JVM: {
                                parseJvm(reader, address, list, new HashSet<String>());
                                break;
                            }
                            case SOCKET_BINDING_GROUP: {
                                parseSocketBindingGroupRef(reader, address, list);
                                break;
                            }
                            case DEPLOYMENTS: {
                                if (sawDeployments) {
                                    throw new XMLStreamException(element.getLocalName() + " already defined", reader.getLocation());
                                }
                                sawDeployments = true;
                                parseDeployments(reader, address, list);
                                break;
                            }
                            case SYSTEM_PROPERTIES: {
                                parseSystemProperties(reader, address, list);
                                break;
                            }
                            default:
                                throw ParseUtils.unexpectedElement(reader);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    void parseDeployments(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
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
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                    switch (attribute) {
                        case NAME: {
                            if (!names.add(value)) {
                                throw ParseUtils.duplicateNamedElement(reader, value);
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
                                hash = ParseUtils.hexStringToByteArray(value);
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
                            if (! Boolean.parseBoolean(value)) {
                                throw new XMLStreamException("Attribute '" + attribute.getLocalName() + "' is not allowed", reader.getLocation());
                            }
                            break;
                        }
                        case START: {
                            startInput = value;
                            break;
                        }
                        default:
                            throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (uniqueName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            if (runtimeName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.RUNTIME_NAME));
            }
            if (hash == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SHA1));
            }
            final boolean toStart = startInput == null ? true : Boolean.parseBoolean(startInput);

            // Handle elements
            ParseUtils.requireNoContent(reader);

            final ModelNode deploymentAdd = new ModelNode();
            // TODO decide whether deployments are an attribute of list type or a child
//          deploymentAdd.get(OP_ADDR).set(address).add(DEPLOYMENT, uniqueName);
//          deploymentAdd.get(OP).set(ADD);
            deploymentAdd.get(OP_ADDR).set(address);
            deploymentAdd.get(OP).set("add-deployment");
            deploymentAdd.get(REQUEST_PROPERTIES, "unique-name").set(uniqueName);
            deploymentAdd.get(REQUEST_PROPERTIES, "runtime-name").set(runtimeName);
            deploymentAdd.get(REQUEST_PROPERTIES, "sha1").set(hash);
            deploymentAdd.get(REQUEST_PROPERTIES, "start").set(toStart);
            list.add(deploymentAdd);
        }
    }

    void parseProfiles(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            // Attributes
            requireSingleAttribute(reader, Attribute.NAME.getLocalName());
            final String name = reader.getAttributeValue(0);
            if (! names.add(name)) {
                throw new XMLStreamException("Duplicate profile declaration " + name, reader.getLocation());
            }

            final List<ModelNode> subsystems = new ArrayList<ModelNode>();
            final Set<String> includes = new HashSet<String>();
            final ModelNode profileIncludes = new ModelNode();

            // Content
            // Sequence
            final Set<String> configuredSubsystemTypes = new HashSet<String>();
            while (reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case UNKNOWN: {
                        if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
                            throw unexpectedElement(reader);
                        }
                        if (!configuredSubsystemTypes.add(reader.getNamespaceURI())) {
                            throw new XMLStreamException("Duplicate subsystem declaration", reader.getLocation());
                        }
                        // parse content
                        reader.handleAny(subsystems);

                        break;
                    }
                    case DOMAIN_1_0: {
                        // include should come first
                        if (configuredSubsystemTypes.size() > 0) {
                            throw unexpectedElement(reader);
                        }
                        if (Element.forName(reader.getLocalName()) != Element.INCLUDE) {
                            throw unexpectedElement(reader);
                        }
                        final String includedName = readStringAttributeElement(reader, Attribute.PROFILE.getLocalName());
                        if (! names.contains(includedName)) {
                            throw new XMLStreamException("No profile found for inclusion", reader.getLocation());
                        }
                        if (! includes.add(includedName)) {
                            throw new XMLStreamException("Duplicate profile include", reader.getLocation());
                        }
                        profileIncludes.add(includedName);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
            final ModelNode profile = new ModelNode();
            profile.get(OP).set(ADD);
            profile.get(OP_ADDR).set(address).add(ModelDescriptionConstants.PROFILE, name);
            profile.get(REQUEST_PROPERTIES, "includes").set(profileIncludes);
            list.add(profile);

            // Process subsystems
            for(final ModelNode update : subsystems) {
                // Process relative subsystem path address
                final ModelNode subsystemAddress = address.clone().set(address).set(ModelDescriptionConstants.PROFILE, name);
                for(final Property path : update.get(OP_ADDR).asPropertyList()) {
                    subsystemAddress.add(path.getName(), path.getValue().asString());
                }
                update.get(OP_ADDR).set(subsystemAddress);
                list.add(update);
            }

            if (configuredSubsystemTypes.size() == 0) {
                throw new XMLStreamException("Profile has no subsystem configurations", reader.getLocation());
            }
        }

    }
}
