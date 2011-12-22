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
import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ARCHIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_SOURCE_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NOT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_REF;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOURCE_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOURCE_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.parseBoundedIntegerAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.parsePossibleExpression;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.requireSingleAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedEndElement;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
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
 * Bits of parsing and marshalling logic that are common across more than one of standalone.xml, domain.xml and host.xml.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class CommonXml implements XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelMarshallingContext> {

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
        set.add("jboss.server.temp.dir");
        // NOTE we actually don't create services for the following
        // however the names remain restricted for use in the configuration
        set.add("jboss.modules.dir");
        set.add("jboss.server.deploy.dir");
        set.add("jboss.domain.servers.dir");
        RESTRICTED_PATHS = Collections.unmodifiableSet(set);
    }

    private static final char[] NEW_LINE = new char[]{'\n'};

    protected final ModuleLoader moduleLoader;
    private final ExecutorService bootExecutor;

    protected CommonXml(final ModuleLoader loader, ExecutorService executorService) {
        moduleLoader = loader;
        bootExecutor = executorService;
    }

    protected String getDefaultName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw MESSAGES.cannotDetermineDefaultName(e);
        }
    }

    protected void parseNamespaces(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes) {
        final int namespaceCount = reader.getNamespaceCount();
        for (int i = 0; i < namespaceCount; i++) {
            String prefix = reader.getNamespacePrefix(i);
            // FIXME - remove once STXM-8 is released
            if (prefix != null && prefix.length() > 0) {
                nodes.add(NamespaceAddHandler.getAddNamespaceOperation(address, prefix, reader.getNamespaceURI(i)));
            }
        }
    }

    protected void readHeadComment(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes)
            throws XMLStreamException {
        // TODO STXM-6
    }

    protected void readTailComment(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> nodes)
            throws XMLStreamException {
        // TODO STXM-6
    }

    protected void parseSchemaLocations(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> updateList, final int idx) throws XMLStreamException {
        final List<String> elements = reader.getListAttributeValue(idx);
        final List<String> values = new ArrayList<String>();
        for (String element : elements) {
            if (!element.trim().isEmpty()) {
                values.add(element);
            }
        }
        if ((values.size() & 1) != 0) {
            throw invalidAttributeValue(reader, idx);
        }
        final Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            String key = it.next();
            String val = it.next();
            if (key.length() > 0 && val.length() > 0) {
                updateList.add(SchemaLocationAddHandler.getAddSchemaLocationOperation(address, key, val));
            }
        }
    }

    protected void writeSchemaLocation(final XMLExtendedStreamWriter writer, final ModelNode modelNode)
            throws XMLStreamException {
        if (!modelNode.hasDefined(SCHEMA_LOCATIONS)) {
            return;
        }
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
            writer.writeAttribute(Namespace.XML_SCHEMA_INSTANCE.getUriString(), Attribute.SCHEMA_LOCATION.getLocalName(),
                    b.toString());
        }
    }

    protected void writeNamespaces(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        final boolean needXsd = modelNode.hasDefined(SCHEMA_LOCATIONS);
        final boolean hasNamepaces = modelNode.hasDefined(NAMESPACES);
        if (!needXsd && !hasNamepaces) {
            return;
        }

        boolean wroteXsd = false;
        final String xsdUri = Namespace.XML_SCHEMA_INSTANCE.getUriString();
        if (hasNamepaces) {
            for (final Property property : modelNode.get(NAMESPACES).asPropertyList()) {
                final String uri = property.getValue().asString();
                writer.writeNamespace(property.getName(), uri);
                if (!wroteXsd && xsdUri.equals(uri)) {
                    wroteXsd = true;
                }
            }
        }
        if (needXsd && !wroteXsd) {
            writer.writeNamespace("xsd", xsdUri);
        }
    }

    protected static void writeElement(final XMLExtendedStreamWriter writer, final Element element) throws XMLStreamException {
        writer.writeStartElement(element.getLocalName());
    }

    protected void writeExtensions(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        Set<String> keys = modelNode.keys();
        if (keys.size() > 0) {
            writer.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (final String extension : keys) {
                writer.writeEmptyElement(Element.EXTENSION.getLocalName());
                writer.writeAttribute(Attribute.MODULE.getLocalName(), extension);
            }
            writer.writeEndElement();
        }
    }

    protected void writePaths(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        List<Property> paths = node.asPropertyList();
        if (paths.size() > 0) {
            writer.writeStartElement(Element.PATHS.getLocalName());

            for (final Property path : paths) {
                final ModelNode value = path.getValue();
                writer.writeEmptyElement(Element.PATH.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), path.getName());
                writer.writeAttribute(Attribute.PATH.getLocalName(), value.get(PATH).asString());
                if (value.has(RELATIVE_TO) && value.get(RELATIVE_TO).isDefined()) {
                    writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(), value.get(RELATIVE_TO).asString());
                }
            }
            writer.writeEndElement();
        }
    }

    protected void parseExtensions(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {

        long start = System.currentTimeMillis();

        requireNoAttributes(reader);

        final Set<String> found = new HashSet<String>();

        final ExtensionParsingContextImpl context = new ExtensionParsingContextImpl(reader.getXMLMapper());

        final Map<String, Future<XMLStreamException>> loadFutures = bootExecutor != null
                ? new LinkedHashMap<String, Future<XMLStreamException>>() : null;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.EXTENSION) {
                throw unexpectedElement(reader);
            }

            // One attribute && require no content
            final String moduleName = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());

            if (!found.add(moduleName)) {
                // duplicate module name
                throw invalidAttributeValue(reader, 0);
            }

            if (loadFutures != null) {
                // Load the module asynchronously
                Callable<XMLStreamException> callable = new Callable<XMLStreamException>() {
                    @Override
                    public XMLStreamException call() throws Exception {
                        return loadModule(moduleName, context);
                    }
                };
                Future<XMLStreamException> future = bootExecutor.submit(callable);
                loadFutures.put(moduleName, future);
            } else {
                // Load the module from this thread
                XMLStreamException xse = loadModule(moduleName, context);
                if (xse != null) {
                    throw xse;
                }
                addExtensionAddOperation(address, list, moduleName);
            }

        }

        if (loadFutures != null) {
            for (Map.Entry<String, Future<XMLStreamException>> entry : loadFutures.entrySet()) {

                try {
                    XMLStreamException xse = entry.getValue().get();
                    if (xse != null) {
                        throw xse;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw MESSAGES.moduleLoadingInterrupted(entry.getKey());
                } catch (ExecutionException e) {
                    throw MESSAGES.failedToLoadModule(e, entry.getKey());
                }

                addExtensionAddOperation(address, list, entry.getKey());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("Parsed extensions in [%d] ms", elapsed);
        }
    }

    private void addExtensionAddOperation(ModelNode address, List<ModelNode> list, String moduleName) {
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(address).add(EXTENSION, moduleName);
        add.get(OP).set(ADD);
        list.add(add);
    }

    private XMLStreamException loadModule(final String moduleName, final ExtensionParsingContext context) throws XMLStreamException {
        // Register element handlers for this extension
        try {
            final Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
            boolean initialized = false;
            for (final Extension extension : module.loadService(Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    extension.initializeParsers(context);
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
                if (!initialized) {
                    initialized = true;
                }
            }
            if (!initialized) {
                throw MESSAGES.notFound("META-INF/services/", Extension.class.getName(), module.getIdentifier());
            }
            return null;
        } catch (final ModuleLoadException e) {
            throw MESSAGES.failedToLoadModule(e);
        }

    }

    protected void parseFSBaseType(final XMLExtendedStreamReader reader, final ModelNode parent, final boolean isArchive)
            throws XMLStreamException {
        final ModelNode content = parent.get("content").add();
        content.get(ARCHIVE).set(isArchive);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            final String value = reader.getAttributeValue(i);
            switch (attribute) {
                case PATH:
                    content.get(PATH).set(value);
                    break;
                case RELATIVE_TO:
                    content.get(RELATIVE_TO).set(value);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);
    }

    protected void parsePaths(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list,
            final boolean requirePath) throws XMLStreamException {
        final Set<String> pathNames = new HashSet<String>();
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            requireNamespace(reader, expectedNs);

            switch (element) {
                case PATH: {
                    parsePath(reader, address, list, requirePath, pathNames);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    protected void parsePath(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list,
            final boolean requirePath, final Set<String> defined) throws XMLStreamException {
        String name = null;
        String path = null;
        String relativeTo = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value.trim();
                        if (RESTRICTED_PATHS.contains(value)) {
                            throw MESSAGES.reserved(name, reader.getLocation());
                        }
                        if (!defined.add(name)) {
                            throw MESSAGES.alreadyDefined(name, reader.getLocation());
                        }
                        break;
                    }
                    case PATH: {
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
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (requirePath && path == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.PATH));
        }
        requireNoContent(reader);
        final ModelNode update = new ModelNode();
        update.get(OP_ADDR).set(address).add(ModelDescriptionConstants.PATH, name);
        update.get(OP).set(ADD);
        update.get(NAME).set(name);
        if (path != null)
            update.get(PATH).set(path);
        if (relativeTo != null)
            update.get(RELATIVE_TO).set(relativeTo);
        list.add(update);
    }

    protected void parseSystemProperties(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs,
            final List<ModelNode> updates, boolean standalone) throws XMLStreamException {

        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.PROPERTY) {
                throw unexpectedElement(reader);
            }

            String name = null;
            String value = null;
            Boolean boottime = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String val = reader.getAttributeValue(i);
                if (!isNoNamespaceAttribute(reader, i)) {
                    throw unexpectedAttribute(reader, i);
                } else {
                    final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));

                    switch (attribute) {
                        case NAME: {
                            if (name != null) {
                                throw ParseUtils.duplicateAttribute(reader, NAME);
                            }
                            name = val;
                            break;
                        }
                        case VALUE: {
                            if (value != null) {
                                throw ParseUtils.duplicateAttribute(reader, VALUE);
                            }
                            value = val;
                            break;
                        }
                        case BOOT_TIME: {
                            if (standalone) {
                                throw unexpectedAttribute(reader, i);
                            }
                            boottime = Boolean.valueOf(val);
                            break;
                        }
                        default: {
                            throw unexpectedAttribute(reader, i);
                        }
                    }
                }
            }
            requireNoContent(reader);

            ModelNode propAddr = new ModelNode().set(address).add(SYSTEM_PROPERTY, name);
            ModelNode op = Util.getEmptyOperation(SystemPropertyAddHandler.OPERATION_NAME, propAddr);
            op.get(VALUE).set(value);
            if (boottime != null) {
                op.get(BOOT_TIME).set(boottime.booleanValue());
            }

            updates.add(op);
        }
    }

    protected ModelNode parseProperties(final XMLExtendedStreamReader reader, final Namespace expectedNs) throws XMLStreamException {

        final ModelNode properties = new ModelNode();
        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw unexpectedElement(reader);
            }
            final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
            requireNoContent(reader);
            properties.get(array[0]).set(array[1]);
        }
        return properties;
    }

    protected void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> updates,
            final Set<String> jvmNames) throws XMLStreamException {
        parseJvm(reader, parentAddress, expectedNs, updates, jvmNames, false);
    }

    protected void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> updates,
            final Set<String> jvmNames, final boolean server) throws XMLStreamException {

        // Handle attributes
        final List<ModelNode> attrUpdates = new ArrayList<ModelNode>();
        String name = null;
        String type = null;
        String home = null;
        Boolean debugEnabled = null;
        String debugOptions = null;
        Boolean envClasspathIgnored = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        if (name != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());

                        if (!jvmNames.add(value)) {
                            throw MESSAGES.duplicateDeclaration("JVM", value, reader.getLocation());
                        }
                        name = value;
                        break;
                    }
                    case JAVA_HOME: {
                        if (home != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        home = value;
                        final ModelNode update = Util.getWriteAttributeOperation(null, JVMHandlers.JVM_JAVA_HOME, home);
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
                        if (!server) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        if (debugEnabled != null) {
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        }
                        debugEnabled = Boolean.valueOf(value);
                        final ModelNode update = Util.getWriteAttributeOperation(null, JVMHandlers.JVM_DEBUG_ENABLED,
                                debugEnabled);
                        attrUpdates.add(update);
                        break;
                    }
                    case DEBUG_OPTIONS: {
                        if (!server) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        if (debugOptions != null) {
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        }
                        debugOptions = value;
                        final ModelNode update = Util.getWriteAttributeOperation(null, JVMHandlers.JVM_DEBUG_OPTIONS,
                                debugOptions);
                        attrUpdates.add(update);
                        break;
                    }
                    case ENV_CLASSPATH_IGNORED: {
                        if (envClasspathIgnored != null)
                            throw ParseUtils.duplicateAttribute(reader, attribute.getLocalName());
                        envClasspathIgnored = Boolean.valueOf(value);
                        final ModelNode update = Util.getWriteAttributeOperation(null, JVMHandlers.JVM_ENV_CLASSPATH_IGNORED,
                                envClasspathIgnored);
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
        if (debugEnabled != null && debugOptions == null && debugEnabled.booleanValue()) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.DEBUG_OPTIONS));
        }

        final ModelNode address = parentAddress.clone();
        address.add(ModelDescriptionConstants.JVM, name);

        final ModelNode addUpdate = Util.getEmptyOperation(ADD, address);
        if (type != null)
            addUpdate.get(JVM_TYPE).set(type);
        updates.add(addUpdate);

        // Now we've done the add and we know the address
        for (final ModelNode attrUpdate : attrUpdates) {
            attrUpdate.get(OP_ADDR).set(address);
            updates.add(attrUpdate);
        }

        // Handle elements
        boolean hasJvmOptions = false;
        boolean hasEnvironmentVariables = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
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
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    updates.add(Util.getWriteAttributeOperation(address, JVMHandlers.JVM_ENV_VARIABLES,
                            parseProperties(reader, expectedNs)));
                    hasEnvironmentVariables = true;
                    break;
                }
                case JVM_OPTIONS: {
                    if (hasJvmOptions) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseJvmOptions(reader, address, expectedNs, updates);
                    hasJvmOptions = true;
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    private void parseHeap(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
            throws XMLStreamException {

        String size = null;
        String maxSize = null;

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        size = value;
                        break;
                    }
                    case MAX_SIZE: {
                        maxSize = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (size != null) {
            final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_HEAP, size);
            updates.add(update);
        }

        if (maxSize != null) {
            final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_MAX_HEAP, maxSize);
            updates.add(update);
        }

        // Handle elements
        requireNoContent(reader);
    }

    private void parsePermgen(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
            throws XMLStreamException {

        String size = null;
        String maxSize = null;

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        size = value;
                        break;
                    }
                    case MAX_SIZE: {
                        maxSize = value;
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (size != null) {
            final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_PERMGEN, size);
            updates.add(update);
        }

        if (maxSize != null) {
            final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_MAX_PERMGEN, maxSize);
            updates.add(update);
        }

        // Handle elements
        requireNoContent(reader);
    }

    private void parseStack(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
            throws XMLStreamException {

        // Handle attributes
        boolean sizeSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_STACK, value);
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
        requireNoContent(reader);
    }

    private void parseAgentLib(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
            throws XMLStreamException {

        // Handle attributes
        boolean valueSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_AGENT_LIB, value);
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
        requireNoContent(reader);
    }

    private void parseAgentPath(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
            throws XMLStreamException {

        // Handle attributes
        boolean valueSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_AGENT_PATH, value);
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
        requireNoContent(reader);
    }

    private void parseJavaagent(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
            throws XMLStreamException {

        // Handle attributes
        boolean valueSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case VALUE: {
                        final ModelNode update = Util.getWriteAttributeOperation(address, JVMHandlers.JVM_JAVA_AGENT, value);
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
        requireNoContent(reader);
    }

    private void parseJvmOptions(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> updates)
            throws XMLStreamException {

        // Handle attributes
        ParseUtils.requireNoAttributes(reader);
        // Handle elements
        boolean optionSet = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (element == Element.OPTION) {
                // Handle attributes
                String option = null;
                final int count = reader.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    final String attrValue = reader.getAttributeValue(i);
                    if (!isNoNamespaceAttribute(reader, i)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    } else {
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
                update.get(OP).set(JVMHandlers.ADD_JVM_OPTION);
                update.get(JVMHandlers.JVM_OPTION).set(option);
                updates.add(update);
                optionSet = true;
                // Handle elements
                requireNoContent(reader);
            } else {
                throw unexpectedElement(reader);
            }
        }
        if (!optionSet) {
            throw missingRequiredElement(reader, Collections.singleton(Element.OPTION));
        }
    }

    protected void parseInterfaceCriteria(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode interfaceModel)
            throws XMLStreamException {
        // all subsequent elements are criteria elements
        if (reader.nextTag() == END_ELEMENT) {
            return;
        }
        requireNamespace(reader, expectedNs);
        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case ANY_ADDRESS:
            case ANY_IPV4_ADDRESS:
            case ANY_IPV6_ADDRESS: {
                interfaceModel.get(element.getLocalName()).set(true);
                requireNoContent(reader); // consume this element
                requireNoContent(reader); // consume rest of criteria (no further content allowed)
                return;
            }
        }
        do {
            requireNamespace(reader, expectedNs);
            element = Element.forName(reader.getLocalName());
            switch (element) {
                case ANY:
                    parseCompoundInterfaceCriterion(reader, expectedNs, interfaceModel.get(ANY).setEmptyObject());
                    break;
                case NOT:
                    parseCompoundInterfaceCriterion(reader, expectedNs, interfaceModel.get(NOT).setEmptyObject());
                    break;
                default: {
                    // parseSimpleInterfaceCriterion(reader, criteria.add().set(element.getLocalName(), new
                    // ModelNode()).get(element.getLocalName()));
                    parseSimpleInterfaceCriterion(reader, interfaceModel, false);
                    break;
                }
            }
        } while (reader.nextTag() != END_ELEMENT);
    }

    protected void parseCompoundInterfaceCriterion(final XMLExtendedStreamReader reader, final Namespace expectedNs, final ModelNode subModel)
            throws XMLStreamException {
        requireNoAttributes(reader);
        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            parseSimpleInterfaceCriterion(reader, subModel, true);
        }
    }

    protected void parseContentType(final XMLExtendedStreamReader reader, final ModelNode parent) throws XMLStreamException {
        final ModelNode content = parent.get("content").add();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            final String value = reader.getAttributeValue(i);
            switch (attribute) {
                case SHA1:
                    try {
                        content.get(HASH).set(HashUtil.hexStringToByteArray(value));
                    } catch (final Exception e) {
                        throw MESSAGES.invalidSha1Value(e, value, attribute.getLocalName(), reader.getLocation());
                    }
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        // Handle elements
        requireNoContent(reader);
    }

    /**
     * Creates the appropriate AbstractInterfaceCriteriaElement for simple criterion.
     *
     * Note! changes/additions made here will likely need to be added to the corresponding write method that handles the write
     * of the element. Failure to do so will result in a configuration that can be read, but not written out.
     *
     * @see {@link #writeInterfaceCriteria(org.jboss.staxmapper.XMLExtendedStreamWriter, org.jboss.dmr.ModelNode, boolean)}
     * @throws javax.xml.stream.XMLStreamException if an error occurs
     */
    protected void parseSimpleInterfaceCriterion(final XMLExtendedStreamReader reader, final ModelNode subModel, boolean nested)
            throws XMLStreamException {
        final Element element = Element.forName(reader.getLocalName());
        final String localName = element.getLocalName();
        switch (element) {
            case INET_ADDRESS: {
                requireSingleAttribute(reader, Attribute.VALUE.getLocalName());
                final String value = reader.getAttributeValue(0);
                ModelNode valueNode = parsePossibleExpression(value);
                requireNoContent(reader);
                // todo: validate IP address
                if(nested) {
                    subModel.get(localName).add(valueNode);
                } else {
                    subModel.get(localName).set(valueNode);
                }
                break;
            }
            case LOOPBACK_ADDRESS: {
                requireSingleAttribute(reader, Attribute.VALUE.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate IP address
                subModel.get(localName).set(value);
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
                subModel.get(localName).set(true);
                break;
            }
            case NIC: {
                requireSingleAttribute(reader, Attribute.NAME.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate NIC name
                if(nested) {
                    subModel.get(localName).add(value);
                } else {
                    subModel.get(localName).set(value);
                }
                break;
            }
            case NIC_MATCH: {
                requireSingleAttribute(reader, Attribute.PATTERN.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);
                // todo: validate pattern
                if(nested) {
                    subModel.get(localName).add(value);
                } else {
                    subModel.get(localName).set(value);
                }
                break;
            }
            case SUBNET_MATCH: {
                requireSingleAttribute(reader, Attribute.VALUE.getLocalName());
                final String value = reader.getAttributeValue(0);
                requireNoContent(reader);

                final String[] split = value.split("/");
                try {
                    if (split.length != 2) {
                        throw new XMLStreamException(MESSAGES.invalidAddressMaskValue(value), reader.getLocation());
                    }
                    // todo - possible DNS hit here
                    final InetAddress addr = InetAddress.getByName(split[0]);
                    // Validate both parts of the split
                    addr.getAddress();
                    Integer.parseInt(split[1]);
                    if(nested) {
                        subModel.get(localName).add(value);
                    } else {
                        subModel.get(localName).set(value);
                    }
                    break;
                } catch (final NumberFormatException e) {
                    throw new XMLStreamException(MESSAGES.invalidAddressMask(split[0], e.getLocalizedMessage()),
                            reader.getLocation(), e);
                } catch (final UnknownHostException e) {
                    throw new XMLStreamException(MESSAGES.invalidAddressValue(split[1], e.getLocalizedMessage()),
                            reader.getLocation(), e);
                }
            }
            default:
                throw unexpectedElement(reader);
        }
    }

    protected void parseInterfaces(final XMLExtendedStreamReader reader, final Set<String> names, final ModelNode address,
            final Namespace expectedNs, final List<ModelNode> list, final boolean checkSpecified) throws XMLStreamException {
        requireNoAttributes(reader);

        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            Element element = Element.forName(reader.getLocalName());
            if (Element.INTERFACE != element) {
                throw unexpectedElement(reader);
            }

            // Attributes
            requireSingleAttribute(reader, Attribute.NAME.getLocalName());
            final String name = reader.getAttributeValue(0);
            if (!names.add(name)) {
                throw MESSAGES.duplicateInterfaceDeclaration(reader.getLocation());
            }
            final ModelNode interfaceAdd = new ModelNode();
            interfaceAdd.get(OP_ADDR).set(address).add(ModelDescriptionConstants.INTERFACE, name);
            interfaceAdd.get(OP).set(ADD);

            final ModelNode criteriaNode = interfaceAdd;
            parseInterfaceCriteria(reader, expectedNs, interfaceAdd);

            if (checkSpecified && criteriaNode.getType() != ModelType.STRING && criteriaNode.getType() != ModelType.EXPRESSION
                    && criteriaNode.asInt() == 0) {
                throw unexpectedEndElement(reader);
            }
            list.add(interfaceAdd);
        }
    }

    protected void parseSocketBindingGroupRef(final XMLExtendedStreamReader reader, final ModelNode address,
            final List<ModelNode> updates) throws XMLStreamException {
        // Handle attributes
        String name = null;
        int offset = -1;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
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
                                throw MESSAGES.invalidValueGreaterThan(attribute.getLocalName(), offset, 0, reader.getLocation());
                            }
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
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.REF));
        }

        // Handle elements
        requireNoContent(reader);

        ModelNode update = Util.getWriteAttributeOperation(address, SOCKET_BINDING_GROUP, name);

        updates.add(update);

        if (offset < 0) {
            offset = 0;
        }
        if (offset > 0) {
            update = Util.getWriteAttributeOperation(address, SOCKET_BINDING_PORT_OFFSET, offset);
        }
        updates.add(update);
    }

    protected String parseSocketBinding(final XMLExtendedStreamReader reader, final Set<String> interfaces,
            final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.PORT);
        String name = null;

        final ModelNode binding = new ModelNode();
        binding.get(OP_ADDR); // undefined until we parse name
        binding.get(OP).set(ADD);

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
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
                        if (!interfaces.contains(value)) {
                            throw MESSAGES.unknownInterface(value, attribute.getLocalName(),
                                    Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        binding.get(INTERFACE).set(value);
                        break;
                    }
                    case PORT: {
                        binding.get(PORT).set(parseBoundedIntegerAttribute(reader, i, 0, 65535, true));
                        break;
                    }
                    case FIXED_PORT: {
                        binding.get(FIXED_PORT).set(Boolean.parseBoolean(value));
                        break;
                    }
                    case MULTICAST_ADDRESS: {
                        ModelNode mcastNode = parsePossibleExpression(value);
                        if (mcastNode.getType() == ModelType.EXPRESSION) {
                            binding.get(MULTICAST_ADDRESS).set(mcastNode);
                        } else {
                            try {
                                final InetAddress mcastAddr = InetAddress.getByName(value);
                                if (!mcastAddr.isMulticastAddress()) {
                                    throw MESSAGES.invalidMulticastAddress(value, attribute.getLocalName(), reader.getLocation());
                                }
                                binding.get(MULTICAST_ADDRESS).set(value);
                            } catch (final UnknownHostException e) {
                                    throw MESSAGES.invalidMulticastAddress(e, value, attribute.getLocalName(), reader.getLocation());
                            }
                        }
                        break;
                    }
                    case MULTICAST_PORT: {
                        binding.get(MULTICAST_PORT).set(parseBoundedIntegerAttribute(reader, i, 1, 65535, true));
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);

        updates.add(binding);
        return name;
    }

    protected String parseOutboundSocketBinding(final XMLExtendedStreamReader reader, final Set<String> interfaces,
                                                final String socketBindingGroupName, final ModelNode address, final List<ModelNode> updates) throws XMLStreamException {

        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        String outboundSocketBindingName = null;

        final ModelNode outboundSocketBindingAddOperation = new ModelNode();
        outboundSocketBindingAddOperation.get(OP).set(ADD); // address for this ADD operation will be set later, once the local-destination or remote-destination is parsed

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case NAME: {
                        outboundSocketBindingName = value;
                        break;
                    }
                    case SOURCE_INTERFACE: {
                        if (!interfaces.contains(value)) {
                            throw MESSAGES.unknownValueForElement(Attribute.SOURCE_INTERFACE.getLocalName(), value,
                                    Element.INTERFACE.getLocalName(), Element.INTERFACES.getLocalName(), reader.getLocation());
                        }
                        outboundSocketBindingAddOperation.get(SOURCE_INTERFACE).set(value);
                        break;
                    }
                    case SOURCE_PORT: {
                        outboundSocketBindingAddOperation.get(SOURCE_PORT).set(parseBoundedIntegerAttribute(reader, i, 0, 65535, true));
                        break;
                    }
                    case FIXED_SOURCE_PORT: {
                        outboundSocketBindingAddOperation.get(FIXED_SOURCE_PORT).set(parsePossibleExpression(value));
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        boolean mutuallyExclusiveElementAlreadyFound = false;
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case LOCAL_DESTINATION: {
                    if (mutuallyExclusiveElementAlreadyFound) {
                        throw MESSAGES.invalidOutboundSocketBinding(outboundSocketBindingName, Element.LOCAL_DESTINATION.getLocalName(),
                                Element.REMOTE_DESTINATION.getLocalName(), reader.getLocation());
                    } else {
                        mutuallyExclusiveElementAlreadyFound = true;
                    }
                    // parse the local destination outbound socket binding
                    this.parseLocalDestinationOutboundSocketBinding(reader, outboundSocketBindingName, outboundSocketBindingAddOperation);
                    // set the address of the add operation
                    // /socket-binding-group=<groupname>/local-destination-outbound-socket-binding=<outboundSocketBindingName>
                    final ModelNode addr = address.clone().add(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketBindingName);
                    outboundSocketBindingAddOperation.get(OP_ADDR).set(addr);
                    break;
                }
                case REMOTE_DESTINATION: {
                    if (mutuallyExclusiveElementAlreadyFound) {
                        throw MESSAGES.invalidOutboundSocketBinding(outboundSocketBindingName, Element.LOCAL_DESTINATION.getLocalName(),
                                Element.REMOTE_DESTINATION.getLocalName(), reader.getLocation());
                    } else {
                        mutuallyExclusiveElementAlreadyFound = true;
                    }
                    // parse the remote destination outbound socket binding
                    this.parseRemoteDestinationOutboundSocketBinding(reader, outboundSocketBindingName, outboundSocketBindingAddOperation);
                    // /socket-binding-group=<groupname>/remote-destination-outbound-socket-binding=<outboundSocketBindingName>
                    final ModelNode addr = address.clone().add(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketBindingName);
                    outboundSocketBindingAddOperation.get(OP_ADDR).set(addr);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        // add the "add" operations to the updates
        updates.add(outboundSocketBindingAddOperation);
        return outboundSocketBindingName;
    }

    private void parseLocalDestinationOutboundSocketBinding(final XMLExtendedStreamReader reader, final String outboundSocketBindingName,
                                                            final ModelNode outboundSocketBindingAddOperation) throws XMLStreamException {

        final EnumSet<Attribute> required = EnumSet.of(Attribute.SOCKET_BINDING_REF);

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case SOCKET_BINDING_REF: {
                        outboundSocketBindingAddOperation.get(SOCKET_BINDING_REF).set(value);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);
    }

    private void parseRemoteDestinationOutboundSocketBinding(final XMLExtendedStreamReader reader, final String outboundSocketBindingName,
                                                             final ModelNode outboundSocketBindingAddOperation) throws XMLStreamException {

        final EnumSet<Attribute> required = EnumSet.of(Attribute.HOST, Attribute.PORT);

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case HOST: {
                        outboundSocketBindingAddOperation.get(HOST).set(value);
                        break;
                    }
                    case PORT: {
                        outboundSocketBindingAddOperation.get(PORT).set(parseBoundedIntegerAttribute(reader, i, 0, 65535, true));
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        // Handle elements
        requireNoContent(reader);
    }

    protected void parseDeployments(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list,
            final boolean allowEnabled) throws XMLStreamException {
        requireNoAttributes(reader);

        final Set<String> names = new HashSet<String>();

        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            Element deployment = Element.forName(reader.getLocalName());
            if (Element.DEPLOYMENT != deployment) {
                throw unexpectedElement(reader);
            }

            // Handle attributes
            String uniqueName = null;
            String runtimeName = null;
            String startInput = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                if (!isNoNamespaceAttribute(reader, i)) {
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
                        case ENABLED: {
                            if (allowEnabled) {
                                startInput = value;
                                break;
                            } else {
                                throw unexpectedAttribute(reader, i);
                            }
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
            final boolean enabled = startInput == null ? true : Boolean.parseBoolean(startInput);

            final ModelNode deploymentAddress = address.clone().add(DEPLOYMENT, uniqueName);
            final ModelNode deploymentAdd = Util.getEmptyOperation(ADD, deploymentAddress);

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                requireNamespace(reader, expectedNs);
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case CONTENT:
                        parseContentType(reader, deploymentAdd);
                        break;
                    case FS_ARCHIVE:
                        parseFSBaseType(reader, deploymentAdd, true);
                        break;
                    case FS_EXPLODED:
                        parseFSBaseType(reader, deploymentAdd, false);
                        break;
                    default:
                        throw unexpectedElement(reader);
                }
            }

            deploymentAdd.get(RUNTIME_NAME).set(runtimeName);
            if (allowEnabled) {
                deploymentAdd.get(ENABLED).set(enabled);
            }
            list.add(deploymentAdd);
        }
    }

    protected void parseVault(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list) throws XMLStreamException {
        // Some form of assertion could be added to ensure we did not reach here for 1.0 schema based XML but in reality that
        // should not happen.

        final int vaultAttribCount = reader.getAttributeCount();

        ModelNode vault = new ModelNode();
        String code = null;

        if(vaultAttribCount > 1) {
            throw unexpectedAttribute(reader, vaultAttribCount);
        }

        for (int i = 0; i < vaultAttribCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CODE: {
                    code = value;
                    vault.get(Attribute.CODE.getLocalName()).set(code);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        ModelNode vaultAddress = address.clone();
        vaultAddress.add(CORE_SERVICE, VAULT);
        if(code != null){
            vault.get(Attribute.CODE.getLocalName()).set(code);
        }
        vault.get(OP_ADDR).set(vaultAddress);
        vault.get(OP).set(ADD);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case VAULT_OPTION: {
                    parseModuleOption(reader, vault.get(VAULT_OPTIONS));
                    break;
                }
            }
        }
        list.add(vault);
    }

    protected void parseModuleOption(XMLExtendedStreamReader reader, ModelNode moduleOptions) throws XMLStreamException {
        String name = null;
        String val = null;
        EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.VALUE);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case VALUE: {
                    val = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (required.size() > 0) {
            throw missingRequired(reader, required);
        }

        moduleOptions.add(name, val);
        requireNoContent(reader);
    }

    /**
     * Write the interfaces including the criteria elements.
     *
     * @param writer the xml stream writer
     * @param modelNode the model
     * @throws XMLStreamException
     */
    protected void writeInterfaces(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        writer.writeStartElement(Element.INTERFACES.getLocalName());
        final Set<String> interfaces = modelNode.keys();
        for (String ifaceName : interfaces) {
            final ModelNode iface = modelNode.get(ifaceName);
            writer.writeStartElement(Element.INTERFACE.getLocalName());
            writeAttribute(writer, Attribute.NAME, ifaceName);
            // <any-* /> is just handled at the root
            if(iface.get(Element.ANY_ADDRESS.getLocalName()).asBoolean(false)) {
                writer.writeEmptyElement(Element.ANY_ADDRESS.getLocalName());
            } else if(iface.get(Element.ANY_IPV4_ADDRESS.getLocalName()).asBoolean(false)) {
                writer.writeEmptyElement(Element.ANY_IPV4_ADDRESS.getLocalName());
            } else if(iface.get(Element.ANY_IPV6_ADDRESS.getLocalName()).asBoolean(false)) {
                writer.writeEmptyElement(Element.ANY_IPV6_ADDRESS.getLocalName());
            } else {
                // Write the other criteria elements
                writeInterfaceCriteria(writer, iface, false);
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Write the criteria elements, extracting the information of the sub-model.
     *
     * @param writer the xml stream writer
     * @param subModel the interface model
     * @param nested whether it the criteria elements are nested as part of <not /> or <any />
     * @throws XMLStreamException
     */
    private void writeInterfaceCriteria(final XMLExtendedStreamWriter writer, final ModelNode subModel, final boolean nested) throws XMLStreamException {
        for(final Property property : subModel.asPropertyList()) {
            if(property.getValue().isDefined()) {
                writeInterfaceCriteria(writer, property, nested);
            }
        }
    }

    private void writeInterfaceCriteria(final XMLExtendedStreamWriter writer, final Property property, final boolean nested) throws XMLStreamException {
        final Element element = Element.forName(property.getName());
        switch (element) {
            case INET_ADDRESS:
                writeInterfaceCriteria(writer, element, Attribute.VALUE, property.getValue(), nested);
                break;
            case LOOPBACK_ADDRESS:
                writeInterfaceCriteria(writer, element, Attribute.VALUE, property.getValue(), false);
                break;
            case LINK_LOCAL_ADDRESS:
            case LOOPBACK:
            case MULTICAST:
            case POINT_TO_POINT:
            case PUBLIC_ADDRESS:
            case SITE_LOCAL_ADDRESS:
            case UP:
            case VIRTUAL: {
                if(property.getValue().asBoolean(false)) {
                    writer.writeEmptyElement(element.getLocalName());
                }
                break;
            }
            case NIC:
                writeInterfaceCriteria(writer, element, Attribute.NAME, property.getValue(), nested);
                break;
            case NIC_MATCH:
                writeInterfaceCriteria(writer, element, Attribute.PATTERN, property.getValue(), nested);
                break;
            case SUBNET_MATCH:
                writeInterfaceCriteria(writer, element, Attribute.VALUE, property.getValue(), nested);
                break;
            case ANY :
            case NOT:
                if(nested) {
                    break;
                }
                writer.writeStartElement(element.getLocalName());
                writeInterfaceCriteria(writer, property.getValue(), true);
                writer.writeEndElement();
                break;
            case NAME:
                // not a criteria element; ignore
                break;
            default: {
                // TODO we perhaps should just log a warning.
                throw MESSAGES.unknownCriteriaInterfaceProperty(property.getName());
            }
        }
    }

    private static void writeInterfaceCriteria(final XMLExtendedStreamWriter writer, final Element element, final Attribute attribute, final ModelNode subModel, boolean asList) throws XMLStreamException {
        if(asList) {
            // Nested criteria elements are represented as list in the model
            writeListAsMultipleElements(writer, element, attribute, subModel);
        } else {
            writeSingleElement(writer, element, attribute, subModel);
        }
    }

    private static void writeSingleElement(final XMLExtendedStreamWriter writer, final Element element, final Attribute attribute, final ModelNode subModel) throws XMLStreamException {
        writer.writeEmptyElement(element.getLocalName());
        writeAttribute(writer, attribute, subModel.asString());
    }

    private static void writeListAsMultipleElements(final XMLExtendedStreamWriter writer, final Element element, Attribute attribute, final ModelNode subModel) throws XMLStreamException {
        final List<ModelNode> list = subModel.asList();
        for(final ModelNode node : list) {
            writer.writeEmptyElement(element.getLocalName());
            writeAttribute(writer, attribute, node.asString());
        }
    }

    protected void writeSocketBindingGroup(XMLExtendedStreamWriter writer, ModelNode bindingGroup, boolean fromServer)
            throws XMLStreamException {

        writer.writeStartElement(Element.SOCKET_BINDING_GROUP.getLocalName());

        SocketBindingGroupResourceDefinition.NAME.marshallAsAttribute(bindingGroup, writer);
        SocketBindingGroupResourceDefinition.DEFAULT_INTERFACE.marshallAsAttribute(bindingGroup, writer);

        if (fromServer) {
            SocketBindingGroupResourceDefinition.PORT_OFFSET.marshallAsAttribute(bindingGroup, writer);
        }
        if (!fromServer) {
            SocketBindingGroupResourceDefinition.INCLUDES.marshallAsElement(bindingGroup, writer);
        }

        if (bindingGroup.hasDefined(SOCKET_BINDING)) {
            ModelNode bindings = bindingGroup.get(SOCKET_BINDING);
            for (String bindingName : bindings.keys()) {
                ModelNode binding = bindings.get(bindingName);
                writer.writeStartElement(Element.SOCKET_BINDING.getLocalName());
                writeAttribute(writer, Attribute.NAME, bindingName);
                ModelNode attr = binding.get(INTERFACE);
                if (attr.isDefined()) {
                    writeAttribute(writer, Attribute.INTERFACE, attr.asString());
                }
                attr = binding.get(PORT);
                writeAttribute(writer, Attribute.PORT, attr.asString());

                attr = binding.get(FIXED_PORT);
                if (attr.isDefined() && attr.asBoolean()) {
                    writeAttribute(writer, Attribute.FIXED_PORT, attr.asString());
                }
                attr = binding.get(MULTICAST_ADDRESS);
                if (attr.isDefined()) {
                    writeAttribute(writer, Attribute.MULTICAST_ADDRESS, attr.asString());
                }
                attr = binding.get(MULTICAST_PORT);
                if (attr.isDefined()) {
                    writeAttribute(writer, Attribute.MULTICAST_PORT, attr.asString());
                }
                writer.writeEndElement();
            }
        }
        // outbound-socket-binding (for local destination)
        if (bindingGroup.hasDefined(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING)) {
            final ModelNode localDestinationOutboundSocketBindings = bindingGroup.get(LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING);
            for (final String outboundSocketBindingName : localDestinationOutboundSocketBindings.keys()) {
                final ModelNode outboundSocketBinding = localDestinationOutboundSocketBindings.get(outboundSocketBindingName);
                // <outbound-socket-binding>
                writer.writeStartElement(Element.OUTBOUND_SOCKET_BINDING.getLocalName());
                // name of the outbound socket binding
                writeAttribute(writer, Attribute.NAME, outboundSocketBindingName);
                // (optional) source port
                if (outboundSocketBinding.hasDefined(SOURCE_PORT)) {
                    final String sourcePort = outboundSocketBinding.get(SOURCE_PORT).asString();
                    writeAttribute(writer, Attribute.SOURCE_PORT, sourcePort);
                }
                // (optional) source interface
                if (outboundSocketBinding.hasDefined(SOURCE_INTERFACE)) {
                    final String sourceInterface = outboundSocketBinding.get(SOURCE_INTERFACE).asString();
                    writeAttribute(writer, Attribute.SOURCE_INTERFACE, sourceInterface);
                }
                // (optional) fixedSourcePort
                if (outboundSocketBinding.hasDefined(FIXED_SOURCE_PORT)) {
                    final String fixedSourcePort = outboundSocketBinding.get(FIXED_SOURCE_PORT).asString();
                    writeAttribute(writer, Attribute.FIXED_SOURCE_PORT, fixedSourcePort);
                }
                // write the <local-destination> element
                writer.writeStartElement(Element.LOCAL_DESTINATION.getLocalName());
                // socket-binding-ref
                final ModelNode socketBindingRef = outboundSocketBinding.get(SOCKET_BINDING_REF);
                // write the socket-binding-ref attribute for the local-destination element
                writeAttribute(writer, Attribute.SOCKET_BINDING_REF, socketBindingRef.asString());
                // </local-destination>
                writer.writeEndElement();
                // </outbound-socket-binding>
                writer.writeEndElement();
            }
        }
        // outbound-socket-binding (for remote destination)
        if (bindingGroup.hasDefined(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING)) {
            final ModelNode remoteDestinationOutboundSocketBindings = bindingGroup.get(REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING);
            for (final String outboundSocketBindingName : remoteDestinationOutboundSocketBindings.keys()) {
                final ModelNode outboundSocketBinding = remoteDestinationOutboundSocketBindings.get(outboundSocketBindingName);
                // <outbound-socket-binding>
                writer.writeStartElement(Element.OUTBOUND_SOCKET_BINDING.getLocalName());
                // name of the outbound socket binding
                writeAttribute(writer, Attribute.NAME, outboundSocketBindingName);
                // (optional) source port
                if (outboundSocketBinding.hasDefined(SOURCE_PORT)) {
                    final String sourcePort = outboundSocketBinding.get(SOURCE_PORT).asString();
                    writeAttribute(writer, Attribute.SOURCE_PORT, sourcePort);
                }
                // (optional) source interface
                if (outboundSocketBinding.hasDefined(SOURCE_INTERFACE)) {
                    final String sourceInterface = outboundSocketBinding.get(SOURCE_INTERFACE).asString();
                    writeAttribute(writer, Attribute.SOURCE_INTERFACE, sourceInterface);
                }
                // (optional) fixedSourcePort
                if (outboundSocketBinding.hasDefined(FIXED_SOURCE_PORT)) {
                    final String fixedSourcePort = outboundSocketBinding.get(FIXED_SOURCE_PORT).asString();
                    writeAttribute(writer, Attribute.FIXED_SOURCE_PORT, fixedSourcePort);
                }
                // write the <remote-destination> element
                writer.writeStartElement(Element.REMOTE_DESTINATION.getLocalName());
                // destination host
                final ModelNode host = outboundSocketBinding.get(HOST);
                writeAttribute(writer, Attribute.HOST, host.asString());
                // destination port
                final ModelNode destPort = outboundSocketBinding.get(PORT);
                writeAttribute(writer, Attribute.PORT, destPort.asString());
                // </remote-destination>
                writer.writeEndElement();
                // </outbound-socket-binding>
                writer.writeEndElement();
            }
        }
        // </socket-binding-group>
        writer.writeEndElement();
    }

    protected void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode modelNode, Element element,
            boolean standalone) throws XMLStreamException {
        final List<Property> properties = modelNode.asPropertyList();
        if (properties.size() > 0) {
            writer.writeStartElement(element.getLocalName());
            for (Property prop : properties) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writeAttribute(writer, Attribute.NAME, prop.getName());
                ModelNode sysProp = prop.getValue();
                if (sysProp.hasDefined(VALUE)) {
                    writeAttribute(writer, Attribute.VALUE, sysProp.get(VALUE).asString());
                }
                if (!standalone && sysProp.hasDefined(BOOT_TIME) && !sysProp.get(BOOT_TIME).asBoolean()) {
                    writeAttribute(writer, Attribute.BOOT_TIME, "false");
                }

                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    protected static void writeAttribute(XMLExtendedStreamWriter writer, Attribute attribute, String value)
            throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), value);
    }

    protected void writeJVMElement(final XMLExtendedStreamWriter writer, final String jvmName, final ModelNode jvmElement)
            throws XMLStreamException {
        writer.writeStartElement(Element.JVM.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), jvmName);

        if (jvmElement.hasDefined(JVM_TYPE)) {
            writer.writeAttribute(Attribute.TYPE.getLocalName(), jvmElement.get(JVM_TYPE).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_JAVA_HOME)) {
            writer.writeAttribute(Attribute.JAVA_HOME.getLocalName(), jvmElement.get(JVMHandlers.JVM_JAVA_HOME).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_DEBUG_ENABLED)) {
            writer.writeAttribute(Attribute.DEBUG_ENABLED.getLocalName(), jvmElement.get(JVMHandlers.JVM_DEBUG_ENABLED)
                    .asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_DEBUG_OPTIONS)) {
            if (!jvmElement.hasDefined(JVMHandlers.JVM_DEBUG_ENABLED)) {
                writer.writeAttribute(Attribute.DEBUG_ENABLED.getLocalName(), "false");
            }
            writer.writeAttribute(Attribute.DEBUG_OPTIONS.getLocalName(), jvmElement.get(JVMHandlers.JVM_DEBUG_OPTIONS)
                    .asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_ENV_CLASSPATH_IGNORED)) {
            writer.writeAttribute(Attribute.ENV_CLASSPATH_IGNORED.getLocalName(),
                    jvmElement.get(JVMHandlers.JVM_ENV_CLASSPATH_IGNORED).asString());
        }

        if (jvmElement.hasDefined(JVMHandlers.JVM_HEAP) || jvmElement.hasDefined(JVMHandlers.JVM_MAX_HEAP)) {
            writer.writeEmptyElement(Element.HEAP.getLocalName());
            if (jvmElement.hasDefined(JVMHandlers.JVM_HEAP))
                writer.writeAttribute(Attribute.SIZE.getLocalName(), jvmElement.get(JVMHandlers.JVM_HEAP).asString());
            if (jvmElement.hasDefined(JVMHandlers.JVM_MAX_HEAP))
                writer.writeAttribute(Attribute.MAX_SIZE.getLocalName(), jvmElement.get(JVMHandlers.JVM_MAX_HEAP).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_PERMGEN) || jvmElement.hasDefined(JVMHandlers.JVM_MAX_PERMGEN)) {
            writer.writeEmptyElement(Element.PERMGEN.getLocalName());
            if (jvmElement.hasDefined(JVMHandlers.JVM_PERMGEN))
                writer.writeAttribute(Attribute.SIZE.getLocalName(), jvmElement.get(JVMHandlers.JVM_PERMGEN).asString());
            if (jvmElement.hasDefined(JVMHandlers.JVM_MAX_PERMGEN))
                writer.writeAttribute(Attribute.MAX_SIZE.getLocalName(), jvmElement.get(JVMHandlers.JVM_MAX_PERMGEN).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_STACK)) {
            writer.writeEmptyElement(Element.STACK.getLocalName());
            writer.writeAttribute(Attribute.SIZE.getLocalName(), jvmElement.get(JVMHandlers.JVM_STACK).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_AGENT_LIB)) {
            writer.writeEmptyElement(Element.AGENT_LIB.getLocalName());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), jvmElement.get(JVMHandlers.JVM_AGENT_LIB).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_AGENT_PATH)) {
            writer.writeEmptyElement(Element.AGENT_PATH.getLocalName());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), jvmElement.get(JVMHandlers.JVM_AGENT_PATH).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_JAVA_AGENT)) {
            writer.writeEmptyElement(Element.JAVA_AGENT.getLocalName());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), jvmElement.get(JVMHandlers.JVM_JAVA_AGENT).asString());
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_OPTIONS)) {
            writer.writeStartElement(Element.JVM_OPTIONS.getLocalName());
            for (final ModelNode option : jvmElement.get(JVMHandlers.JVM_OPTIONS).asList()) {
                writer.writeEmptyElement(Element.OPTION.getLocalName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), option.asString());
            }
            writer.writeEndElement();
        }
        if (jvmElement.hasDefined(JVMHandlers.JVM_ENV_VARIABLES)) {
            writer.writeStartElement(Element.ENVIRONMENT_VARIABLES.getLocalName());
            for (final Property variable : jvmElement.get(JVMHandlers.JVM_ENV_VARIABLES).asPropertyList()) {
                writer.writeEmptyElement(Element.VARIABLE.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), variable.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), variable.getValue().asString());
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    protected static void writeContentItem(final XMLExtendedStreamWriter writer, final ModelNode contentItem)
            throws XMLStreamException {
        if (contentItem.has(HASH)) {
            writeElement(writer, Element.CONTENT);
            writeAttribute(writer, Attribute.SHA1, HashUtil.bytesToHexString(contentItem.require(HASH).asBytes()));
            writer.writeEndElement();
        } else {
            if (contentItem.require(ARCHIVE).asBoolean()) {
                writeElement(writer, Element.FS_ARCHIVE);
            } else {
                writeElement(writer, Element.FS_EXPLODED);
            }
            writeAttribute(writer, Attribute.PATH, contentItem.require(PATH).asString());
            if (contentItem.has(RELATIVE_TO))
                writeAttribute(writer, Attribute.RELATIVE_TO, contentItem.require(RELATIVE_TO).asString());
            writer.writeEndElement();
        }
    }

    protected void writeVault(XMLExtendedStreamWriter writer, ModelNode vault) throws XMLStreamException {
        writer.writeStartElement(Element.VAULT.getLocalName());
        String code = vault.hasDefined(Attribute.CODE.getLocalName()) ? vault.get(Attribute.CODE.getLocalName()).asString() : null;
        if (code != null && !code.isEmpty()) {
            writer.writeAttribute(Attribute.CODE.getLocalName(), code);
        }

        if (vault.hasDefined(VAULT_OPTION)) {
            ModelNode properties = vault.get(VAULT_OPTION);
            for (Property prop : properties.asPropertyList()) {
                writer.writeEmptyElement(Element.VAULT_OPTION.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), prop.getValue().asString());
            }
        }
        writer.writeEndElement();
    }

    protected static void writeNewLine(XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(NEW_LINE, 0, 1);
    }
}
