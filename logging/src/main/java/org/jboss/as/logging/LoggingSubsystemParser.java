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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.duplicateNamedElement;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.logging.CommonAttributes.APPEND;
import static org.jboss.as.logging.CommonAttributes.ASYNC_HANDLER;
import static org.jboss.as.logging.CommonAttributes.AUTOFLUSH;
import static org.jboss.as.logging.CommonAttributes.CONSOLE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILE;
import static org.jboss.as.logging.CommonAttributes.FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.HANDLERS;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.CommonAttributes.LOGGER;
import static org.jboss.as.logging.CommonAttributes.MAX_BACKUP_INDEX;
import static org.jboss.as.logging.CommonAttributes.OVERFLOW_ACTION;
import static org.jboss.as.logging.CommonAttributes.PATH;
import static org.jboss.as.logging.CommonAttributes.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.logging.CommonAttributes.ROOT_LOGGER;
import static org.jboss.as.logging.CommonAttributes.ROTATE_SIZE;
import static org.jboss.as.logging.CommonAttributes.SIZE_ROTATING_FILE_HANDLER;
import static org.jboss.as.logging.CommonAttributes.SUBHANDLERS;
import static org.jboss.as.logging.CommonAttributes.SUFFIX;
import static org.jboss.as.logging.CommonAttributes.TARGET;
import static org.jboss.as.logging.CommonAttributes.USE_PARENT_HANDLERS;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
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

    private static final LoggingSubsystemParser INSTANCE = new LoggingSubsystemParser();

    public static LoggingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private LoggingSubsystemParser() {
        //
    }


    /** {@inheritDoc} */
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
                case LOGGING_1_0: {
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
        // Attributes
        String name = null;
        boolean useParentHandlers = true;
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
                    useParentHandlers = Boolean.parseBoolean(value);
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
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Element
        String level = null;
        ModelNode handlers = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (!encountered.add(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    switch (element) {
                        case LEVEL: {
                            level = readStringAttributeElement(reader, "name");
                            break;
                        }
                        case HANDLERS: {
                            handlers = parseHandlersElement(reader);
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

        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(LOGGER, name);
        node.get(USE_PARENT_HANDLERS).set(useParentHandlers);
        node.get(LEVEL).set(level);
        if(handlers != null) node.get(HANDLERS).set(handlers);
        list.add(node);
    }

    static void parseAsyncHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
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
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Elements
        String levelName = null;
        ModelNode subhandlers = null;
        int queueLength = 0;
        OverflowAction overflowAction = OverflowAction.BLOCK;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case LEVEL: {
                    levelName = readStringAttributeElement(reader, "name");
                    break;
                }
                case SUBHANDLERS: {
                    subhandlers = parseHandlersElement(reader);
                    break;
                }
                case QUEUE_LENGTH: {
                    queueLength = Integer.parseInt(readStringAttributeElement(reader, "value"));
                    break;
                }
                case OVERFLOW_ACTION: {
                    overflowAction = OverflowAction.valueOf(readStringAttributeElement(reader, "value").toUpperCase(Locale.US));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        // TODO - Only values set in the XML should be set on the model otherwise defaults end up being written when
        // marshalling.

        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(ASYNC_HANDLER, name);
        node.get(LEVEL).set(levelName);
        if(subhandlers != null) node.get(SUBHANDLERS).set(subhandlers);
        node.get(AUTOFLUSH).set(Boolean.valueOf(autoflush));
        node.get(QUEUE_LENGTH).set(queueLength);
        node.get(OVERFLOW_ACTION).set(overflowAction.toString());
        list.add(node);
    }

    static void parseRootLoggerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        String level = null;
        // Elements
        ModelNode handlers = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    if (encountered.contains(element)) {
                        throw duplicateNamedElement(reader, reader.getLocalName());
                    }
                    encountered.add(element);
                    switch (element) {
                        case LEVEL: {
                            level = readStringAttributeElement(reader, "name");
                            break;
                        }
                        case HANDLERS: {
                            handlers = parseHandlersElement(reader);
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

        final ModelNode node = new ModelNode();
        node.get(OP).set(RootLoggerAdd.OPERATION_NAME);
        node.get(OP_ADDR).set(address);
        node.get(LEVEL).set(level);
        if(handlers != null) node.get(HANDLERS).set(handlers);
        list.add(node);
    }

    static void parseConsoleHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
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
                    autoflush = Boolean.parseBoolean(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Elements
        String levelName = null;
        String encoding = null;
        String formatterSpec = null;
        String target = null;
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            final Element element = Element.forName(reader.getLocalName());
            if (!encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case LEVEL: {
                    levelName = readStringAttributeElement(reader, "name");
                    break;
                }
                case ENCODING: {
                    encoding = readStringAttributeElement(reader, "value");
                    break;
                }
                case FORMATTER: {
                    formatterSpec = parseFormatterElement(reader);
                    break;
                }
                case TARGET: {
                    target = readStringAttributeElement(reader, "name");
                    if (!(target.equals("System.out") || target.equals("System.err"))) {
                        throw new XMLStreamException("Invalid value for target name", reader.getLocation());
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(CONSOLE_HANDLER, name);
        node.get(AUTOFLUSH).set(autoflush);
        node.get(LEVEL).set(levelName);
        if(formatterSpec != null) node.get(FORMATTER).set(formatterSpec);
        if(encoding != null) node.get(ENCODING).set(encoding);
        list.add(node);
    }

    static void parseFileHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
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
                    autoflush = Boolean.parseBoolean(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Elements
        String levelName = null;
        String encoding = null;
        ModelNode fileSpec = null;
        boolean append = true;
        String formatterSpec = null;

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
                    levelName = readStringAttributeElement(reader, "name");
                    break;
                }
                case ENCODING: {
                    encoding = readStringAttributeElement(reader, "value");
                    break;
                }
                case FORMATTER: {
                    formatterSpec = parseFormatterElement(reader);
                    break;
                }
                case FILE: {
                    fileSpec = parseFileElement(reader);
                    break;
                }
                case APPEND: {
                    append = Boolean.parseBoolean(readStringAttributeElement(reader, "value"));
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!requiredElem.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(FILE_HANDLER, name);
        node.get(AUTOFLUSH).set(autoflush);
        node.get(LEVEL).set(levelName);
        if(encoding != null) node.get(ENCODING).set(encoding);
        if(formatterSpec != null) node.get(FORMATTER).set(formatterSpec);
        node.get(FILE).set(fileSpec);
        node.get(APPEND).set(append);
        list.add(node);
    }

    static void parsePeriodicRotatingFileHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
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
                    autoflush = Boolean.parseBoolean(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Elements
        String levelName = null;
        String encoding = null;
        String suffix = null;
        ModelNode fileSpec = null;
        boolean append = true;
        String formatterSpec = null;

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
                    levelName = readStringAttributeElement(reader, "name");
                    break;
                }
                case ENCODING: {
                    encoding = readStringAttributeElement(reader, "value");
                    break;
                }
                case FORMATTER: {
                    formatterSpec = parseFormatterElement(reader);
                    break;
                }
                case FILE: {
                    fileSpec = parseFileElement(reader);
                    break;
                }
                case APPEND: {
                    append = Boolean.parseBoolean(readStringAttributeElement(reader, "value"));
                    break;
                }
                case SUFFIX: {
                    suffix = readStringAttributeElement(reader, "value");
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!requiredElem.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(PERIODIC_ROTATING_FILE_HANDLER, name);
        node.get(AUTOFLUSH).set(autoflush);
        node.get(LEVEL).set(levelName);
        if(encoding != null) node.get(ENCODING).set(encoding);
        if(formatterSpec != null) node.get(FORMATTER).set(formatterSpec);
        node.get(FILE).set(fileSpec);
        node.get(APPEND).set(append);
        if(suffix != null) node.get(SUFFIX).set(suffix);
        list.add(node);
    }

    static void parseSizeRotatingHandlerElement(final XMLExtendedStreamReader reader, final ModelNode address, final List<ModelNode> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
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
                    autoflush = Boolean.parseBoolean(value);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        // Elements
        String levelName = null;
        String encoding = null;
        ModelNode fileSpec = null;
        boolean append = true;
        long rotateSize = 0L;
        int maxBackupIndex = 1;
        String formatterSpec = null;

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
                    levelName = readStringAttributeElement(reader, "name");
                    break;
                }
                case ENCODING: {
                    encoding = readStringAttributeElement(reader, "value");
                    break;
                }
                case FORMATTER: {
                    formatterSpec = parseFormatterElement(reader);
                    break;
                }
                case FILE: {
                    fileSpec = parseFileElement(reader);
                    break;
                }
                case APPEND: {
                    append = Boolean.parseBoolean(readStringAttributeElement(reader, "value"));
                    break;
                }
                case ROTATE_SIZE: {
                    rotateSize = parseSize(readStringAttributeElement(reader, "value"));
                    break;
                }
                case MAX_BACKUP_INDEX: {
                    try {
                        maxBackupIndex = Integer.parseInt(readStringAttributeElement(reader, "value"));
                    } catch (NumberFormatException e) {
                        throw new XMLStreamException(e.getMessage(), reader.getLocation(), e);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!requiredElem.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ModelNode node = new ModelNode();
        node.get(OP).set(ADD);
        node.get(OP_ADDR).set(address).add(SIZE_ROTATING_FILE_HANDLER, name);
        node.get(AUTOFLUSH).set(autoflush);
        node.get(LEVEL).set(levelName);
        if(encoding != null) node.get(ENCODING).set(encoding);
        if(formatterSpec != null) node.get(FORMATTER).set(formatterSpec);
        node.get(FILE).set(fileSpec);
        node.get(APPEND).set(append);
        if (rotateSize > 0L) {
            node.get(ROTATE_SIZE).set(rotateSize);
        }
        if (maxBackupIndex > 0) {
            node.get(MAX_BACKUP_INDEX).set(maxBackupIndex);
        }
        list.add(node);
    }

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)([kKmMgGbBtT])?");

    private static long parseSize(final String value) {
        final Matcher matcher = SIZE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        long qty = Long.parseLong(matcher.group(1), 10);
        final String chr = matcher.group(2);
        if (chr != null) {
            switch (chr.charAt(0)) {
                case 'b':
                case 'B':
                    break;
                case 'k':
                case 'K':
                    qty <<= 10L;
                    break;
                case 'm':
                case 'M':
                    qty <<= 20L;
                    break;
                case 'g':
                case 'G':
                    qty <<= 30L;
                    break;
                case 't':
                case 'T':
                    qty <<= 40L;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        return qty;
    }

    private static ModelNode parseFileElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String path = null;
        String relativeTo = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
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
        requireNoContent(reader);
        final ModelNode node = new ModelNode();
        if(path != null) node.get(PATH).set(path);
        if(relativeTo != null) node.get(RELATIVE_TO).set(relativeTo);
        return node;
    }

    private static String parseFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        String formatterSpec = null;
        if (reader.nextTag() != START_ELEMENT) {
            throw new XMLStreamException("Missing required nested filter element", reader.getLocation());
        }
        switch (Namespace.forUri(reader.getNamespaceURI())) {
            case LOGGING_1_0: {
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

    private static ModelNode parseHandlersElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        final ModelNode handlers = new ModelNode();

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HANDLER: {
                            handlers.add(readStringAttributeElement(reader, "name"));
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
        return handlers;
    }

    /** {@inheritDoc} */
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
            writeRootLogger(writer, node.get(ROOT_LOGGER));
        }
        writer.writeEndElement();
    }

    private void writeConsoleHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name)
            throws XMLStreamException {
        writer.writeStartElement(Element.CONSOLE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (node.hasDefined(AUTOFLUSH)) {
            writeAttribute(writer, Attribute.AUTOFLUSH, node.get(AUTOFLUSH));
        }
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        if (node.hasDefined(TARGET)) {
            writer.writeStartElement(Element.TARGET.getLocalName());
            writeAttribute(writer, Attribute.NAME, node.get(TARGET));
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.FILE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (node.hasDefined(AUTOFLUSH)) {
            writeAttribute(writer, Attribute.AUTOFLUSH, node.get(AUTOFLUSH));
        }
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        writeFile(writer, node);
        writeAppend(writer, node);

        writer.writeEndElement();
    }

    private void writePeriodicRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.PERIODIC_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (node.hasDefined(AUTOFLUSH)) {
            writeAttribute(writer, Attribute.AUTOFLUSH, node.get(AUTOFLUSH));
        }
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        writeFile(writer, node);
        if (node.hasDefined(SUFFIX)) {
            writer.writeStartElement(Element.SUFFIX.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(SUFFIX));
            writer.writeEndElement();
        }
        writeAppend(writer, node);

        writer.writeEndElement();
    }

    private void writeSizeRotatingFileHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.SIZE_ROTATING_FILE_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        if (node.hasDefined(AUTOFLUSH)) {
            writeAttribute(writer, Attribute.AUTOFLUSH, node.get(AUTOFLUSH));
        }
        writeLevel(writer, node);
        writeEncoding(writer, node);
        writeFilter(writer, node);
        writeFormatter(writer, node);
        writeProperties(writer, node);
        writeFile(writer, node);
        if (node.hasDefined(ROTATE_SIZE)) {
            writer.writeStartElement(Element.ROTATE_SIZE.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(ROTATE_SIZE));
            writer.writeEndElement();
        }
        if (node.hasDefined(MAX_BACKUP_INDEX)) {
            writer.writeStartElement(Element.MAX_BACKUP_INDEX.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(MAX_BACKUP_INDEX));
            writer.writeEndElement();
        }
        writeAppend(writer, node);

        writer.writeEndElement();
    }

    private void writeAsynchHandler(final XMLExtendedStreamWriter writer, final ModelNode node, final String name) throws XMLStreamException {
        writer.writeStartElement(Element.ASYNC_HANDLER.getLocalName());
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);
        writeLevel(writer, node);
        writeFilter(writer, node);
        writeProperties(writer, node);
        if (node.hasDefined(QUEUE_LENGTH)) {
            writer.writeStartElement(Element.QUEUE_LENGTH.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(QUEUE_LENGTH));
            writer.writeEndElement();
        }
        if (node.hasDefined(OVERFLOW_ACTION)) {
            writer.writeStartElement(Element.OVERFLOW_ACTION.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(OVERFLOW_ACTION));
            writer.writeEndElement();
        }
        if (node.hasDefined(SUBHANDLERS)) {
            final ModelNode handlers = node.get(SUBHANDLERS);
            writeHandlersContent(writer, Element.SUBHANDLERS, handlers);
        }

        writer.writeEndElement();
    }

    private void writeLogger(final XMLExtendedStreamWriter writer, String name, final ModelNode node) throws XMLStreamException {
        writer.writeStartElement(Element.LOGGER.getLocalName());
        writer.writeAttribute(Attribute.CATEGORY.getLocalName(), name);
        if (node.hasDefined(USE_PARENT_HANDLERS)) {
            writeAttribute(writer, Attribute.USE_PARENT_HANDLERS, node.get(USE_PARENT_HANDLERS));
        }
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
        if (node.hasDefined(LEVEL)) {
            writer.writeStartElement(Element.LEVEL.getLocalName());
            writeAttribute(writer, Attribute.NAME, node.get(LEVEL));
            writer.writeEndElement();
        }
    }

    private void writeFilter(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        //TODO - we're not parsing it yet
    }

    private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        //TODO - we're not parsing it yet
    }

    private void writeFormatter(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(FORMATTER)) {
            writer.writeStartElement(Element.FORMATTER.getLocalName());
            writer.writeStartElement(Element.PATTERN_FORMATTER.getLocalName());
            writeAttribute(writer, Attribute.PATTERN, node.get(FORMATTER));
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private void writeFile(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(FILE)) {
            writer.writeStartElement(Element.FILE.getLocalName());
            final ModelNode file = node.get(FILE);
            if (file.hasDefined(RELATIVE_TO)) {
                writeAttribute(writer, Attribute.RELATIVE_TO, file.get(RELATIVE_TO));
            }
            if (file.hasDefined(PATH)) {
                writeAttribute(writer, Attribute.PATH, file.get(PATH));
            }
            writer.writeEndElement();
        }
    }

    private void writeEncoding(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(ENCODING)) {
            writer.writeStartElement(Element.ENCODING.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(ENCODING));
            writer.writeEndElement();
        }
    }

    private void writeHandlers(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        if (node.hasDefined(HANDLERS)) {
            final ModelNode handlers = node.get(HANDLERS);
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
        if (node.hasDefined(APPEND)) {
            writer.writeStartElement(Element.APPEND.getLocalName());
            writeAttribute(writer, Attribute.VALUE, node.get(APPEND));
            writer.writeEndElement();
        }
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }

}
