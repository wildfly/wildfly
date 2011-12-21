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
import static org.jboss.as.logging.CommonAttributes.ACCEPT;
import static org.jboss.as.logging.CommonAttributes.ALL;
import static org.jboss.as.logging.CommonAttributes.ANY;
import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.ASYNC_HANDLER;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.CATEGORY;
import static org.jboss.as.logging.CommonAttributes.CHANGE_LEVEL;
import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.CONSOLE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.CUSTOM_HANDLER;
import static org.jboss.as.logging.CommonAttributes.DENY;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.LEVEL_RANGE;
import static org.jboss.as.logging.CommonAttributes.LOGGER;
import static org.jboss.as.logging.CommonAttributes.MATCH;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.MAX_INCLUSIVE;
import static org.jboss.as.logging.CommonAttributes.MAX_LEVEL;
import static org.jboss.as.logging.CommonAttributes.MIN_INCLUSIVE;
import static org.jboss.as.logging.CommonAttributes.MIN_LEVEL;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.NAME;
import static org.jboss.as.logging.CommonAttributes.NEW_LEVEL;
import static org.jboss.as.logging.CommonAttributes.NOT;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.PATTERN;
import static org.jboss.as.logging.CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.logging.CommonAttributes.REPLACE;
import static org.jboss.as.logging.CommonAttributes.REPLACEMENT;
import static org.jboss.as.logging.CommonAttributes.REPLACE_ALL;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER_NAME;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SIZE_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.CommonAttributes.TARGET;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.logging.handlers.console.Target;
import org.jboss.as.logging.loggers.RootLoggerAdd;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
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

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, LoggingExtension.SUBSYSTEM_NAME);
        address.protect();

        list.add(LoggingExtension.NewLoggingSubsystemAdd.createOperation(address));

        // Elements
        final Set<String> loggerNames = new HashSet<String>();
        final Set<String> handlerNames = new HashSet<String>();
        boolean gotRoot = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0:
                case LOGGING_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGGER: {
                            parseLoggerElement(reader, address, list, loggerNames);
                            break;
                        }
                        case ROOT_LOGGER: {
                            if (gotRoot) {
                                throw unexpectedElement(reader);
                            }
                            gotRoot = true;
                            parseRootLoggerElement(reader, address, list);
                            break;
                        }
                        case CONSOLE_HANDLER: {
                            parseConsoleHandlerElement(reader, address, list, handlerNames);
                            break;
                        }
                        case FILE_HANDLER: {
                            parseFileHandlerElement(reader, address, list, handlerNames);
                            break;
                        }
                        case CUSTOM_HANDLER: {
                            parseCustomHandlerElement(reader, address, list, handlerNames);
                            break;
                        }
                        case PERIODIC_ROTATING_FILE_HANDLER: {
                            parsePeriodicRotatingFileHandlerElement(reader, address, list, handlerNames);
                            break;
                        }
                        case SIZE_ROTATING_FILE_HANDLER: {
                            parseSizeRotatingHandlerElement(reader, address, list, handlerNames);
                            break;
                        }
                        case ASYNC_HANDLER: {
                            parseAsyncHandlerElement(reader, address, list, handlerNames);
                            break;
                        }
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
    }


    static void parseLoggerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        final ModelNode node = new ModelNode();
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
                    CATEGORY.parseAndSetParameter(value, node, reader);
                    name = value;
                    break;
                }
                case USE_PARENT_HANDLERS: {
                    USE_PARENT_HANDLERS.parseAndSetParameter(value, node, reader);
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
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(LOGGER, name);

        // Element
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0:
                case LOGGING_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    switch (element) {
                        case LEVEL: {
                            LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                            break;
                        }
                        case HANDLERS: {
                            parseHandlersElement(node.get(HANDLERS.getName()), reader);
                            break;
                        }
                        case FILTER: {
                            parseFilter(node, reader);
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

    static void parseAsyncHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
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
                    NAME.parseAndSetParameter(value, node, reader);
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
        node.get(OP_ADDR).set(address).add(ASYNC_HANDLER, name);

        // Elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
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
                case FILTER: {
                    parseFilter(node, reader);
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

    static void parseRootLoggerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        final ModelNode node = new ModelNode();
        node.get(OP).set(RootLoggerAdd.OPERATION_NAME);
        node.get(OP_ADDR).set(address).add(ROOT_LOGGER, ROOT_LOGGER_NAME);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0:
                case LOGGING_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (encountered.contains(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    encountered.add(element);
                    switch (element) {
                        case FILTER: {
                            parseFilter(node, reader);
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

    static void parseConsoleHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
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
                    NAME.parseAndSetParameter(value, node, reader);
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
        node.get(OP_ADDR).set(address).add(CONSOLE_HANDLER, name);

        // Elements
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
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
                case FILTER: {
                    parseFilter(node, reader);
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

    static void parseFileHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
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
                    NAME.parseAndSetParameter(value, node, reader);
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
        node.get(OP_ADDR).set(address).add(FILE_HANDLER, name);

        // Elements
        final EnumSet<Element> requiredElem = EnumSet.of(Element.FILE);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
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
                case FILTER: {
                    parseFilter(node, reader);
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

    static void parseCustomHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
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
                    NAME.parseAndSetParameter(value, node, reader);
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
        node.get(OP_ADDR).set(address).add(CUSTOM_HANDLER, name);


        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
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
                case FILTER: {
                    parseFilter(node, reader);
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

    static void parsePeriodicRotatingFileHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
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
                    NAME.parseAndSetParameter(value, node, reader);
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
        node.get(OP_ADDR).set(address).add(PERIODIC_ROTATING_FILE_HANDLER, name);

        final EnumSet<Element> requiredElem = EnumSet.of(Element.FILE, Element.SUFFIX);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
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
                case FILTER: {
                    parseFilter(node, reader);
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

    static void parseSizeRotatingHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
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
                    NAME.parseAndSetParameter(value, node, reader);
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
        node.get(OP_ADDR).set(address).add(SIZE_ROTATING_FILE_HANDLER, name);

        final EnumSet<Element> requiredElem = EnumSet.of(Element.FILE);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
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
                case FILTER: {
                    parseFilter(node, reader);
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
            case LOGGING_1_1: {
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
            node.get(PROPERTIES).add(name, new ModelNode().set(value));
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
                case LOGGING_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HANDLER: {
                            node.add(readStringAttributeElement(reader, "name"));
                            // HANDLER.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader.getLocation());
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

    private static void parseFilter(final ModelNode node, final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        parseFilterChildren(node.get(FILTER.getName()), reader);
    }

    private static void parseFilterChildren(final ModelNode node, final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0:
                case LOGGING_1_1: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case ACCEPT: {
                            ACCEPT.parseAndSetParameter(Boolean.TRUE.toString(), node, reader);
                            requireNoContent(reader);
                            break;
                        }
                        case ALL: {
                            parseFilterChildren(node.get(ALL.getName()), reader);
                            break;
                        }
                        case ANY: {
                            parseFilterChildren(node.get(ANY.getName()), reader);
                            break;
                        }
                        case CHANGE_LEVEL: {
                            CHANGE_LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "new-level"), node, reader);
                            break;
                        }
                        case DENY: {
                            DENY.parseAndSetParameter(Boolean.TRUE.toString(), node, reader);
                            requireNoContent(reader);
                            break;
                        }
                        case LEVEL: {
                            LEVEL.parseAndSetParameter(readStringAttributeElement(reader, "name"), node, reader);
                            break;
                        }
                        case LEVEL_RANGE: {
                            final ModelNode levelRange = node.get(LEVEL_RANGE.getName());
                            parseRequiredAttribute(MIN_LEVEL, reader, levelRange);
                            parseRequiredAttribute(MAX_LEVEL, reader, levelRange);
                            parseAttribute(MIN_INCLUSIVE, reader, levelRange);
                            parseAttribute(MAX_INCLUSIVE, reader, levelRange);
                            requireNoContent(reader);
                            break;
                        }
                        case MATCH: {
                            MATCH.parseAndSetParameter(readStringAttributeElement(reader, "pattern"), node, reader);
                            break;
                        }
                        case NOT: {
                            parseFilterChildren(node.get(NOT.getName()), reader);
                            break;
                        }
                        case REPLACE: {
                            final ModelNode replace = node.get(REPLACE.getName());
                            parseRequiredAttribute(PATTERN, reader, replace);
                            parseRequiredAttribute(REPLACEMENT, reader, replace);
                            parseAttribute(REPLACE_ALL, reader, replace);
                            requireNoContent(reader);
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

    private static void parseRequiredAttribute(final SimpleAttributeDefinition attribute, final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {
        final String value = reader.getAttributeValue(null, attribute.getName());
        if (value == null) {
            throw missingRequired(reader, Collections.singleton(attribute.getName()));
        }
        attribute.parseAndSetParameter(value, node, reader);
    }

    private static void parseAttribute(final SimpleAttributeDefinition attribute, final XMLExtendedStreamReader reader, final ModelNode node) throws XMLStreamException {
        final String value = reader.getAttributeValue(null, attribute.getName());
        if (value != null) {
            attribute.parseAndSetParameter(reader.getAttributeValue(null, attribute.getName()), node, reader);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();
        if (node.hasDefined(ASYNC_HANDLER)) {
            final ModelNode handlers = node.get(ASYNC_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (!handler.isDefined()) {
                    continue;
                }
                writeAsynchHandler(writer, handler, name);
            }
        }
        if (node.hasDefined(CONSOLE_HANDLER)) {
            final ModelNode handlers = node.get(CONSOLE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (!handler.isDefined()) {
                    continue;
                }
                writeConsoleHandler(writer, handler, name);
            }
        }
        if (node.hasDefined(FILE_HANDLER)) {
            final ModelNode handlers = node.get(FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (!handler.isDefined()) {
                    continue;
                }
                writeFileHandler(writer, handler, name);
            }
        }
        if (node.hasDefined(CUSTOM_HANDLER)) {
            final ModelNode handlers = node.get(CUSTOM_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (!handler.isDefined()) {
                    continue;
                }
                writeCustomHandler(writer, handler, name);
            }
        }
        if (node.hasDefined(PERIODIC_ROTATING_FILE_HANDLER)) {
            final ModelNode handlers = node.get(PERIODIC_ROTATING_FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (!handler.isDefined()) {
                    continue;
                }
                writePeriodicRotatingFileHandler(writer, handler, name);
            }
        }
        if (node.hasDefined(SIZE_ROTATING_FILE_HANDLER)) {
            final ModelNode handlers = node.get(SIZE_ROTATING_FILE_HANDLER);

            for (Property handlerProp : handlers.asPropertyList()) {
                final String name = handlerProp.getName();
                final ModelNode handler = handlerProp.getValue();
                if (!handler.isDefined()) {
                    continue;
                }
                writeSizeRotatingFileHandler(writer, handler, name);
            }
        }
        if (node.hasDefined(LOGGER)) {
            for (String name : node.get(LOGGER).keys()) {
                writeLogger(writer, name, node.get(LOGGER, name));
            }
        }
        if (node.hasDefined(ROOT_LOGGER)) {
            writeRootLogger(writer, node.get(ROOT_LOGGER, ROOT_LOGGER_NAME));
        }
        writer.writeEndElement();
    }

    private void writeConsoleHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CONSOLE_HANDLER.getLocalName());
        writer.writeAttribute(NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        if (TARGET.isMarshallable(node)) {
            writer.writeStartElement(Element.TARGET.getLocalName());
            writeAttribute(writer, Attribute.NAME, node.get(TARGET.getName()));
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.FILE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        writeFile(writer, node);
        writeAppend(writer, node);

        writer.writeEndElement();
    }

    private void writeCustomHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CUSTOM_HANDLER.getLocalName());
        writer.writeAttribute(NAME.getXmlName(), name);
        CLASS.marshallAsAttribute(node, writer);
        MODULE.marshallAsAttribute(node, writer);
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);

        writer.writeEndElement();
    }

    private void writePeriodicRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.PERIODIC_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        writeFile(writer, node);
        if (SUFFIX.isMarshallable(node)) {
            writer.writeStartElement(Element.SUFFIX.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(SUFFIX.getName()));
            writer.writeEndElement();
        }
        writeAppend(writer, node);

        writer.writeEndElement();
    }

    private void writeSizeRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.SIZE_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(NAME.getXmlName(), name);
        AUTOFLUSH.marshallAsAttribute(node, writer);
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        writeFile(writer, node);
        if (ROTATE_SIZE.isMarshallable(node)) {
            writer.writeStartElement(Element.ROTATE_SIZE.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(ROTATE_SIZE.getName()));
            writer.writeEndElement();
        }
        if (MAX_BACKUP_INDEX.isMarshallable(node)) {
            writer.writeStartElement(Element.MAX_BACKUP_INDEX.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(MAX_BACKUP_INDEX.getName()));
            writer.writeEndElement();
        }
        writeAppend(writer, node);

        writer.writeEndElement();
    }

    private void writeAsynchHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.ASYNC_HANDLER.getLocalName());
        writer.writeAttribute(NAME.getXmlName(), name);
        writeLevel(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        if (QUEUE_LENGTH.isMarshallable(node)) {
            writer.writeStartElement(Element.QUEUE_LENGTH.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(QUEUE_LENGTH.getName()));
            writer.writeEndElement();
        }
        if (OVERFLOW_ACTION.isMarshallable(node)) {
            writer.writeStartElement(Element.OVERFLOW_ACTION.getLocalName());
            writer.writeAttribute(Attribute.VALUE.getLocalName(), node.get(OVERFLOW_ACTION.getName()).asString().toLowerCase());
            writer.writeEndElement();
        }
        if (SUBHANDLERS.isMarshallable(node)) {
            final ModelNode handlers = node.get(SUBHANDLERS.getName());
            writeHandlersContent(writer, Element.SUBHANDLERS, handlers);
        }

        writer.writeEndElement();
    }

    private void writeLogger(final XMLExtendedStreamWriter writer, String name, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.LOGGER.getLocalName());
        writer.writeAttribute(CATEGORY.getXmlName(), name);
        USE_PARENT_HANDLERS.marshallAsAttribute(node, writer);
        writeLevel(writer, node);
        writeFilter(writer, node);
        writeHandlers(writer, node);
        writer.writeEndElement();
    }

    private void writeRootLogger(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.ROOT_LOGGER.getLocalName());
        writeLevel(writer, node);
        writeFilter(writer, node);
        writeHandlers(writer, node);
        writer.writeEndElement();
    }

    private void writeLevel(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (LEVEL.isMarshallable(node)) {
            writer.writeStartElement(Element.LEVEL.getLocalName());
            writeAttribute(writer, Attribute.NAME, node.get(LEVEL.getName()));
            writer.writeEndElement();
        }
    }

    private void writeFilter(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (FILTER.isMarshallable(node)) {
            final ModelNode filter = node.get(FILTER.getName());
            writer.writeStartElement(FILTER.getXmlName());
            writeFilterChildren(writer, filter);
            writer.writeEndElement();
        }
    }

    private void writeFilterChildren(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        doMarshal(writer, ACCEPT, null, node);
        if (ALL.isMarshallable(node)) {
            writer.writeStartElement(ALL.getXmlName());
            writeFilterChildren(writer, node.get(ALL.getName()));
            writer.writeEndElement();
        }
        if (ANY.isMarshallable(node)) {
            writer.writeStartElement(ANY.getXmlName());
            writeFilterChildren(writer, node.get(ANY.getName()));
            writer.writeEndElement();
        }
        doMarshal(writer, CHANGE_LEVEL, NEW_LEVEL, node);
        doMarshal(writer, DENY, null, node);
        writeLevel(writer, node);
        doMarshal(writer, LEVEL_RANGE, node, MIN_LEVEL, MIN_INCLUSIVE, MAX_LEVEL, MAX_INCLUSIVE);
        doMarshal(writer, MATCH, PATTERN, node);
        if (NOT.isMarshallable(node)) {
            writer.writeStartElement(NOT.getXmlName());
            writeFilterChildren(writer, node.get(NOT.getName()));
            writer.writeEndElement();
        }
        doMarshal(writer, REPLACE, node, PATTERN, REPLACEMENT, REPLACE_ALL);
    }

    private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(PROPERTIES)) {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());
            final List<Property> props = node.get(PROPERTIES).asPropertyList();
            for (Property prop : props) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), prop.getName());
                writeAttribute(writer, Attribute.VALUE, prop.getValue());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private void writeFormatter(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (FORMATTER.isMarshallable(node)) {
            writer.writeStartElement(Element.FORMATTER.getLocalName());
            writer.writeStartElement(Element.PATTERN_FORMATTER.getLocalName());
            writeAttribute(writer, Attribute.PATTERN, node.get(FORMATTER.getName()));
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private void writeFile(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (FILE.isMarshallable(node)) {
            writer.writeStartElement(Element.FILE.getLocalName());
            final ModelNode file = node.get(FILE.getName());
            RELATIVE_TO.marshallAsAttribute(file, writer);
            PATH.marshallAsAttribute(file, writer);
            writer.writeEndElement();
        }
    }

    private void writeEncoding(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (ENCODING.isMarshallable(node)) {
            writer.writeStartElement(Element.ENCODING.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(ENCODING.getName()));
            writer.writeEndElement();
        }
    }

    private void writeHandlers(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (HANDLERS.isMarshallable(node)) {
            final ModelNode handlers = node.get(HANDLERS.getName());
            writeHandlersContent(writer, Element.HANDLERS, handlers);
        }
    }

    private void writeHandlersContent(final XMLExtendedStreamWriter writer, Element element, final ModelNode handlers) throws XMLStreamException {
        if (handlers.getType() == ModelType.LIST) {
            writer.writeStartElement(element.getLocalName());
            for (ModelNode handler : handlers.asList()) {
                if (handler.isDefined()) {
                    writer.writeStartElement(Element.HANDLER.getLocalName());
                    writeAttribute(writer, Attribute.NAME, handler);
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
    }

    private void writeAppend(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (APPEND.isMarshallable(node)) {
            writer.writeStartElement(Element.APPEND.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(APPEND.getName()));
            writer.writeEndElement();
        }
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }

    private void doMarshal(final XMLExtendedStreamWriter writer, final AttributeDefinition element, final AttributeDefinition attribute, final ModelNode node) throws XMLStreamException {
        if (element.isMarshallable(node)) {
            if (attribute == null) {
                writer.writeEmptyElement(element.getXmlName());
            } else {
                writer.writeStartElement(element.getXmlName());
                writer.writeAttribute(attribute.getXmlName(), node.get(element.getXmlName()).asString());
                writer.writeEndElement();
            }
        }
    }

    private void doMarshal(final XMLExtendedStreamWriter writer, final AttributeDefinition element, final ModelNode node, final AttributeDefinition... attributes) throws XMLStreamException {
        if (element.isMarshallable(node)) {
            writer.writeStartElement(element.getXmlName());
            final ModelNode elementNode = node.get(element.getName());
            for (AttributeDefinition attribute : attributes) {
                if (attribute.isMarshallable(elementNode)) {
                    writer.writeAttribute(attribute.getXmlName(), elementNode.get(attribute.getName()).asString());
                }
            }
            writer.writeEndElement();
        }
    }
}
