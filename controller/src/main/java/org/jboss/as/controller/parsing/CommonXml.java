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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.jboss.as.controller.parsing.ParseUtils.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class CommonXml implements XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

    /** The restricted path names. */
    protected static final Set<String> RESTRICTED_PATHS;

    static {
        final HashSet<String> set = new HashSet<String>(10);
        // Define the restricted path names.
        set.add("jboss.home");
        set.add("jboss.home.dir");
        set.add("user.home");
        set.add("user.dir");
        set.add("java.home");
        set.add("jboss.server.base.dir");
        set.add("jboss.server.data.dir");
        set.add("jboss.server.log.dir");
        set.add("jboss.server.tmp.dir");
        // NOTE we actually don't create services for the following
        // however the names remain restricted for use in the configuration
        set.add("jboss.modules.dir");
        set.add("jboss.server.deploy.dir");
        set.add("jboss.domain.servers.dir");
        RESTRICTED_PATHS = Collections.unmodifiableSet(set);
    }

    protected final ModuleLoader moduleLoader;
    protected final NewExtensionContext extensionContext;

    protected CommonXml(final ModuleLoader loader, final NewExtensionContext context) {
        moduleLoader = loader;
        extensionContext = context;
    }

    protected void parseNamespaces(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes) {
        final int namespaceCount = reader.getNamespaceCount();
        for (int i = 0; i < namespaceCount; i ++) {
            final ModelNode operation = new ModelNode();
            operation.get("address").set(address);
            operation.get("operation").add("add-namespace");
            operation.get("namespace").add(reader.getNamespacePrefix(i), reader.getNamespaceURI(i));
            nodes.add(operation);
        }
    }

    @SuppressWarnings("unused")
    protected void readHeadComment(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes) throws XMLStreamException {
        // TODO STXM-6
    }

    @SuppressWarnings("unused")
    protected void readTailComment(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes) throws XMLStreamException {
        // TODO STXM-6
    }

    protected void parseSchemaLocations(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updateList, final int idx) throws XMLStreamException {
        final List<String> values = reader.getListAttributeValue(idx);
        if ((values.size() & 1) != 0) {
            throw invalidAttributeValue(reader, idx);
        }
        final Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            final ModelNode update = new ModelNode();
            update.get("address").set(address);
            update.get("operation").set("add-schema-location");
            update.get("namespace-uri").set(it.next());
            update.get("location-uri").set(it.next());
            updateList.add(update);
        }
    }

    protected void writeSchemaLocation(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        final StringBuilder b = new StringBuilder();
        final Iterator<ModelNode> iterator = modelNode.get("schema-location").asList().iterator();
        while (iterator.hasNext()) {
            final ModelNode location = iterator.next();
            final Property property = location.asProperty();
            b.append(property.getName()).append(' ').append(property.getValue().asString());
            if (iterator.hasNext()) {
                b.append(' ');
            }
        }
        if (b.length() > 0) {
            writer.writeAttribute(Namespace.XML_SCHEMA_INSTANCE.getUriString(), Attribute.SCHEMA_LOCATION.getLocalName(), b.toString());
        }
    }

    protected void writeNamespaces(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        for (ModelNode namespace : modelNode.get("namespace").asList()) {
            final Property property = namespace.asProperty();
            writer.writeNamespace(property.getName(), property.getValue().asString());
        }
    }

    protected void parseExtensions(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> found = new HashSet<String>();

        final ExtensionParsingContextImpl context = new ExtensionParsingContextImpl(reader.getXMLMapper());

        while (reader.nextTag() != END_ELEMENT) {
            // Attribute && require no content
            final String moduleName = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());

            if (! found.add(moduleName)) {
                // duplicate module name
                throw invalidAttributeValue(reader, 0);
            }

            // Register element handlers for this extension
            try {
                Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
                boolean initialized = false;
                for (NewExtension extension : module.loadService(NewExtension.class)) {
                    extension.initializeParsers(context);
                    if (!initialized) {
                        initialized = true;
                    }
                }
                if (!initialized) {
                    throw new IllegalStateException("No META-INF/services/" + NewExtension.class.getName() + " found for " + module.getIdentifier());
                }
                final ModelNode add = new ModelNode();
                add.get("address").set(address).add("extension", moduleName);
                add.get("operation").set("add-extension");
                list.add(add);
            } catch (ModuleLoadException e) {
                throw new XMLStreamException("Failed to load module", e);
            }
        }
    }

    protected void parseInterfaceCriteria(final XMLExtendedStreamReader reader, final ModelNode criteria) throws XMLStreamException {
        // all subsequent elements are criteria elements
        if (reader.nextTag() == END_ELEMENT) {
            return;
        }
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
            throw unexpectedElement(reader);
        }
        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case ANY_ADDRESS:
            case ANY_IPV4_ADDRESS:
            case ANY_IPV6_ADDRESS: {
                criteria.set(element.getLocalName());
                requireNoContent(reader); // consume this element
                requireNoContent(reader); // consume rest of criteria (no further content allowed)
                return;
            }
        }
        do {
            switch (element) {
                case ANY: parseCompoundInterfaceCriterion(reader, criteria.add().set("any", new ModelNode()).get("any")); break;
                case NOT: parseCompoundInterfaceCriterion(reader, criteria.add().set("not", new ModelNode()).get("not")); break;
                default: {
                    parseSimpleInterfaceCriterion(reader, criteria.add().set(element.getLocalName(), new ModelNode()).get(element.getLocalName()));
                    break;
                }
            }
        } while (reader.nextTag() != END_ELEMENT);
    }

    protected void parseCompoundInterfaceCriterion(final XMLExtendedStreamReader reader, final ModelNode criterion) throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.nextTag() != END_ELEMENT) {
            parseSimpleInterfaceCriterion(reader, criterion.add());
        }
    }

    /**
     * Creates the appropriate AbstractInterfaceCriteriaElement for simple criterion.
     *
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    protected void parseSimpleInterfaceCriterion(XMLExtendedStreamReader reader, ModelNode criteria) throws XMLStreamException {
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
            throw unexpectedElement(reader);
        }
        Element element = Element.forName(reader.getLocalName());
        final String localName = element.getLocalName();
        switch (element) {
            case INET_ADDRESS: {
                requireSingleAttribute(reader, Attribute.VALUE.getLocalName());
                String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate IP address
                criteria.add(localName, value);
                break;
            }
            case LINK_LOCAL_ADDRESS:
            case LOOPBACK:
            case MULTICAST:
            case POINT_TO_POINT:
            case PUBLIC_ADDRESS:
            case SITE_LOCAL_ADDRESS:
            case UP:
            case VIRTUAL: {
                criteria.add(localName);
                break;
            }
            case NIC: {
                requireSingleAttribute(reader, Attribute.NAME.getLocalName());
                String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate NIC name
                criteria.add(localName, value);
                break;
            }
            case NIC_MATCH: {
                requireSingleAttribute(reader, Attribute.PATTERN.getLocalName());
                String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate pattern
                criteria.add(localName, value);
                break;
            }
            case SUBNET_MATCH: {
                requireSingleAttribute(reader, Attribute.VALUE.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);

                final String[] split = value.split("/");
                try {
                    if (split.length != 2) {
                        throw new XMLStreamException("Invalid 'value' " + value + " -- must be of the form address/mask", reader.getLocation());
                    }
                    // todo - possible DNS hit here
                    final InetAddress addr = InetAddress.getByName(split[1]);
                    final byte[] net = addr.getAddress();
                    final int mask = Integer.parseInt(split[1]);
                    final ModelNode node = criteria.add().set(localName, new ModelNode()).get(localName);
                    node.get("network").set(net);
                    node.get("mask").set(mask);
                    break;
                }
                catch (NumberFormatException e) {
                    throw new XMLStreamException("Invalid mask " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                }
                catch (UnknownHostException e) {
                    throw new XMLStreamException("Invalid address " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                }
            }
            default: throw unexpectedElement(reader);
        }
    }

    protected void parseInterfaces(final XMLExtendedStreamReader reader, final Set<String> names, final ModelNode address, final List<ModelNode> list, boolean checkSpecified) throws XMLStreamException {
        requireNoAttributes(reader);

        while (reader.nextTag() != END_ELEMENT) {
            // Attributes
            requireSingleAttribute(reader, Attribute.NAME.getLocalName());
            final String name = reader.getAttributeValue(0);
            if (! names.add(name)) {
                throw new XMLStreamException("Duplicate interface declaration", reader.getLocation());
            }
            final ModelNode interfaceAdd = new ModelNode();
            interfaceAdd.get("address").set(address);
            interfaceAdd.get("operation").set("add-interface");

            final ModelNode criteriaNode = interfaceAdd.get("criteria");
            parseInterfaceCriteria(reader, criteriaNode);

            if (criteriaNode.asInt() == 0 && checkSpecified) {
                throw unexpectedEndElement(reader);
            }
            list.add(interfaceAdd);
        }
    }

    protected void parseSocketBindingGroup(final XMLExtendedStreamReader reader, Set<String> interfaces, final ModelNode address, List<ModelNode> bindingUpdates, boolean allowInclude) throws XMLStreamException {
        Set<String> includedGroups = new HashSet<String>();
        Set<String> socketBindings = new HashSet<String>();

        // Handle attributes
        String[] attrValues = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.DEFAULT_INTERFACE.getLocalName());
        final String name = attrValues[0];
        String defaultInterface = attrValues[1];

        final ModelNode bindingGroupUpdate = new ModelNode();
        bindingGroupUpdate.get("address").set(address).add("socket-binding-group", name);
        final ModelNode bindings = bindingGroupUpdate.get("bindings");
        bindings.setEmptyList();

        // Handle elements
        while (reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case INCLUDE: {
                            if(! allowInclude) {
                                // no include in standalone
                                throw unexpectedElement(reader);
                            }
                            final String includedGroup = readStringAttributeElement(reader, Attribute.SOCKET_BINDING_GROUP.getLocalName());
                            if (includedGroups.contains(includedGroup)) {
                                throw new XMLStreamException("Included socket-binding-group " + includedGroup + " already declared", reader.getLocation());
                            }
                            includedGroups.add(includedGroup);
                            break;
                        }
                        case SOCKET_BINDING: {
                            final String bindingName = parseSocketBinding(reader, interfaces, bindings.add(), defaultInterface);
                            if (socketBindings.contains(bindingName)) {
                                throw new XMLStreamException("socket-binding " + bindingName + " already declared", reader.getLocation());
                            }
                            socketBindings.add(bindingName);
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
        bindingUpdates.add(bindingGroupUpdate);
    }

    protected String parseSocketBinding(final XMLExtendedStreamReader reader, Set<String> interfaces, ModelNode binding, final String inheritedInterfaceName) throws XMLStreamException {

        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        String name = null;

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case NAME: {
                        binding.get("name").set(name = value);
                        break;
                    }
                    case INTERFACE: {
                        if (! interfaces.contains(value)) {
                            throw new XMLStreamException("Unknown interface " + value +
                                    " " + attribute.getLocalName() + " must be declared in element " +
                                    Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        binding.get("interface").set(value);
                        break;
                    }
                    case PORT: {
                        binding.get("port").set(parseBoundedIntegerAttribute(reader, i, 0, 65535));
                        break;
                    }
                    case FIXED_PORT: {
                        binding.get("fixed-port").set(Boolean.parseBoolean(value));
                        break;
                    }
                    case MULTICAST_ADDRESS: {
                        try {
                            InetAddress mcastAddr = InetAddress.getByName(value);
                            if (!mcastAddr.isMulticastAddress()) {
                                throw new XMLStreamException("Value " + value + " for attribute " +
                                        attribute.getLocalName() + " is not a valid multicast address",
                                        reader.getLocation());
                            }
                            binding.get("multicast-address").set(mcastAddr.toString());
                        } catch (UnknownHostException e) {
                            throw new XMLStreamException("Value " + value + " for attribute " +
                                    attribute.getLocalName() + " is not a valid multicast address",
                                    reader.getLocation(), e);
                        }
                    }
                    case MULTICAST_PORT: {
                        binding.get("multicast-port").set(parseBoundedIntegerAttribute(reader, i, 1, 65535));
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! binding.has("interface")) {
            binding.get("interface").set(inheritedInterfaceName);
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);
        return name;
    }
}
