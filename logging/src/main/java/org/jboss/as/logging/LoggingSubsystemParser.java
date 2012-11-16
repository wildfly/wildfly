/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.controller.services.path.PathResourceDefinition.PATH;
import static org.jboss.as.controller.services.path.PathResourceDefinition.RELATIVE_TO;
import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.ASYNC_HANDLER;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.CATEGORY;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.CONSOLE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.CUSTOM_HANDLER;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.FILTER_SPEC;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.HANDLER_NAME;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.LOGGER;
import static org.jboss.as.logging.CommonAttributes.LOGGING_PROFILE;
import static org.jboss.as.logging.CommonAttributes.LOGGING_PROFILES;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.MAX_INCLUSIVE;
import static org.jboss.as.logging.CommonAttributes.MAX_LEVEL;
import static org.jboss.as.logging.CommonAttributes.MIN_INCLUSIVE;
import static org.jboss.as.logging.CommonAttributes.MIN_LEVEL;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.PATTERN;
import static org.jboss.as.logging.CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.REPLACEMENT;
import static org.jboss.as.logging.CommonAttributes.REPLACE_ALL;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER_ATTRIBUTE_NAME;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SIZE_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.CommonAttributes.TARGET;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    static final LoggingSubsystemParser INSTANCE = new LoggingSubsystemParser();

    private LoggingSubsystemParser() {
        //
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, LoggingExtension.SUBSYSTEM_NAME));

        list.add(Util.createAddOperation(address));

        final List<ModelNode> loggerOperations = new ArrayList<ModelNode>();
        final List<ModelNode> asyncHandlerOperations = new ArrayList<ModelNode>();
        final List<ModelNode> otherOperations = new ArrayList<ModelNode>();

        // Elements
        final Set<String> loggerNames = new HashSet<String>();
        final Set<String> handlerNames = new HashSet<String>();
        boolean gotRoot = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
            switch (namespace) {
                case LOGGING_1_0:
                case LOGGING_1_1:
                case LOGGING_1_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGGER: {
                            parseLoggerElement(reader, address, loggerOperations, loggerNames);
                            break;
                        }
                        case ROOT_LOGGER: {
                            if (gotRoot) {
                                throw unexpectedElement(reader);
                            }
                            gotRoot = true;
                            parseRootLoggerElement(reader, address, loggerOperations);
                            break;
                        }
                        case CONSOLE_HANDLER: {
                            parseConsoleHandlerElement(reader, address, otherOperations, handlerNames);
                            break;
                        }
                        case FILE_HANDLER: {
                            parseFileHandlerElement(reader, address, otherOperations, handlerNames);
                            break;
                        }
                        case CUSTOM_HANDLER: {
                            if (namespace == Namespace.LOGGING_1_0)
                                throw unexpectedElement(reader);
                            parseCustomHandlerElement(reader, address, otherOperations, handlerNames);
                            break;
                        }
                        case PERIODIC_ROTATING_FILE_HANDLER: {
                            parsePeriodicRotatingFileHandlerElement(reader, address, otherOperations, handlerNames);
                            break;
                        }
                        case SIZE_ROTATING_FILE_HANDLER: {
                            parseSizeRotatingHandlerElement(reader, address, otherOperations, handlerNames);
                            break;
                        }
                        case ASYNC_HANDLER: {
                            parseAsyncHandlerElement(reader, address, asyncHandlerOperations, handlerNames);
                            break;
                        }
                        case LOGGING_PROFILES:
                            if (namespace == Namespace.LOGGING_1_0 || namespace == Namespace.LOGGING_1_1)
                                throw unexpectedElement(reader);
                            parseLoggingProfilesElement(reader, address, list);
                            break;
                        default: {
                            reader.handleAny(list);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        list.addAll(otherOperations);
        list.addAll(asyncHandlerOperations);
        list.addAll(loggerOperations);
    }


    static void parseLoggerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.CATEGORY);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case CATEGORY: {
                    name = value;
                    break;
                }
                case USE_PARENT_HANDLERS: {
                    USE_PARENT_HANDLERS.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        assert name != null;
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Setup the operation
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(address.toModelNode()).add(LOGGER, name);

        // Element
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (namespace) {
                case LOGGING_1_0:
                case LOGGING_1_1:
                case LOGGING_1_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    switch (element) {
                        case LEVEL: {
                            LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), op, reader);
                            break;
                        }
                        case HANDLERS: {
                            parseHandlersElement(op.get(HANDLERS.getName()), reader);
                            break;
                        }
                        case FILTER_SPEC:
                        case FILTER: {
                            parseFilter(namespace, op, reader);
                            break;
                        }
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
        list.add(op);
    }

    static void parseAsyncHandlerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
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
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Setup the operation
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode()).add(ASYNC_HANDLER, name);

        // Elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case LEVEL: {
                    LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                    break;
                }
                case SUBHANDLERS: {
                    parseHandlersElement(node.get(SUBHANDLERS.getName()), reader);
                    break;
                }
                case FILTER_SPEC:
                case FILTER: {
                    parseFilter(namespace, node, reader);
                    break;
                }
                case FORMATTER: {
                    FORMATTER.parseAndSetParameter(parseFormatterElement(reader), node, reader);
                    break;
                }
                case QUEUE_LENGTH: {
                    QUEUE_LENGTH.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case OVERFLOW_ACTION: {
                    OVERFLOW_ACTION.parseAndSetParameter(readStringAttributeElement(reader, "value").toUpperCase(Locale.US), node, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        list.add(node);
    }

    static void parseRootLoggerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        final ModelNode node = new ModelNode();
        node.get(OP).set(RootLoggerResourceDefinition.ROOT_LOGGER_ADD_OPERATION_NAME);
        node.get(OP_ADDR).set(address.toModelNode()).add(ROOT_LOGGER, ROOT_LOGGER_ATTRIBUTE_NAME);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (namespace) {
                case LOGGING_1_0:
                case LOGGING_1_1:
                case LOGGING_1_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (encountered.contains(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    encountered.add(element);
                    switch (element) {
                        case FILTER_SPEC:
                        case FILTER: {
                            parseFilter(namespace, node, reader);
                            break;
                        }
                        case LEVEL: {
                            LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                            break;
                        }
                        case HANDLERS: {
                            parseHandlersElement(node.get(HANDLERS.getName()), reader);
                            break;
                        }
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
        list.add(node);
    }

    static void parseConsoleHandlerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
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
                case AUTOFLUSH: {
                    AUTOFLUSH.parseAndSetParameter(value, node, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Set-up the operation
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode()).add(CONSOLE_HANDLER, name);

        // Elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case LEVEL: {
                    LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                    break;
                }
                case ENCODING: {
                    ENCODING.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case FILTER_SPEC:
                case FILTER: {
                    parseFilter(namespace, node, reader);
                    break;
                }
                case FORMATTER: {
                    FORMATTER.parseAndSetParameter(parseFormatterElement(reader), node, reader);
                    break;
                }
                case TARGET: {
                    final String target = readStringAttributeElement(reader, "name");
                    if (!(target.equals("System.out") || target.equals("System.err"))) {
                        throw new XMLStreamException(MESSAGES.invalidTargetName(EnumSet.allOf(Target.class)), reader.getLocation());
                    }
                    TARGET.parseAndSetParameter(target, node, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        list.add(node);
    }

    static void parseFileHandlerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
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
                case AUTOFLUSH: {
                    AUTOFLUSH.parseAndSetParameter(value, node, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Setup the operation
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode()).add(FILE_HANDLER, name);

        // Elements
        final EnumSet<Element> requiredElem = EnumSet.of(Element.FILE);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            requiredElem.remove(element);
            switch (element) {
                case LEVEL: {
                    LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                    break;
                }
                case ENCODING: {
                    ENCODING.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case FILTER_SPEC:
                case FILTER: {
                    parseFilter(namespace, node, reader);
                    break;
                }
                case FORMATTER: {
                    FORMATTER.parseAndSetParameter(parseFormatterElement(reader), node, reader);
                    break;
                }
                case FILE: {
                    parseFileElement(node.get(FILE.getName()), reader);
                    break;
                }
                case APPEND: {
                    APPEND.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!requiredElem.isEmpty()) {
            throw missingRequired(reader, requiredElem);
        }
        list.add(node);
    }

    static void parseCustomHandlerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.CLASS, Attribute.MODULE);
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
                case CLASS: {
                    CLASS.parseAndSetParameter(value, node, reader);
                    break;
                }
                case MODULE: {
                    MODULE.parseAndSetParameter(value, node, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Setup the operation
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode()).add(CUSTOM_HANDLER, name);


        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case LEVEL: {
                    LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                    break;
                }
                case ENCODING: {
                    ENCODING.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case FILTER_SPEC:
                case FILTER: {
                    parseFilter(namespace, node, reader);
                    break;
                }
                case FORMATTER: {
                    FORMATTER.parseAndSetParameter(parseFormatterElement(reader), node, reader);
                    break;
                }
                case PROPERTIES: {
                    parsePropertyElement(node, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        list.add(node);
    }

    static void parsePeriodicRotatingFileHandlerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
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
                case AUTOFLUSH: {
                    AUTOFLUSH.parseAndSetParameter(value, node, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Setup the operation
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode()).add(PERIODIC_ROTATING_FILE_HANDLER, name);

        final EnumSet<Element> requiredElem = EnumSet.of(Element.FILE, Element.SUFFIX);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            requiredElem.remove(element);
            switch (element) {
                case LEVEL: {
                    LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                    break;
                }
                case ENCODING: {
                    ENCODING.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case FILTER_SPEC:
                case FILTER: {
                    parseFilter(namespace, node, reader);
                    break;
                }
                case FORMATTER: {
                    FORMATTER.parseAndSetParameter(parseFormatterElement(reader), node, reader);
                    break;
                }
                case FILE: {
                    parseFileElement(node.get(FILE.getName()), reader);
                    break;
                }
                case APPEND: {
                    APPEND.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case SUFFIX: {
                    SUFFIX.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!requiredElem.isEmpty()) {
            throw missingRequired(reader, requiredElem);
        }
        list.add(node);
    }

    static void parseSizeRotatingHandlerElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
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
                case AUTOFLUSH: {
                    AUTOFLUSH.parseAndSetParameter(value, node, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Setup the operation
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address.toModelNode()).add(SIZE_ROTATING_FILE_HANDLER, name);

        final EnumSet<Element> requiredElem = EnumSet.of(Element.FILE);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        final Namespace namespace = Namespace.forUri(reader.getNamespaceURI());
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            requiredElem.remove(element);
            switch (element) {
                case LEVEL: {
                    LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                    break;
                }
                case ENCODING: {
                    ENCODING.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case FILTER_SPEC:
                case FILTER: {
                    parseFilter(namespace, node, reader);
                    break;
                }
                case FORMATTER: {
                    FORMATTER.parseAndSetParameter(parseFormatterElement(reader), node, reader);
                    break;
                }
                case FILE: {
                    parseFileElement(node.get(FILE.getName()), reader);
                    break;
                }
                case APPEND: {
                    APPEND.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case ROTATE_SIZE: {
                    ROTATE_SIZE.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                case MAX_BACKUP_INDEX: {
                    MAX_BACKUP_INDEX.parseAndSetParameter(readStringAttributeElement(reader, "value"), node, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        list.add(node);
    }

    private static void parseFileElement(final ModelNode node, final XMLExtendedStreamReader reader) throws XMLStreamException {
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATH: {
                    PATH.parseAndSetParameter(value, node, reader);
                    break;
                }
                case RELATIVE_TO: {
                    RELATIVE_TO.parseAndSetParameter(value, node, reader);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        requireNoContent(reader);
    }

    private static String parseFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        final String formatterSpec;
        if (reader.nextTag() != START_ELEMENT) {
            throw new XMLStreamException(MESSAGES.missingRequiredNestedFilterElement(), reader.getLocation());
        }
        switch (Namespace.forUri(reader.getNamespaceURI())) {
            case LOGGING_1_0:
            case LOGGING_1_1:
            case LOGGING_1_2: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case PATTERN_FORMATTER: {
                        formatterSpec = parsePatternFormatterElement(reader);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
                break;
            }
            default: {
                throw unexpectedElement(reader);
            }
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return formatterSpec;
    }

    private static String parsePatternFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String pattern = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATTERN);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case PATTERN: {
                    pattern = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        requireNoContent(reader);
        return pattern;
    }

    private static void parsePropertyElement(final ModelNode node, final XMLExtendedStreamReader reader) throws XMLStreamException {
        while (reader.nextTag() != END_ELEMENT) {
            final int cnt = reader.getAttributeCount();
            String name = null;
            String value = null;
            for (int i = 0; i < cnt; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String attrValue = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = attrValue;
                        break;
                    }
                    case VALUE: {
                        value = attrValue;
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME.getLocalName()));
            }
            node.get(PROPERTIES.getName()).add(name, new ModelNode(value));
            if (reader.nextTag() != END_ELEMENT) {
                throw unexpectedElement(reader);
            }
        }
    }

    private static void parseHandlersElement(final ModelNode node, final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0:
                case LOGGING_1_1:
                case LOGGING_1_2: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HANDLER: {
                            node.add(readStringAttributeElement(reader, "name"));
                            break;
                        }
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

    static void parseLoggingProfilesElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list) throws XMLStreamException {
        final Set<String> profileNames = new HashSet<String>();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGGING_PROFILE: {
                    parseLoggingProfileElement(reader, address, list, profileNames);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
    }

    static void parseLoggingProfileElement(final XMLExtendedStreamReader reader, final PathAddress address, final List<ModelNode> list, final Set<String> profileNames) throws XMLStreamException {
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
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
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (!profileNames.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Setup the address
        final PathAddress profileAddress = address.append(LOGGING_PROFILE, name);
        list.add(Util.createAddOperation(profileAddress));

        final List<ModelNode> loggerOperations = new ArrayList<ModelNode>();
        final List<ModelNode> asyncHandlerOperations = new ArrayList<ModelNode>();
        final List<ModelNode> otherOperations = new ArrayList<ModelNode>();

        final Set<String> loggerNames = new HashSet<String>();
        final Set<String> handlerNames = new HashSet<String>();
        boolean gotRoot = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case LOGGER: {
                    parseLoggerElement(reader, profileAddress, loggerOperations, loggerNames);
                    break;
                }
                case ROOT_LOGGER: {
                    if (gotRoot) {
                        throw unexpectedElement(reader);
                    }
                    gotRoot = true;
                    parseRootLoggerElement(reader, profileAddress, loggerOperations);
                    break;
                }
                case CONSOLE_HANDLER: {
                    parseConsoleHandlerElement(reader, profileAddress, otherOperations, handlerNames);
                    break;
                }
                case FILE_HANDLER: {
                    parseFileHandlerElement(reader, profileAddress, otherOperations, handlerNames);
                    break;
                }
                case CUSTOM_HANDLER: {
                    parseCustomHandlerElement(reader, profileAddress, otherOperations, handlerNames);
                    break;
                }
                case PERIODIC_ROTATING_FILE_HANDLER: {
                    parsePeriodicRotatingFileHandlerElement(reader, profileAddress, otherOperations, handlerNames);
                    break;
                }
                case SIZE_ROTATING_FILE_HANDLER: {
                    parseSizeRotatingHandlerElement(reader, profileAddress, otherOperations, handlerNames);
                    break;
                }
                case ASYNC_HANDLER: {
                    parseAsyncHandlerElement(reader, profileAddress, asyncHandlerOperations, handlerNames);
                    break;
                }
                default: {
                    reader.handleAny(list);
                    break;
                }
            }
        }
        list.addAll(otherOperations);
        list.addAll(asyncHandlerOperations);
        list.addAll(loggerOperations);
    }

    private static void parseFilter(final Namespace namespace, final ModelNode node, final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (namespace == Namespace.LOGGING_1_0 || namespace == Namespace.LOGGING_1_1) {
            // No attributes
            if (reader.getAttributeCount() > 0) {
                throw unexpectedAttribute(reader, 0);
            }
            final StringBuilder filter = new StringBuilder();
            parseFilterChildren(filter, false, reader);
            node.get(FILTER_SPEC.getName()).set(filter.toString());
        } else {
            FILTER_SPEC.parseAndSetParameter(readStringAttributeElement(reader, Attribute.VALUE.getLocalName()), node, reader);
        }
    }

    private static void parseFilterChildren(final StringBuilder filter, final boolean useDelimiter, final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        final char delimiter = ',';

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ACCEPT: {
                    filter.append(Filters.ACCEPT);
                    requireNoContent(reader);
                    break;
                }
                case ALL: {
                    filter.append(Filters.ALL).append('(');
                    parseFilterChildren(filter, true, reader);
                    // If the last character is a delimiter remove it
                    final int index = filter.length() - 1;
                    if (filter.charAt(index) == delimiter) {
                        filter.setCharAt(index, ')');
                    } else {
                        filter.append(')');
                    }
                    break;
                }
                case ANY: {
                    filter.append(Filters.ANY).append('(');
                    parseFilterChildren(filter, true, reader);
                    // If the last character is a delimiter remove it
                    final int index = filter.length() - 1;
                    if (filter.charAt(index) == delimiter) {
                        filter.setCharAt(index, ')');
                    } else {
                        filter.append(')');
                    }
                    break;
                }
                case CHANGE_LEVEL: {
                    filter.append(Filters.LEVEL_CHANGE)
                            .append('(')
                            .append(readStringAttributeElement(reader, CommonAttributes.NEW_LEVEL.getName()))
                            .append(')');
                    break;
                }
                case DENY: {
                    filter.append(Filters.DENY);
                    requireNoContent(reader);
                    break;
                }
                case LEVEL: {
                    filter.append(Filters.LEVELS)
                            .append('(')
                            .append(readStringAttributeElement(reader, NAME.getName()))
                            .append(')');
                    break;
                }
                case LEVEL_RANGE: {
                    filter.append(Filters.LEVEL_RANGE);
                    final boolean minInclusive = Boolean.parseBoolean(reader.getAttributeValue(null, MIN_INCLUSIVE.getName()));
                    final boolean maxInclusive = Boolean.parseBoolean(reader.getAttributeValue(null, MAX_INCLUSIVE.getName()));
                    if (minInclusive) {
                        filter.append('[');
                    } else {
                        filter.append('(');
                    }
                    filter.append(reader.getAttributeValue(null, MIN_LEVEL.getName())).append(delimiter);
                    filter.append(reader.getAttributeValue(null, MAX_LEVEL.getName()));
                    if (maxInclusive) {
                        filter.append(']');
                    } else {
                        filter.append(')');
                    }
                    requireNoContent(reader);
                    break;
                }
                case MATCH: {
                    filter.append(Filters.MATCH).append("(\"").append(readStringAttributeElement(reader, PATTERN.getName())).append("\")");
                    break;
                }
                case NOT: {
                    filter.append(Filters.NOT).append('(');
                    parseFilterChildren(filter, true, reader);
                    // If the last character is a delimiter remove it
                    final int index = filter.length() - 1;
                    if (filter.charAt(index) == delimiter) {
                        filter.setCharAt(index, ')');
                    } else {
                        filter.append(')');
                    }
                    break;
                }
                case REPLACE: {
                    final boolean replaceAll = Boolean.valueOf(reader.getAttributeValue(null, REPLACE_ALL.getName()));
                    if (replaceAll) {
                        filter.append(Filters.SUBSTITUTE_ALL);
                    } else {
                        filter.append(Filters.SUBSTITUTE);
                    }
                    filter.append("(\"")
                            .append(reader.getAttributeValue(null, PATTERN.getName()))
                            .append('"')
                            .append(delimiter)
                            .append('"')
                            .append(reader.getAttributeValue(null, REPLACEMENT.getName()))
                            .append("\")");
                    break;
                }
                default:
                    throw unexpectedElement(reader);
            }
            if (useDelimiter) {
                filter.append(delimiter);
            }

        }
    }

    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();

        writeContent(writer, node);

        if (node.hasDefined(LOGGING_PROFILE)) {
            final List<Property> profiles = node.get(LOGGING_PROFILE).asPropertyList();
            if (!profiles.isEmpty()) {
                writer.writeStartElement(LOGGING_PROFILES);
                for (Property profile : profiles) {
                    final String name = profile.getName();
                    writer.writeStartElement(LOGGING_PROFILE);
                    writer.writeAttribute(Attribute.NAME.getLocalName(), name);
                    writeContent(writer, profile.getValue());
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    public void writeContent(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {

        if (node.hasDefined(ASYNC_HANDLER)) {
            final ModelNode handlers = node.get(ASYNC_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeAsynchHandler(writer, handler, name);
                }
            }
        }
        if (node.hasDefined(CONSOLE_HANDLER)) {
            final ModelNode handlers = node.get(CONSOLE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeConsoleHandler(writer, handler, name);
                }
            }
        }
        if (node.hasDefined(FILE_HANDLER)) {
            final ModelNode handlers = node.get(FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeFileHandler(writer, handler, name);
                }
            }
        }
        if (node.hasDefined(CUSTOM_HANDLER)) {
            final ModelNode handlers = node.get(CUSTOM_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeCustomHandler(writer, handler, name);
                }
            }
        }
        if (node.hasDefined(PERIODIC_ROTATING_FILE_HANDLER)) {
            final ModelNode handlers = node.get(PERIODIC_ROTATING_FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writePeriodicRotatingFileHandler(writer, handler, name);
                }
            }
        }
        if (node.hasDefined(SIZE_ROTATING_FILE_HANDLER)) {
            final ModelNode handlers = node.get(SIZE_ROTATING_FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (handler.isDefined()) {
                    writeSizeRotatingFileHandler(writer, handler, name);
                }
            }
        }
        if (node.hasDefined(LOGGER)) {
            for (String name : node.get(LOGGER).keys()) {
                writeLogger(writer, name, node.get(LOGGER, name));
            }
        }
        if (node.hasDefined(ROOT_LOGGER)) {
            writeRootLogger(writer, node.get(ROOT_LOGGER, ROOT_LOGGER_ATTRIBUTE_NAME));
        }
    }

    private void writeCommonLogger(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        LEVEL.marshallAsElement(node, writer);
        FILTER_SPEC.marshallAsElement(node, writer);
        HANDLERS.marshallAsElement(node, writer);
    }

    private void writeCommonHandler(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        LEVEL.marshallAsElement(node, writer);
        ENCODING.marshallAsElement(node, writer);
        FILTER_SPEC.marshallAsElement(node, writer);
        FORMATTER.marshallAsElement(node, writer);
    }

    private void writeConsoleHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CONSOLE_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeCommonHandler(writer, node);
        TARGET.marshallAsElement(node, writer);
        writer.writeEndElement();
    }

    private void writeFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.FILE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeCommonHandler(writer, node);
        FILE.marshallAsElement(node, writer);
        APPEND.marshallAsElement(node, writer);

        writer.writeEndElement();
    }

    private void writeCustomHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CUSTOM_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        CLASS.marshallAsAttribute(node, writer);
        MODULE.marshallAsAttribute(node, writer);
        writeCommonHandler(writer, node);
        PROPERTIES.marshallAsElement(node, writer);

        writer.writeEndElement();
    }

    private void writePeriodicRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.PERIODIC_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeCommonHandler(writer, node);
        FILE.marshallAsElement(node, writer);
        SUFFIX.marshallAsElement(node, writer);
        APPEND.marshallAsElement(node, writer);

        writer.writeEndElement();
    }

    private void writeSizeRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.SIZE_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeCommonHandler(writer, node);
        FILE.marshallAsElement(node, writer);
        ROTATE_SIZE.marshallAsElement(node, writer);
        MAX_BACKUP_INDEX.marshallAsElement(node, writer);
        APPEND.marshallAsElement(node, writer);

        writer.writeEndElement();
    }

    private void writeAsynchHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.ASYNC_HANDLER.getLocalName());
        writer.writeAttribute(HANDLER_NAME.getXmlName(), name);
        LEVEL.marshallAsElement(node, writer);
        FILTER_SPEC.marshallAsElement(node, writer);
        FORMATTER.marshallAsElement(node, writer);
        QUEUE_LENGTH.marshallAsElement(node, writer);
        OVERFLOW_ACTION.marshallAsElement(node, writer);
        SUBHANDLERS.marshallAsElement(node, writer);

        writer.writeEndElement();
    }

    private void writeLogger(final XMLExtendedStreamWriter writer, String name, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.LOGGER.getLocalName());
        writer.writeAttribute(CATEGORY.getXmlName(), name);
        USE_PARENT_HANDLERS.marshallAsAttribute(node, writer);
        writeCommonLogger(writer, node);
        writer.writeEndElement();
    }

    private void writeRootLogger(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.ROOT_LOGGER.getLocalName());
        writeCommonLogger(writer, node);
        writer.writeEndElement();
    }
}
