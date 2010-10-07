/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext;
import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.PropertiesElement;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LoggingSubsystemParser implements XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<LoggingSubsystemElement>>>, XMLStreamConstants {



    private static final LoggingSubsystemParser INSTANCE = new LoggingSubsystemParser();

    /**
     * @return the instance
     */
    public static LoggingSubsystemParser getInstance() {
        return INSTANCE;
    }

    private static XMLStreamException unexpectedAttribute(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Unexpected attribute '" + reader.getAttributeName(index) + "' encountered", reader.getLocation());
    }

    private static XMLStreamException duplicateNamedElement(final XMLExtendedStreamReader reader, final String name) {
        return new XMLStreamException("An element of this type named '" + name + "' has already been declared", reader.getLocation());
    }

    private static XMLStreamException unexpectedElement(final XMLExtendedStreamReader reader) {
        return new XMLStreamException("Unexpected element '" + reader.getName() + "' encountered", reader.getLocation());
    }

    private static XMLStreamException invalidAttributeValue(final XMLExtendedStreamReader reader, final int index) {
        return new XMLStreamException("Invalid value '" + reader.getAttributeValue(index) + "' for attribute '" + reader.getAttributeName(index));
    }

    private static XMLStreamException missingRequired(final XMLExtendedStreamReader reader, final Set<?> required) {
        final StringBuilder b = new StringBuilder();
        Iterator<?> iterator = required.iterator();
        while (iterator.hasNext()) {
            final Object o = iterator.next();
            b.append(o.toString());
            if (iterator.hasNext()) {
                b.append(", ");
            }
        }
        return new XMLStreamException("Missing required attribute(s): " + b, reader.getLocation());
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<LoggingSubsystemElement>> result)
            throws XMLStreamException {
        final List<AbstractSubsystemUpdate<LoggingSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<LoggingSubsystemElement, ?>>();
        readElement(reader, updates);
        result.setResult(new ExtensionContext.SubsystemConfiguration<LoggingSubsystemElement>(new LoggingSubsystemAdd(), updates));
    }

    void readElement(XMLExtendedStreamReader reader, List<? super AbstractSubsystemUpdate<LoggingSubsystemElement, ?>> updates)
            throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        // Elements
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
                        case LOGGER: {
                            LoggerAdd add = parseLoggerElement(reader);
                            updates.add(add);
                            break;
                        }
                        case ROOT_LOGGER: {
//                            RootLoggerAdd add = parseRootLoggerElement(reader);
//                            updates.add(add);
                            break;
                        }
                        case CONSOLE_HANDLER: {
//                            ConsoleHandlerAdd add = parseConsoleHandlerElement(reader);
//                            updates.add(new HandlerAdd(add));
                            break;
                        }
                        case FILE_HANDLER: {
//                            FileHandlerElement handlerElement = parseFileHandlerElement(reader);
//                            updates.add(new HandlerAdd(handlerElement));
                            break;
                        }
                        case PERIODIC_ROTATING_FILE_HANDLER: {
//                            PeriodicRotatingFileHandlerElement handlerElement = parsePeriodicRotatingFileHandlerElement(reader);
//                            updates.add(new HandlerAdd(handlerElement));
                            break;
                        }
                        case SIZE_ROTATING_FILE_HANDLER: {
//                            SizeRotatingFileHandlerElement handlerElement = parseSizeRotatingHandlerElement(reader);
//                            updates.add(new HandlerAdd(handlerElement));
                            break;
                        }
                        case ASYNC_HANDLER: {
//                            AsyncHandlerElement handlerElement = parseAsyncHandlerElement(reader);
//                            updates.add(new HandlerAdd(handlerElement));
                            break;
                        }
                        default: {
                            final ParseResult<AbstractHandlerElement<?>> result = new ParseResult<AbstractHandlerElement<?>>();
                            reader.handleAny(result);
                            AbstractHandlerElement<?> handlerElement = result.getResult();
//                            updates.add(new HandlerAdd(handlerElement));
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

    private static LoggerAdd parseLoggerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {

        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case CATEGORY: {
                        name = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        assert name != null;
        final LoggerAdd add = new LoggerAdd(name);
        Level level = null;

        // Elements
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
                            add.setLevel(parseLevelElement(reader));
                            break;
                        }
                        case FILTER: {
//                            add.setFilter(parseFilterElement(reader));
                            break;
                        }
                        case HANDLERS: {
//                            add.setHandlers(parseHandlersElement(reader));
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return add;
    }

    private static Level parseLevelElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        Level level = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        try {
                            level = Level.parse(value);
                        } catch (IllegalArgumentException e) {
                            throw invalidAttributeValue(reader, i);
                        }
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return level;
    }

    private static RootLoggerElement parseRootLoggerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        final RootLoggerElement loggerElement = new RootLoggerElement();

        // Elements
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
                            loggerElement.setLevel(parseLevelElement(reader));
                            break;
                        }
                        case FILTER: {
                            loggerElement.setFilter(parseFilterElement(reader));
                            break;
                        }
                        case HANDLERS: {
                            loggerElement.setHandlers(parseHandlersElement(reader));
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return loggerElement;
    }

    private static void readHandlerChildren(final AbstractHandlerElement<?> handlerElement, final XMLExtendedStreamReader reader) throws XMLStreamException {
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
                            handlerElement.setLevel(parseLevelElement(reader));
                            break;
                        }
                        case FILTER: {
                            handlerElement.setFilter(parseFilterElement(reader));
                            break;
                        }
                        case FORMATTER: {
                            handlerElement.setFormatter(parseFormatterElement(reader));
                            break;
                        }
                        case SUBHANDLERS: {
//                            handlerElement.setSubhandlers(parseHandlersElement(reader));
                            break;
                        }
                        case PROPERTIES: {
                            handlerElement.setProperties(parsePropertiesElement(reader));
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                }
                default: throw unexpectedElement(reader);
            }
        }
    }

    private static PropertiesElement parsePropertiesElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return new PropertiesElement(reader);
    }

    private static AbstractFormatterElement<?> parseFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        AbstractFormatterElement<?> formatterElement = null;
        if (reader.nextTag() != START_ELEMENT) {
            throw new XMLStreamException("Missing required nested filter element", reader.getLocation());
        }
        switch (Namespace.forUri(reader.getNamespaceURI())) {
            case LOGGING_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case PATTERN_FORMATTER: {
                        formatterElement = parsePatternFormatterElement(reader);
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
            default: {
                final ParseResult<AbstractFormatterElement<?>> result = new ParseResult<AbstractFormatterElement<?>>();
                reader.handleAny(result);
                formatterElement = result.getResult();
                break;
            }
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return formatterElement;
    }

    private static PatternFormatterElement parsePatternFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String pattern = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATTERN);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                required.remove(attribute);
                switch (attribute) {
                    case PATTERN: {
                        pattern = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        final PatternFormatterElement element = new PatternFormatterElement(pattern);
//        element.setPattern(pattern);
        return element;
    }

    private static ConsoleHandlerElement parseConsoleHandlerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String encoding = null;
        Boolean autoflush = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.FILE_NAME, Attribute.NAME);
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
                        break;
                    }
                    case ENCODING: {
                        encoding = value;
                        break;
                    }
                    case AUTOFLUSH: {
                        autoflush = Boolean.valueOf(value);
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final ConsoleHandlerElement handlerElement = new ConsoleHandlerElement(null, name);
        if (encoding != null) handlerElement.setEncoding(encoding);
        if (autoflush != null) handlerElement.setAutoflush(autoflush);
        readHandlerChildren(handlerElement, reader);
        return handlerElement;
    }

    private static FileHandlerElement parseFileHandlerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String encoding = null;
        String fileName = null;
        Boolean append = null;
        Boolean autoflush = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.FILE_NAME, Attribute.NAME);
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
                        break;
                    }
                    case ENCODING: {
                        encoding = value;
                        break;
                    }
                    case AUTOFLUSH: {
                        autoflush = Boolean.valueOf(value);
                        break;
                    }
                    case FILE_NAME: {
                        fileName = value;
                        break;
                    }
                    case APPEND: {
                        append = Boolean.valueOf(value);
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final FileHandlerElement handlerElement = new FileHandlerElement(name);
        handlerElement.setFileName(fileName);
        if (encoding != null) handlerElement.setEncoding(encoding);
        if (autoflush != null) handlerElement.setAutoflush(autoflush);
        if (append != null) handlerElement.setAppend(append.booleanValue());
        readHandlerChildren(handlerElement, reader);
        return handlerElement;
    }

    private static PeriodicRotatingFileHandlerElement parsePeriodicRotatingFileHandlerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String encoding = null;
        String fileName = null;
        String suffix = null;
        Boolean append = null;
        Boolean autoflush = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.FILE_NAME, Attribute.SUFFIX, Attribute.NAME);
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
                        break;
                    }
                    case ENCODING: {
                        encoding = value;
                        break;
                    }
                    case AUTOFLUSH: {
                        autoflush = Boolean.valueOf(value);
                        break;
                    }
                    case FILE_NAME: {
                        fileName = value;
                        break;
                    }
                    case APPEND: {
                        append = Boolean.valueOf(value);
                        break;
                    }
                    case SUFFIX: {
                        suffix = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final PeriodicRotatingFileHandlerElement handlerElement = new PeriodicRotatingFileHandlerElement(name);
        handlerElement.setFileName(fileName);
        handlerElement.setSuffix(suffix);
        if (encoding != null) handlerElement.setEncoding(encoding);
        if (autoflush != null) handlerElement.setAutoflush(autoflush);
        if (append != null) handlerElement.setAppend(append.booleanValue());
        readHandlerChildren(handlerElement, reader);
        return handlerElement;
    }

    private static SizeRotatingFileHandlerElement parseSizeRotatingHandlerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        String encoding = null;
        String fileName = null;
        int maxBackupIndex = -1;
        long rotateSize = -1L;
        Boolean append = null;
        Boolean autoflush = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.FILE_NAME, Attribute.SUFFIX, Attribute.NAME);
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
                        break;
                    }
                    case ENCODING: {
                        encoding = value;
                        break;
                    }
                    case AUTOFLUSH: {
                        autoflush = Boolean.valueOf(value);
                        break;
                    }
                    case FILE_NAME: {
                        fileName = value;
                        break;
                    }
                    case APPEND: {
                        append = Boolean.valueOf(value);
                        break;
                    }
                    case MAX_BACKUP_INDEX: {
                        try {
                            maxBackupIndex = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            throw invalidAttributeValue(reader, i);
                        }
                        if (maxBackupIndex < 1) {
                            throw invalidAttributeValue(reader, i);
                        }
                        break;
                    }
                    case ROTATE_SIZE: {
                        try {
                            rotateSize = parseSize(value);
                        } catch (IllegalArgumentException e) {
                            throw invalidAttributeValue(reader, i);
                        }
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final SizeRotatingFileHandlerElement handlerElement = new SizeRotatingFileHandlerElement(name);
        handlerElement.setFileName(fileName);
        if (maxBackupIndex > 0) handlerElement.setMaxBackupIndex(maxBackupIndex);
        if (rotateSize > 0) handlerElement.setRotateSize(rotateSize);
        if (encoding != null) handlerElement.setEncoding(encoding);
        if (autoflush != null) handlerElement.setAutoflush(autoflush);
        if (append != null) handlerElement.setAppend(append.booleanValue());
        readHandlerChildren(handlerElement, reader);
        return handlerElement;
    }

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)([kKmMgGbBtT])?");

    private static long parseSize(final String value) {
        final Matcher matcher = SIZE_PATTERN.matcher(value);
        if (! matcher.matches()) {
            throw new IllegalArgumentException();
        }
        long qty = Long.parseLong(matcher.group(1), 10);
        final String chr = matcher.group(2);
        if (chr != null) {
            switch (chr.charAt(0)) {
                case 'b': case 'B': break;
                case 'k': case 'K': qty <<= 10L; break;
                case 'm': case 'M': qty <<= 20L; break;
                case 'g': case 'G': qty <<= 30L; break;
                case 't': case 'T': qty <<= 40L; break;
                default: throw new IllegalStateException();
            }
        }
        return qty;
    }

    private static AsyncHandlerElement parseAsyncHandlerElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        int queueLength = 512;
        OverflowAction overflowAction = OverflowAction.BLOCK;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.FILE_NAME, Attribute.NAME);
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
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        final AsyncHandlerElement handlerElement = new AsyncHandlerElement(name);
        handlerElement.setQueueLength(queueLength);
        handlerElement.setOverflowAction(overflowAction);
        readHandlerChildren(handlerElement, reader);
        return handlerElement;
    }

    private static FilterElement parseFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        final FilterElement element = new FilterElement(parseSimpleFilter(reader));
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return element;
    }

    private static FilterType parseSimpleFilter(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final FilterType filterElement;
        switch (Namespace.forUri(reader.getNamespaceURI())) {
            case LOGGING_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case ALL: {
                        filterElement = parseAllFilterElement(reader);
                        break;
                    }
                    case ANY: {
                        filterElement = parseAnyFilterElement(reader);
                        break;
                    }
                    case ACCEPT: {
                        filterElement = parseAcceptFilterElement(reader);
                        break;
                    }
                    case DENY: {
                        filterElement = parseDenyFilterElement(reader);
                        break;
                    }
                    case NOT: {
                        filterElement = parseNotFilterElement(reader);
                        break;
                    }
                    case MATCH: {
                        filterElement = parseMatchFilterElement(reader);
                        break;
                    }
                    case REPLACE: {
                        filterElement = parseReplaceFilterElement(reader);
                        break;
                    }
                    case LEVEL: {
                        filterElement = parseLevelFilterElement(reader);
                        break;
                    }
                    case LEVEL_RANGE: {
                        filterElement = parseLevelRangeFilterElement(reader);
                        break;
                    }
                    case CHANGE_LEVEL: {
                        filterElement = parseChangeLevelFilterElement(reader);
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
        return filterElement;
    }

    private static FilterType parseChangeLevelFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String newLevel = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NEW_LEVEL);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NEW_LEVEL: {
                        newLevel = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return new ChangeLevelFilterType(newLevel);
    }

    private static FilterType parseLevelRangeFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String minLevel = null;
        String maxLevel = null;
        boolean minInclusive = true;
        boolean maxInclusive = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.MIN_LEVEL, Attribute.MAX_LEVEL);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case MIN_LEVEL: {
                        minLevel = value;
                        break;
                    }
                    case MAX_LEVEL: {
                        maxLevel = value;
                        break;
                    }
                    case MIN_INCLUSIVE: {
                        minInclusive = Boolean.parseBoolean(value);
                        break;
                    }
                    case MAX_INCLUSIVE: {
                        maxInclusive = Boolean.parseBoolean(value);
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return new LevelRangeFilterType(minLevel, minInclusive, maxLevel, maxInclusive);
    }

    private static FilterType parseLevelFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        return new LevelFilterType(parseRefElement(reader));
    }

    private static FilterType parseReplaceFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String pattern = null;
        String replacement = null;
        boolean replaceAll = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATTERN, Attribute.REPLACEMENT);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATTERN: {
                        pattern = value;
                        break;
                    }
                    case REPLACEMENT: {
                        replacement = value;
                        break;
                    }
                    case REPLACE_ALL: {
                        replaceAll = Boolean.parseBoolean(value);
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return new ReplaceFilterType(pattern, replacement, replaceAll);
    }

    private static FilterType parseMatchFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String pattern = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATTERN);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATTERN: {
                        pattern = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return new PatternFilterType(pattern);
    }

    private static FilterType parseNotFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        if (reader.nextTag() != START_ELEMENT) {
            throw new XMLStreamException("Expected filter element");
        }
        return new NotFilterType(parseSimpleFilter(reader));
    }

    private static FilterType parseAcceptFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return FilterType.ACCEPT;
    }

    private static FilterType parseDenyFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return FilterType.DENY;
    }

    private static AllFilterType parseAllFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        List<FilterType> list = new ArrayList<FilterType>();
        while (reader.nextTag() == START_ELEMENT) {
            list.add(parseSimpleFilter(reader));
        }
        return new AllFilterType(list.toArray(new FilterType[list.size()]));
    }

    private static AnyFilterType parseAnyFilterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        List<FilterType> list = new ArrayList<FilterType>();
        while (reader.nextTag() == START_ELEMENT) {
            list.add(parseSimpleFilter(reader));
        }
        return new AnyFilterType(list.toArray(new FilterType[list.size()]));
    }

    private static List<String> parseHandlersElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        final List<String> handlers = new ArrayList<String>();

        // Elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case HANDLER: {
                            handlers.add(parseRefElement(reader));
                            break;
                        }
                        default: throw unexpectedElement(reader);
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

    private static String parseRefElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    }
                    default: throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return name;
    }
}
