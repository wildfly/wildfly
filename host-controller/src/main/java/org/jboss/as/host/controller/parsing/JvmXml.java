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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.CommonXml;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.host.controller.JvmType;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Utilities for parsing and marshalling domain.xml and host.xml JVM configurations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JvmXml {

    public static void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> updates,
            final Set<String> jvmNames) throws XMLStreamException {
        parseJvm(reader, parentAddress, expectedNs, updates, jvmNames, false);
    }

    public static void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> updates,
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
                            CommonXml.parseProperties(reader, expectedNs)));
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

    private static void parseHeap(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
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

    private static void parsePermgen(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
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

    private static void parseStack(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
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

    private static void parseAgentLib(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
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

    private static void parseAgentPath(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
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

    private static void parseJavaagent(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> updates)
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

    private static void parseJvmOptions(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> updates)
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

    public static void writeJVMElement(final XMLExtendedStreamWriter writer, final String jvmName, final ModelNode jvmElement)
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

}
