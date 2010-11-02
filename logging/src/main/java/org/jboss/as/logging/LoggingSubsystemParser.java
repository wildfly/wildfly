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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.as.ExtensionContext;
import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import static org.jboss.as.model.ParseUtils.readStringAttributeElement;
import static org.jboss.as.model.ParseUtils.requireNoContent;

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

    /**
     * {@inheritDoc}
     */
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
        final Set<String> loggerNames = new HashSet<String>();
        final Set<String> handlerNames = new HashSet<String>();
        boolean gotRoot = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case LOGGING_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case LOGGER: {
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parseLoggerElement(reader, updates, loggerNames);
                            break;
                        }
                        case ROOT_LOGGER: {
                            if (gotRoot) {
                                throw unexpectedElement(reader);
                            }
                            gotRoot = true;
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parseRootLoggerElement(reader, updates);
                            break;
                        }
                        case CONSOLE_HANDLER: {
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parseConsoleHandlerElement(reader, updates, handlerNames);
                            break;
                        }
                        case FILE_HANDLER: {
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parseFileHandlerElement(reader, updates, handlerNames);
                            break;
                        }
                        case PERIODIC_ROTATING_FILE_HANDLER: {
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parsePeriodicRotatingFileHandlerElement(reader, updates, handlerNames);
                            break;
                        }
                        case SIZE_ROTATING_FILE_HANDLER: {
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parseSizeRotatingHandlerElement(reader, updates, handlerNames);
                            break;
                        }
                        case ASYNC_HANDLER: {
                            // http://youtrack.jetbrains.net/issue/IDEA-59290
                            //noinspection unchecked
                            parseAsyncHandlerElement(reader, updates, handlerNames);
                            break;
                        }
                        default: {
                            reader.handleAny(updates);
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

    private static void parseLoggerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list, final Set<String> names) throws XMLStreamException {

        // Attributes
        String name = null;
        boolean useParentHandlers = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.CATEGORY);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    case USE_PARENT_HANDLERS: {
                        useParentHandlers = Boolean.parseBoolean(value);
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        assert name != null;
        if (names.contains(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final LoggerAdd add = new LoggerAdd(name);
        add.setUseParentHandlers(useParentHandlers);

        // Elements
        List<String> handlers = null;
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
                            add.setLevelName(parseLevelElement(reader));
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
        list.add(add);
        if (handlers != null) {
            for (String handlerName : handlers) {
                list.add(new LoggerHandlerAdd(name, handlerName));
            }
        }
    }

    private static String parseLevelElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String level = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        level = value;
                        break;
                    }
                    default:
                        throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return level;
    }

    private static void parseRootLoggerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list) throws XMLStreamException {
        // No attributes
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }

        String level = null;
        // Elements
        List<String> handlers = null;
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
                            level = parseLevelElement(reader);
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
        final RootLoggerAdd add = new RootLoggerAdd();
        add.setLevelName(level);
        list.add(add);
        if (handlers != null) {
            for (String handlerName : handlers) {
                list.add(new LoggerHandlerAdd("", handlerName));
            }
        }
    }

    private static AbstractFormatterSpec parseFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            throw unexpectedAttribute(reader, 0);
        }
        AbstractFormatterSpec formatterSpec = null;
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

    private static PatternFormatterSpec parsePatternFormatterElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String pattern = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATTERN);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        final PatternFormatterSpec spec = new PatternFormatterSpec(pattern);
        return spec;
    }

    private static void parseConsoleHandlerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    case AUTOFLUSH: {
                        autoflush = Boolean.parseBoolean(value);
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

        // Elements
        String levelName = null;
        String encoding = null;
        AbstractFormatterSpec formatterSpec = null;
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
        if (names.contains(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final ConsoleHandlerAdd add = new ConsoleHandlerAdd(name);
        add.setAutoflush(Boolean.valueOf(autoflush));
        add.setLevelName(levelName);
        add.setEncoding(encoding);
        add.setFormatter(formatterSpec);
        if (target != null) add.setTarget(Target.fromString(target));
        list.add(add);
    }

    private static void parseFileHandlerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    case AUTOFLUSH: {
                        autoflush = Boolean.parseBoolean(value);
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

        // Elements
        String levelName = null;
        String encoding = null;
        FileSpec fileSpec = null;
        boolean append = true;
        AbstractFormatterSpec formatterSpec = null;

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
        if (names.contains(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final FileHandlerAdd add = new FileHandlerAdd(name);
        add.setAutoflush(Boolean.valueOf(autoflush));
        add.setLevelName(levelName);
        add.setEncoding(encoding);
        add.setFormatter(formatterSpec);
        add.setPath(fileSpec.fileName);
        add.setRelativeTo(fileSpec.relativeTo);
        add.setAppend(append);
        list.add(add);
    }

    private static FileSpec parseFileElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String path = null;
        String relativeTo = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.PATH);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            } else {
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
        }
        requireNoContent(reader);
        return new FileSpec(relativeTo, path);
    }

    private static void parsePeriodicRotatingFileHandlerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    case AUTOFLUSH: {
                        autoflush = Boolean.parseBoolean(value);
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

        // Elements
        String levelName = null;
        String encoding = null;
        String suffix = null;
        FileSpec fileSpec = null;
        boolean append = true;
        AbstractFormatterSpec formatterSpec = null;

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
        if (names.contains(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final PeriodicRotatingFileHandlerAdd add = new PeriodicRotatingFileHandlerAdd(name);
        add.setAutoflush(Boolean.valueOf(autoflush));
        add.setLevelName(levelName);
        add.setEncoding(encoding);
        add.setFormatter(formatterSpec);
        add.setPath(fileSpec.fileName);
        add.setRelativeTo(fileSpec.relativeTo);
        add.setAppend(append);
        add.setSuffix(suffix);
        list.add(add);
    }

    private static void parseSizeRotatingHandlerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    case AUTOFLUSH: {
                        autoflush = Boolean.parseBoolean(value);
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

        // Elements
        String levelName = null;
        String encoding = null;
        FileSpec fileSpec = null;
        boolean append = true;
        long rotateSize = 0L;
        int maxBackupIndex = 1;
        AbstractFormatterSpec formatterSpec = null;

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
        if (names.contains(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final SizeRotatingFileHandlerAdd add = new SizeRotatingFileHandlerAdd(name);
        add.setAutoflush(Boolean.valueOf(autoflush));
        add.setLevelName(levelName);
        add.setEncoding(encoding);
        add.setFormatter(formatterSpec);
        add.setPath(fileSpec.fileName);
        add.setRelativeTo(fileSpec.relativeTo);
        add.setAppend(append);
        if (rotateSize > 0L) {
            add.setRotateSize(rotateSize);
        }
        if (maxBackupIndex > 0) {
            add.setMaxBackupIndex(maxBackupIndex);
        }
        list.add(add);
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

    private static void parseAsyncHandlerElement(final XMLExtendedStreamReader reader, List<? super AbstractLoggingSubsystemUpdate<?>> list, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean autoflush = true;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.FILE_NAME, Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    default:
                        throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        // Elements
        String levelName = null;
        List<String> subhandlers = null;
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
        if (names.contains(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final AsyncHandlerAdd add = new AsyncHandlerAdd(name);
        if (subhandlers != null) add.setSubhandlers(subhandlers.toArray(new String[subhandlers.size()]));
        if (queueLength > 0) add.setQueueLength(queueLength);
        add.setOverflowAction(overflowAction);
        add.setAutoflush(Boolean.valueOf(autoflush));
        add.setLevelName(levelName);
        list.add(add);
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

    private static String parseRefElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        // Attributes
        String name = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
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
                    default:
                        throw unexpectedAttribute(reader, i);
                }
                required.remove(attribute);
            }
        }
        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
        return name;
    }

    private static final class FileSpec {

        private final String relativeTo;

        private final String fileName;

        private FileSpec(final String relativeTo, final String fileName) {
            this.relativeTo = relativeTo;
            this.fileName = fileName;
        }
    }
}
