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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.isNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.host.controller.model.jvm.JvmAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Utilities for parsing and marshalling domain.xml and host.xml JVM configurations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author Kabir Khan
 */
public class JvmXml {

    public static void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> updates,
            final Set<String> jvmNames) throws XMLStreamException {
        parseJvm(reader, parentAddress, expectedNs, updates, jvmNames, false);
    }

    public static void parseJvm(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final Namespace expectedNs, final List<ModelNode> updates,
            final Set<String> jvmNames, final boolean server) throws XMLStreamException {

        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);

        // Handle attributes
        String name = null;
        Boolean debugEnabled = null;
        String debugOptions = null;
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
                        JvmAttributes.JAVA_HOME.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case TYPE: {
                        try {
                            // Validate the type against the enum
                            JvmAttributes.TYPE.parseAndSetParameter(value, addOp, reader);
                        } catch (final IllegalArgumentException e) {
                            throw ParseUtils.invalidAttributeValue(reader, i);
                        }
                        break;
                    }
                    case DEBUG_ENABLED: {
                        if (!server) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        debugEnabled = Boolean.valueOf(value);
                        JvmAttributes.DEBUG_ENABLED.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case DEBUG_OPTIONS: {
                        if (!server) {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        debugOptions = value;
                        JvmAttributes.DEBUG_OPTIONS.parseAndSetParameter(value, addOp, reader);
                        break;
                    }
                    case ENV_CLASSPATH_IGNORED: {
                        JvmAttributes.ENV_CLASSPATH_IGNORED.parseAndSetParameter(value, addOp, reader);
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
        addOp.get(OP_ADDR).set(address);
        updates.add(addOp);

        // Handle elements
        boolean hasJvmOptions = false;
        boolean hasEnvironmentVariables = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case HEAP: {
                    parseHeap(reader, address, addOp);
                    break;
                }
                case PERMGEN: {
                    parsePermgen(reader, address, addOp);
                    break;
                }
                case STACK: {
                    parseStack(reader, address, addOp);
                    break;
                }
                case AGENT_LIB: {
                    parseAgentLib(reader, address, addOp);
                    break;
                }
                case AGENT_PATH: {
                    parseAgentPath(reader, address, addOp);
                    break;
                }
                case JAVA_AGENT: {
                    parseJavaagent(reader, address, addOp);
                    break;
                }
                case ENVIRONMENT_VARIABLES: {
                    if (hasEnvironmentVariables) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseEnvironmentVariables(reader, expectedNs, addOp);
                    hasEnvironmentVariables = true;
                    break;
                }
                case JVM_OPTIONS: {
                    if (hasJvmOptions) {
                        throw MESSAGES.alreadyDefined(element.getLocalName(), reader.getLocation());
                    }
                    parseJvmOptions(reader, address, expectedNs, addOp);
                    hasJvmOptions = true;
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
        }
    }

    public static ModelNode parseEnvironmentVariables(final XMLExtendedStreamReader reader, final Namespace expectedNs, ModelNode addOp) throws XMLStreamException {
        final ModelNode properties = new ModelNode();
        while (reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            if (Element.forName(reader.getLocalName()) != Element.VARIABLE) {
                throw unexpectedElement(reader);
            }
            final String[] array = requireAttributes(reader, Attribute.NAME.getLocalName(), Attribute.VALUE.getLocalName());
            requireNoContent(reader);
            properties.add(array[0], array[1]);
        }

        if (!properties.isDefined()) {
            throw missingRequiredElement(reader, Collections.singleton(Element.OPTION));
        }
        addOp.get(JvmAttributes.JVM_ENV_VARIABLES).set(properties);
        return properties;
    }


    private static void parseHeap(final XMLExtendedStreamReader reader, final ModelNode address, ModelNode addOp)
            throws XMLStreamException {
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
                        if (checkParseAndSetParameter(JvmAttributes.HEAP_SIZE, value, addOp, reader)) {
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
                        break;
                    }
                    case MAX_SIZE: {
                        if (checkParseAndSetParameter(JvmAttributes.MAX_HEAP_SIZE, value, addOp, reader)) {
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        // Handle elements
        requireNoContent(reader);
    }

    private static void parsePermgen(final XMLExtendedStreamReader reader, final ModelNode address, ModelNode addOp)
            throws XMLStreamException {

        // Handle attributes
        boolean sizeSet = false;
        boolean maxSet = false;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (!isNoNamespaceAttribute(reader, i)) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SIZE: {
                        if (checkParseAndSetParameter(JvmAttributes.PERMGEN_SIZE, value, addOp, reader)) {
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
                        break;
                    }
                    case MAX_SIZE: {
                        if (checkParseAndSetParameter(JvmAttributes.MAX_PERMGEN_SIZE, value, addOp, reader)) {
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        // Handle elements
        requireNoContent(reader);
    }

    private static void parseStack(final XMLExtendedStreamReader reader, final ModelNode address, ModelNode addOp)
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
                        sizeSet = true;
                        if (checkParseAndSetParameter(JvmAttributes.STACK_SIZE, value, addOp, reader)){
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());

                        }
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

    private static void parseAgentLib(final XMLExtendedStreamReader reader, final ModelNode address, ModelNode addOp)
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
                        if (checkParseAndSetParameter(JvmAttributes.AGENT_LIB, value, addOp, reader)){
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
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

    private static void parseAgentPath(final XMLExtendedStreamReader reader, final ModelNode address, ModelNode addOp)
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
                        if (checkParseAndSetParameter(JvmAttributes.AGENT_PATH, value, addOp, reader)){
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
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

    private static void parseJavaagent(final XMLExtendedStreamReader reader, final ModelNode address, ModelNode addOp)
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
                        if (checkParseAndSetParameter(JvmAttributes.JAVA_AGENT, value, addOp, reader)) {
                            throw ParseUtils.duplicateNamedElement(reader, reader.getLocalName());
                        }
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

    private static void parseJvmOptions(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final ModelNode addOp)
            throws XMLStreamException {

        ModelNode options = new ModelNode();
        // Handle attributes
        ParseUtils.requireNoAttributes(reader);
        // Handle elements
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

                options.add(option);

                // Handle elements
                requireNoContent(reader);
            } else {
                throw unexpectedElement(reader);
            }
        }
        if (!options.isDefined()) {
            throw missingRequiredElement(reader, Collections.singleton(Element.OPTION));
        }
        addOp.get(JvmAttributes.JVM_OPTIONS).set(options);
    }

    public static void writeJVMElement(final XMLExtendedStreamWriter writer, final String jvmName, final ModelNode jvmElement)
            throws XMLStreamException {
        writer.writeStartElement(Element.JVM.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), jvmName);

        JvmAttributes.TYPE.marshallAsAttribute(jvmElement, writer);
        JvmAttributes.JAVA_HOME.marshallAsAttribute(jvmElement, writer);
        JvmAttributes.DEBUG_ENABLED.marshallAsAttribute(jvmElement, writer);
        JvmAttributes.DEBUG_OPTIONS.marshallAsAttribute(jvmElement, writer);
        if (JvmAttributes.DEBUG_OPTIONS.isMarshallable(jvmElement)) {
            if (!JvmAttributes.DEBUG_ENABLED.isMarshallable(jvmElement)) {
                writer.writeAttribute(Attribute.DEBUG_ENABLED.getLocalName(), "false");
            }
        }
        JvmAttributes.ENV_CLASSPATH_IGNORED.marshallAsAttribute(jvmElement, writer);
        if (JvmAttributes.HEAP_SIZE.isMarshallable(jvmElement) || JvmAttributes.MAX_HEAP_SIZE.isMarshallable(jvmElement)) {
            writer.writeEmptyElement(Element.HEAP.getLocalName());
            JvmAttributes.HEAP_SIZE.marshallAsAttribute(jvmElement, writer);
            JvmAttributes.MAX_HEAP_SIZE.marshallAsAttribute(jvmElement, writer);
        }
        if (JvmAttributes.PERMGEN_SIZE.isMarshallable(jvmElement) || JvmAttributes.MAX_HEAP_SIZE.isMarshallable(jvmElement)) {
            writer.writeEmptyElement(Element.PERMGEN.getLocalName());
            JvmAttributes.PERMGEN_SIZE.marshallAsAttribute(jvmElement, writer);
            JvmAttributes.MAX_PERMGEN_SIZE.marshallAsAttribute(jvmElement, writer);
        }
        if (JvmAttributes.STACK_SIZE.isMarshallable(jvmElement)) {
            writer.writeEmptyElement(Element.STACK.getLocalName());
            JvmAttributes.STACK_SIZE.marshallAsAttribute(jvmElement, writer);
        }
        if (JvmAttributes.AGENT_LIB.isMarshallable(jvmElement)) {
            writer.writeEmptyElement(Element.AGENT_LIB.getLocalName());
            JvmAttributes.AGENT_LIB.marshallAsAttribute(jvmElement, writer);
        }
        if (JvmAttributes.AGENT_PATH.isMarshallable(jvmElement)) {
            writer.writeEmptyElement(Element.AGENT_PATH.getLocalName());
            JvmAttributes.AGENT_PATH.marshallAsAttribute(jvmElement, writer);
        }
        if (JvmAttributes.JAVA_AGENT.isMarshallable(jvmElement)) {
            writer.writeEmptyElement(Element.JAVA_AGENT.getLocalName());
            JvmAttributes.JAVA_AGENT.marshallAsAttribute(jvmElement, writer);
        }
        if (JvmAttributes.OPTIONS.isMarshallable(jvmElement)) {
            JvmAttributes.OPTIONS.marshallAsElement(jvmElement, writer);
        }
        if (JvmAttributes.ENVIRONMENT_VARIABLES.isMarshallable(jvmElement)) {
            JvmAttributes.ENVIRONMENT_VARIABLES.marshallAsElement(jvmElement, writer);
        }

        writer.writeEndElement();
    }

    private static boolean checkParseAndSetParameter(final SimpleAttributeDefinition ad, final String value, final ModelNode operation, final XMLStreamReader reader) throws XMLStreamException {
        boolean alreadyExisted = operation.hasDefined(ad.getName());
        ad.parseAndSetParameter(value, operation, reader);
        return alreadyExisted;
    }
}
