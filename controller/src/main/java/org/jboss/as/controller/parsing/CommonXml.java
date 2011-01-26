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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NETWORK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.parseBoundedIntegerAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedEndElement;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

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
    protected final Map<String, XMLElementWriter<ModelNode>> subsystemWriters = new HashMap<String, XMLElementWriter<ModelNode>>();

    protected CommonXml(final ModuleLoader loader) {
        moduleLoader = loader;
    }

    protected void parseNamespaces(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes) {
        final int namespaceCount = reader.getNamespaceCount();
        for (int i = 0; i < namespaceCount; i ++) {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(address);
            operation.get(OP).set("add-namespace");
            String prefix = reader.getNamespacePrefix(i);
            operation.get("namespace").set(prefix == null ? "" : prefix, reader.getNamespaceURI(i));
            nodes.add(operation);
        }
    }

    protected void readHeadComment(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes) throws XMLStreamException {
        // TODO STXM-6
    }

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
            String key = it.next();
            String val = it.next();
            if (key.length() > 0 && val.length() > 0) {
                final ModelNode update = new ModelNode();
                update.get(OP_ADDR).set(address);
                update.get(OP).set("add-schema-location");
                update.get("schema-location").set(key, val);
                updateList.add(update);
            }
        }
    }

    protected void writeSchemaLocation(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        final StringBuilder b = new StringBuilder();
        final Iterator<ModelNode> iterator = modelNode.get(SCHEMA_LOCATIONS).asList().iterator();
        while (iterator.hasNext()) {
            final ModelNode location = iterator.next();
            final Property property = location.asProperty();
            b.append(property.getName()).append(' ').append(property.getValue().asString());
            if (iterator.hasNext()) {
                b.append(' ');
            }
        }
        if (b.length() > 0) {
            writer.writeAttribute(Namespace.XML_SCHEMA_INSTANCE.getUriString(), Attribute.SCHEMA_LOCATION.getLocalName(), b
                    .toString());
        }
    }

    protected void writeNamespaces(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        for (final ModelNode namespace : modelNode.get(NAMESPACES).asList()) {
            final Property property = namespace.asProperty();
            writer.writeNamespace(property.getName(), property.getValue().asString());
        }
    }

    protected void writeExtensions(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        for(final String extension : modelNode.keys()) {
            writer.writeEmptyElement(Element.EXTENSION.getLocalName());
            writer.writeAttribute(Attribute.MODULE.getLocalName(), extension);
        }
    }

    protected void writePaths(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.PATHS.getLocalName());

        for(final Property path : node.asPropertyList()) {
            final ModelNode value = path.getValue();
            writer.writeEmptyElement(Element.PATH.getLocalName());
            writer.writeAttribute(Attribute.NAME.getLocalName(), path.getName());
            writer.writeAttribute(Attribute.PATH.getLocalName(), value.get(PATH).asString());
            if(value.has(RELATIVE_TO) && value.get(RELATIVE_TO).isDefined()) {
                writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), value.get(RELATIVE_TO).asString());
            }
        }
        writer.writeEndElement();
    }

    protected void parseExtensions(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> found = new HashSet<String>();

        final ExtensionParsingContextImpl context = new ExtensionParsingContextImpl(reader.getXMLMapper());

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            // Attribute && require no content
            final String moduleName = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());

            if (! found.add(moduleName)) {
                // duplicate module name
                throw invalidAttributeValue(reader, 0);
            }

            // Register element handlers for this extension
            try {
                final Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
                boolean initialized = false;
                for (final NewExtension extension : module.loadService(NewExtension.class)) {
                    extension.initializeParsers(context);
                    if (!initialized) {
                        initialized = true;
                    }
                }
                if (!initialized) {
                    throw new IllegalStateException("No META-INF/services/" + NewExtension.class.getName() + " found for " + module.getIdentifier());
                }
                final ModelNode add = new ModelNode();
                add.get(OP_ADDR).set(address).add(EXTENSION, moduleName);
                add.get(OP).set(ADD);
                list.add(add);
            } catch (final ModuleLoadException e) {
                throw new XMLStreamException("Failed to load module", e);
            }
            this.subsystemWriters.putAll(context.getSubsystemWriters());
        }
    }

    protected void parsePaths(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final boolean requirePath) throws XMLStreamException {
        final Set<String> pathNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case PATH: {
                            parsePath(reader, address, list, requirePath, pathNames);
                            break;
                        } default: {
                            throw unexpectedElement(reader);
                        }
                    }
                    break;
                } default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parsePath(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final boolean requirePath, final Set<String> defined) throws XMLStreamException {
        String name = null;
        String path = null;
        String relativeTo = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value.trim();
                        if (RESTRICTED_PATHS.contains(value)) {
                            throw new XMLStreamException(name + " is reserved", reader.getLocation());
                        }
                        if(! defined.add(name)) {
                            throw new XMLStreamException(name + " already defined", reader.getLocation());
                        }
                        break;
                    } case PATH: {
                        path = value;
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = value;
                        break;
                    }
                    default: {
                        throw unexpectedAttribute(reader, i);
                    }
                }
            }
        }
        if(name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if(requirePath && path == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PATH));
        }
        requireNoContent(reader);
        // TODO consider making "path" a resource and not an attribute
        final ModelNode update = new ModelNode();
        update.get(OP_ADDR).set(address).add(ModelDescriptionConstants.PATH, name);
        update.get(OP).set(ADD);
        update.get(NAME).set(name);
        update.get(PATH).set(path);
        if (relativeTo != null) update.get(RELATIVE_TO).set(relativeTo);
        list.add(update);
    }

    protected void parseSystemProperties(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        while (reader.nextTag() != END_ELEMENT) {
            if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
                throw unexpectedElement(reader);
            }
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw unexpectedElement(reader);
            }

            final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
            requireNoContent(reader);

            ModelNode op = Util.getEmptyOperation(SystemPropertyAddHandler.OPERATION_NAME, address);
            op.get(NAME).set(array[0]);
            op.get(VALUE).set(array[1]);
            updates.add(op);
        }
    }

    protected ModelNode parseProperties(final XMLExtendedStreamReader reader) throws XMLStreamException {

        final ModelNode properties = new ModelNode();
        while (reader.nextTag() != END_ELEMENT) {
            if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
                throw unexpectedElement(reader);
            }
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw unexpectedElement(reader);
            }
            final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
            requireNoContent(reader);
            properties.get(array[0]).set(array[1]);
        }
        return properties;
    }

    protected void parseManagementSocket(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
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

        final ModelNode mgmtSocket = new ModelNode();
        if(maxThreads > 0) mgmtSocket.get("max-threads").set(maxThreads);
        mgmtSocket.get(INTERFACE).set(interfaceName);
        mgmtSocket.get(PORT).set(port);
        mgmtSocket.get(OP).set(MANAGEMENT);
        mgmtSocket.get(OP_ADDR).setEmptyList();
        list.add(mgmtSocket);
//        final ModelNode addMgmt = Util.getWriteAttributeOperation(address, MANAGEMENT, mgmtSocket);
//        list.add(addMgmt);

        if (maxThreads > 0) {
            // TODO - this is non-optimal.
            final ModelNode setSocketThreads = new ModelNode();
            setSocketThreads.get(OP_ADDR).set(address);
            setSocketThreads.get(OP).set("write-management-socket-threads");
            setSocketThreads.get("max-threads").set(maxThreads);
            list.add(setSocketThreads);
        }
        reader.discardRemainder();

    }

    protected void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> updates, final Set<String> jvmNames) throws XMLStreamException {

        // Handle attributes
        final List<ModelNode> attrUpdates = new ArrayList<ModelNode>();
        String name = null;
        String type = JvmType.SUN.toString();
        String home = null;
        Boolean debugEnabled = null;
        String debugOptions = null;
        Boolean envClasspathIgnored = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        if (name != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());

                        if (!jvmNames.add(value)) {
                            throw new XMLStreamException("Duplicate JVM declaration " + value, reader.getLocation());
                        }
                        name = value;
                        break;
                    }
                    case JAVA_HOME: {
                        if (home != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        home = value;
                        final ModelNode update = Util.getWriteAttributeOperation(null, "java-home", home);
                        attrUpdates.add(update);
                        break;
                    }
                    case TYPE: {
                        try {
                            // Validate the type against the enum
                            Enum.valueOf(JvmType.class, value);
                            type = value;
                        } catch (final IllegalArgumentException e) {
                            throw ParseUtils.invalidAttributeValue(reader, i);
                        }
                        break;
                    }
                    case DEBUG_ENABLED: {
                        if (debugEnabled != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        debugEnabled = Boolean.valueOf(value);
                        final ModelNode update = Util.getWriteAttributeOperation(null, "debug-enabled", debugEnabled);
                        attrUpdates.add(update);
                        break;
                    }
                    case DEBUG_OPTIONS: {
                        if (debugOptions != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        debugOptions = value;
                        final ModelNode update = Util.getWriteAttributeOperation(null, "debug-options", debugOptions);
                        attrUpdates.add(update);
                        break;
                    }
                    case ENV_CLASSPATH_IGNORED: {
                        if (envClasspathIgnored != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        envClasspathIgnored = Boolean.valueOf(value);
                        final ModelNode update = Util.getWriteAttributeOperation(null, "env-classpath-ignored", envClasspathIgnored);
                        attrUpdates.add(update);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            // FIXME and fix xsd. A name is only required at domain and host
            // level (i.e. when wrapped in <jvms/>). At server-group and server
            // levels it can be unnamed, in which case configuration from
            // domain and host levels aren't mixed in. OR make name required in xsd always
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone().add(ModelDescriptionConstants.JVM, name);

        final ModelNode addUpdate = new ModelNode();
        addUpdate.get(OP_ADDR).set(address);
        addUpdate.get(OP).set(ADD);
        addUpdate.get("name").set(name);
        addUpdate.get("jvm-type").set(type);
        updates.add(addUpdate);

        // Now we've done the add and we know the address
        for (final ModelNode attrUpdate : attrUpdates) {
            attrUpdate.get(OP_ADDR).set(address);
            updates.add(attrUpdate);
        }

        // Handle elements
        boolean hasJvmOptions = false;
        boolean hasEnvironmentVariables = false;
        boolean hasSystemProperties = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HEAP: {
                            parseHeap(reader, address, updates);
                            break;
                        }
                        case PERMGEN: {
                            parsePermgen(reader, address, updates);
                            break;
                        }
                        case STACK: {
                            parseStack(reader, address, updates);
                            break;
                        }
                        case AGENT_LIB: {
                            parseAgentLib(reader, address, updates);
                            break;
                        }
                        case AGENT_PATH: {
                            parseAgentPath(reader, address, updates);
                            break;
                        }
                        case JAVA_AGENT: {
                            parseJavaagent(reader, address, updates);
                            break;
                        }
                        case ENVIRONMENT_VARIABLES: {
                            if (hasEnvironmentVariables) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            updates.add(Util.getWriteAttributeOperation(address, "environment-variables", parseProperties(reader)));
                            hasEnvironmentVariables = true;
                            break;
                        }
                        case SYSTEM_PROPERTIES: {
                            if (hasSystemProperties) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            parseSystemProperties(reader, address, updates);
                            hasSystemProperties = true;
                            break;
                        }
                        case JVM_OPTIONS: {
                            if (hasJvmOptions) {
                                throw new XMLStreamException(element.getLocalName() + " already declared", reader.getLocation());
                            }
                            parseJvmOptions(reader, address, updates);
                            hasJvmOptions = true;
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

    private void parseHeap(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "heap-size", value);
                        updates.add(update);
                        break;
                    }
                    case MAX_SIZE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "max-heap-size", value);
                        updates.add(update);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parsePermgen(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "permgen-size", value);
                        updates.add(update);
                        break;
                    }
                    case MAX_SIZE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "max-permgen-size", value);
                        updates.add(update);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parseStack(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        boolean sizeSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "stack-size", value);
                        updates.add(update);
                        sizeSet = true;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!sizeSet) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SIZE));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parseAgentLib(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        boolean valueSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "agent-lib", value);
                        updates.add(update);
                        valueSet = true;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!valueSet) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parseAgentPath(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        boolean valueSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "agent-path", value);
                        updates.add(update);
                        valueSet = true;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!valueSet) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parseJavaagent(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        boolean valueSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, "java-agent", value);
                        updates.add(update);
                        valueSet = true;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!valueSet) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.VALUE));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
    }

    private void parseJvmOptions(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        // Handle attributes
        ParseUtils.requireNoAttributes(reader);
        // Handle elements
        boolean optionSet = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case DOMAIN_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (element == Element.OPTION) {
                        // Handle attributes
                        String option = null;
                        final int count = reader.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            final String attrValue = reader.getAttributeValue(i);
                            if (reader.getAttributeNamespace(i) != null) {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                            else {
                                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                                switch (attribute) {
                                    case VALUE: {
                                        option = attrValue;
                                        break;
                                    }
                                    default:
                                        throw ParseUtils.unexpectedAttribute(reader, i);
                                }
                            }
                        }
                        if (option == null) {
                            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
                        }

                        // PropertyAdd
                        final ModelNode update = new ModelNode();
                        update.get(OP_ADDR).set(address);
                        update.get(OP).set("add-jvm-option");
                        update.get("option").set(option);
                        updates.add(update);
                        optionSet = true;
                        // Handle elements
                        ParseUtils.requireNoContent(reader);
                    } else {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        if (!optionSet) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.OPTION));
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
            element = Element.forName(reader.getLocalName());
            switch (element) {
                case ANY:
                    parseCompoundInterfaceCriterion(reader, criteria.add().set(ANY, new ModelNode()).get(ANY));
                    break;
                case NOT:
                    parseCompoundInterfaceCriterion(reader, criteria.add().set(NOT, new ModelNode()).get(NOT));
                    break;
                default: {
//                    parseSimpleInterfaceCriterion(reader, criteria.add().set(element.getLocalName(), new ModelNode()).get(element.getLocalName()));
                    parseSimpleInterfaceCriterion(reader, criteria.add());
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
    protected void parseSimpleInterfaceCriterion(final XMLExtendedStreamReader reader, final ModelNode criteria) throws XMLStreamException {
        if (Namespace.forUri(reader.getNamespaceURI()) != Namespace.DOMAIN_1_0) {
            throw unexpectedElement(reader);
        }
        final Element element = Element.forName(reader.getLocalName());
        final String localName = element.getLocalName();
        switch (element) {
            case INET_ADDRESS: {
                requireSingleAttribute(reader, Attribute.VALUE.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate IP address
                criteria.set(localName, value);
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
                requireNoAttributes(reader);
                requireNoContent(reader);
                criteria.set(localName);
                break;
            }
            case NIC: {
                requireSingleAttribute(reader, Attribute.NAME.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate NIC name
                criteria.set(localName, value);
                break;
            }
            case NIC_MATCH: {
                requireSingleAttribute(reader, Attribute.PATTERN.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate pattern
                criteria.set(localName, value);
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
                    final ModelNode node = criteria.set(localName, new ModelNode()).get(localName);
                    node.get(NETWORK).set(net);
                    node.get(MASK).set(mask);
                    break;
                }
                catch (final NumberFormatException e) {
                    throw new XMLStreamException("Invalid mask " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                }
                catch (final UnknownHostException e) {
                    throw new XMLStreamException("Invalid address " + split[1] + " (" + e.getLocalizedMessage() +")", reader.getLocation(), e);
                }
            }
            default: throw unexpectedElement(reader);
        }
    }

    protected void parseInterfaces(final XMLExtendedStreamReader reader, final Set<String> names, final ModelNode address, final List<ModelNode> list, final boolean checkSpecified) throws XMLStreamException {
        requireNoAttributes(reader);

        while (reader.nextTag() != END_ELEMENT) {
            // Attributes
            requireSingleAttribute(reader, Attribute.NAME.getLocalName());
            final String name = reader.getAttributeValue(0);
            if (! names.add(name)) {
                throw new XMLStreamException("Duplicate interface declaration", reader.getLocation());
            }
            final ModelNode interfaceAdd = new ModelNode();
            interfaceAdd.get(OP_ADDR).set(address).add(ModelDescriptionConstants.INTERFACE, name);
            interfaceAdd.get(OP).set(ADD);

            final ModelNode criteriaNode = interfaceAdd.get(CRITERIA);
            parseInterfaceCriteria(reader, criteriaNode);

            if (criteriaNode.getType() != ModelType.STRING && criteriaNode.asInt() == 0 && checkSpecified) {
                throw unexpectedEndElement(reader);
            }
            list.add(interfaceAdd);
        }
    }

    protected void parseSocketBindingGroupRef(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {
        // Handle attributes
        String name = null;
        int offset = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case REF: {
                        if (name != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        name = value;
                        break;
                    }
                    case PORT_OFFSET: {
                        try {
                            if (offset != -1)
                                throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                            offset = Integer.parseInt(value);
                            if (offset < 0) {
                                throw new XMLStreamException(offset + " is not a valid " +
                                        attribute.getLocalName() + " -- must be greater than zero",
                                        reader.getLocation());
                            }
                        } catch (final NumberFormatException e) {
                            throw new XMLStreamException(offset + " is not a valid " +
                                    attribute.getLocalName(), reader.getLocation(), e);
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.REF));
        }

        // Handle elements
        ParseUtils.requireNoContent(reader);

        ModelNode update = Util.getWriteAttributeOperation(address, "socket-binding-group", name);
        updates.add(update);

        if (offset < 0) {
            offset = 0;
        }
        update = Util.getWriteAttributeOperation(address, "socket-binding-port-offset", offset);
        updates.add(update);
    }

    protected String parseSocketBinding(final XMLExtendedStreamReader reader, final Set<String> interfaces, final ModelNode address, final String inheritedInterfaceName, final List<ModelNode> updates) throws XMLStreamException {

        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.PORT);
        String name = null;

        final ModelNode binding = new ModelNode();
        binding.get(OP_ADDR); // undefined until we parse name
        binding.get(OP).set(ADD);

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
                        name = value;
                        binding.get(OP_ADDR).set(address).add(SOCKET_BINDING, name);
                        break;
                    }
                    case INTERFACE: {
                        if (! interfaces.contains(value)) {
                            throw new XMLStreamException("Unknown interface " + value +
                                    " " + attribute.getLocalName() + " must be declared in element " +
                                    Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        binding.get(INTERFACE).set(value);
                        break;
                    }
                    case PORT: {
                        binding.get(PORT).set(parseBoundedIntegerAttribute(reader, i, 0, 65535));
                        break;
                    }
                    case FIXED_PORT: {
                        binding.get(FIXED_PORT).set(Boolean.parseBoolean(value));
                        break;
                    }
                    case MULTICAST_ADDRESS: {
                        try {
                            final InetAddress mcastAddr = InetAddress.getByName(value);
                            if (!mcastAddr.isMulticastAddress()) {
                                throw new XMLStreamException("Value " + value + " for attribute " +
                                        attribute.getLocalName() + " is not a valid multicast address",
                                        reader.getLocation());
                            }
                            binding.get(MULTICAST_ADDRESS).set(mcastAddr.toString());
                        } catch (final UnknownHostException e) {
                            throw new XMLStreamException("Value " + value + " for attribute " +
                                    attribute.getLocalName() + " is not a valid multicast address",
                                    reader.getLocation(), e);
                        }
                    }
                    case MULTICAST_PORT: {
                        binding.get(MULTICAST_PORT).set(parseBoundedIntegerAttribute(reader, i, 1, 65535));
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);

        updates.add(binding);
        return name;
    }

    protected void writeInterfaces(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        writer.writeStartElement(Element.INTERFACES.getLocalName());
        Set<String> interfaces = modelNode.get(INTERFACE).keys();
        for (String ifaceName : interfaces) {
            ModelNode iface = modelNode.get(INTERFACE, ifaceName);
            writer.writeStartElement(Element.INTERFACE.getLocalName());
            writeAttribute(writer, Attribute.NAME, ifaceName);

            ModelNode criteria = iface.get(CRITERIA);
            if (criteria.getType() == ModelType.STRING) {
                String value = criteria.asString();
                if (value.equals(Element.ANY_ADDRESS.getLocalName())) {
                    writer.writeEmptyElement(Element.ANY_ADDRESS.getLocalName());
                } else if (value.equals(Element.ANY_IPV4_ADDRESS.getLocalName())) {
                    writer.writeEmptyElement(Element.ANY_IPV4_ADDRESS.getLocalName());
                } else if (value.equals(Element.ANY_IPV6_ADDRESS.getLocalName())) {
                    writer.writeEmptyElement(Element.ANY_IPV6_ADDRESS.getLocalName());
                } else {
                    // we should never get here
                    throw new RuntimeException("Unkown criteria type: " + value);
                }
            } else if (criteria.getType() == ModelType.LIST) {
                List<ModelNode> values = criteria.asList();
                writeInterfaceCriteria(writer, values);

            } else {
                throw new RuntimeException("Unkown type for criteria node " + criteria);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void writeInterfaceCriteria(final XMLExtendedStreamWriter writer, final List<ModelNode> criteria)
            throws XMLStreamException {
        for (ModelNode value : criteria) {
            // any and not elements are represented by properties
            if (value.getType() == ModelType.PROPERTY) {
                writePropertyInterfaceCriteria(writer, value);
            } else if (value.getType() == ModelType.LIST) {
                List<ModelNode> values = value.asList();
                for (ModelNode node : values) {
                    if (node.getType() == ModelType.PROPERTY) {
                        writePropertyInterfaceCriteria(writer, node);
                    }
                    else {
                        writeSimpleInterfaceCriteria(writer, node);
                    }
                }
            }
        }
    }

    private void writePropertyInterfaceCriteria(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        Property property = node.asProperty();
        Element element = Element.forName(property.getName());
        writer.writeStartElement(element.getLocalName());
        switch (element) {
            case ANY:
                writer.writeStartElement(Element.ANY.getLocalName());
                writeInterfaceCriteria(writer, property.getValue().get(ANY).asList());
                writer.writeEndElement();
                break;
            case NOT:
                writer.writeStartElement(Element.NOT.getLocalName());
                writeInterfaceCriteria(writer, property.getValue().get(NOT).asList());
                writer.writeEndElement();
                break;
            case INET_ADDRESS:
                writeAttribute(writer, Attribute.VALUE, property.getValue().asString());
                break;
            case NIC:
                writeAttribute(writer, Attribute.NAME, property.getValue().asString());
                break;
            case NIC_MATCH:
                writeAttribute(writer, Attribute.PATTERN, property.getValue().asString());
                break;
            case SUBNET_MATCH:
                String network = property.getValue().get(NETWORK).asString();
                String mask = property.getValue().get(MASK).asString();
                String subnet = network + "/" + mask;
                writeAttribute(writer, Attribute.VALUE, subnet);
                break;
            default:
                throw new RuntimeException("Unknown property in interface criteria list: " + property);
        }
        writer.writeEndElement();
    }

    private void writeSimpleInterfaceCriteria(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        Element element = Element.forName(node.asString());
        writer.writeEmptyElement(element.getLocalName());
    }

    protected void writeSocketBindingGroup(XMLExtendedStreamWriter writer, ModelNode bindingGroup, boolean usePortOffset) throws XMLStreamException {
        writer.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());
        ModelNode attr = bindingGroup.get(NAME);
        writeAttribute(writer, Attribute.NAME, attr.asString());

        attr = bindingGroup.get(DEFAULT_INTERFACE);
        writeAttribute(writer, Attribute.DEFAULT_INTERFACE, attr.asString());

        if (usePortOffset && bindingGroup.has(PORT_OFFSET) && bindingGroup.get(PORT_OFFSET).asInt() != 0) {
            attr = bindingGroup.get(PORT_OFFSET);
            writeAttribute(writer, Attribute.PORT_OFFSET, attr.asString());
        }
        ModelNode bindings = bindingGroup.get(SOCKET_BINDING);
        for (String bindingName: bindings.keys()) {
            ModelNode binding = bindings.get(bindingName);
            writer.writeStartElement(Element.SOCKET_BINDING.getLocalName());
            writeAttribute(writer, Attribute.NAME, bindingName);
            attr = binding.get(PORT);
            writeAttribute(writer, Attribute.PORT, attr.asString());

            attr = binding.get(FIXED_PORT);
            if (attr.isDefined() && attr.asBoolean()) {
                writeAttribute(writer, Attribute.FIXED_PORT, attr.asString());
            }
            attr = binding.get(INTERFACE);
            if (attr.isDefined()) {
                writeAttribute(writer, Attribute.INTERFACE, attr.asString());
            }
            attr = binding.get(MULTICAST_ADDRESS);
            if (attr.isDefined()) {
                writeAttribute(writer, Attribute.MULTICAST_ADDRESS, attr.asString());
            }
            attr = binding.get(MULTICAST_PORT);
            if (attr.isDefined()) {
                writeAttribute(writer, Attribute.FIXED_PORT, attr.asString());
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    protected void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode modelNode,
            Element element) throws XMLStreamException {
        writer.writeStartElement(element.getLocalName());
        final List<Property> properties = modelNode.asPropertyList();
        for (Property prop : properties) {
            writer.writeStartElement(Element.PROPERTY.getLocalName());
            writeAttribute(writer, Attribute.NAME, prop.getName());
            writeAttribute(writer, Attribute.VALUE, prop.getValue().asString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    protected static void writeAttribute(XMLExtendedStreamWriter writer, Attribute attribute, String value)
            throws XMLStreamException {
        writeAttribute(writer, attribute, value);
    }

    protected static boolean hasDefinedChild(ModelNode node, String child) {
        return node.has(child) && node.get(child).isDefined();
    }

}
