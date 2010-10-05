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

package org.jboss.as.threads;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jboss.as.ExtensionContext;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamException;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.jboss.as.model.ParseUtils.*;

/**
 * The root element handler for threads subsystem elements.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadsParser implements XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<ThreadsSubsystemElement>>> {

    private static final ThreadsParser INSTANCE = new ThreadsParser();

    private ThreadsParser() {
    }

    /**
     * Get the instance.
     *
     * @return the instance
     */
    public static ThreadsParser getInstance() {
        return INSTANCE;
    }

    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<ExtensionContext.SubsystemConfiguration<ThreadsSubsystemElement>> result) throws XMLStreamException {

        final List<AbstractSubsystemUpdate<ThreadsSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<ThreadsSubsystemElement,?>>();

        // no attributes
        requireNoAttributes(reader);

        final Set<String> threadFactoryNames = new HashSet<String>();
        final Set<String> executorNames = new HashSet<String>();
        final Set<String> scheduledExecutorNames = new HashSet<String>();

        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case THREADS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case THREAD_FACTORY: {
                            //noinspection unchecked
                            parseThreadFactoryElement(reader, updates, threadFactoryNames);
                            break;
                        }
                        case SCHEDULED_THREAD_POOL: {
                            //noinspection unchecked
                            parseScheduledExecutorElement(reader, updates, scheduledExecutorNames);
                            break;
                        }
                        case BOUNDED_QUEUE_THREAD_POOL: {
                            parseBoundedQueueExecutorElement(reader, updates, executorNames);
                            break;
                        }
                        case QUEUELESS_THREAD_POOL: {
                            parseQueuelessExecutorElement(reader, updates, executorNames);
                            break;
                        }
                        case UNBOUNDED_QUEUE_THREAD_POOL: {
                            parseUnboundedQueueExecutorElement(reader, updates, executorNames);
                            break;
                        }
                        default: throw unexpectedElement(reader);
                    }
                    break;
                }
                default: throw unexpectedElement(reader);
            }
        }
        result.setResult(new ExtensionContext.SubsystemConfiguration<ThreadsSubsystemElement>(new ThreadsSubsystemAdd(), updates));
    }

    private void parseUnboundedQueueExecutorElement(final XMLExtendedStreamReader reader, final List<? super AbstractThreadsSubsystemUpdate<?>> updates, final Set<String> names) {
    }

    private void parseQueuelessExecutorElement(final XMLExtendedStreamReader reader, final List<? super AbstractThreadsSubsystemUpdate<?>> updates, final Set<String> names) {
    }

    private void parseThreadFactoryElement(final XMLExtendedStreamReader reader, final List<? super ThreadFactoryAdd> updates, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        String groupName = null;
        String threadNamePattern = null;
        Integer priority = null;
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        final int cnt = reader.getAttributeCount();
        for (int i = 0; i < cnt; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    name = reader.getAttributeValue(i);
                    break;
                }
                case GROUP_NAME: {
                    groupName = reader.getAttributeValue(i);
                    break;
                }
                case THREAD_NAME_PATTERN: {
                    threadNamePattern = reader.getAttributeValue(i);
                    break;
                }
                case PRIORITY: {
                    final int val = reader.getIntAttributeValue(i);
                    if (val < Thread.MIN_PRIORITY || val > Thread.MAX_PRIORITY) {
                        throw invalidAttributeValue(reader, i);
                    }
                    priority = Integer.valueOf(val);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }
        final ThreadFactoryAdd add = new ThreadFactoryAdd(name);
        if (groupName != null) add.setGroupName(groupName);
        if (threadNamePattern != null) add.setThreadNamePattern(threadNamePattern);
        if (priority != null) add.setPriority(priority);

        final Map<String, String> map = add.getProperties();

        // Elements
        if (reader.nextTag() != END_ELEMENT) {

            reader.require(START_ELEMENT, Namespace.CURRENT.getUriString(), Element.PROPERTIES.getLocalName());

            while (reader.nextTag() != END_ELEMENT) {
                reader.require(START_ELEMENT, Namespace.CURRENT.getUriString(), Element.PROPERTY.getLocalName());
                readProperty(reader).addTo(map);
            }
        }

        updates.add(add);
    }

    private void parseScheduledExecutorElement(final XMLExtendedStreamReader reader, final List<? super ScheduledThreadPoolAdd> updates, final Set<String> names) throws XMLStreamException {
        // Attributes
        requireSingleAttribute(reader, Attribute.NAME.getLocalName());
        final String name = reader.getAttributeValue(0);
        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }

        // Elements
        ScaledCount maxSize = null;
        TimeSpec keepaliveTime = null;
        String threadFactoryRef = null;

        final Map<String, String> map = new HashMap<String, String>();
        final EnumSet<Element> required = EnumSet.of(Element.MAX_THREADS);
        final EnumSet<Element> encountered = EnumSet.noneOf(Element.class);
        while (reader.nextTag() != END_ELEMENT) {
            if (! reader.getNamespaceURI().equals(Namespace.CURRENT.getUriString())) {
                throw unexpectedElement(reader);
            }
            final Element element = Element.forName(reader.getLocalName());
            required.remove(element);
            if (! encountered.add(element)) {
                throw unexpectedElement(reader);
            }
            switch (element) {
                case MAX_THREADS: {
                    maxSize = readScaledCountElement(reader);
                    break;
                }
                case KEEPALIVE_TIME: {
                    keepaliveTime = readTimeSpecElement(reader);
                    break;
                }

            }
        }
        final ScheduledThreadPoolAdd add = new ScheduledThreadPoolAdd(name, maxSize);
        if (keepaliveTime != null) add.setKeepaliveTime(keepaliveTime);
        if (threadFactoryRef != null) add.setThreadFactory(threadFactoryRef);
        add.getProperties().putAll(map);
        updates.add(add);
    }

    private void parseBoundedQueueExecutorElement(final XMLExtendedStreamReader reader, final List<? super AbstractThreadsSubsystemUpdate<?>> updates, final Set<String> names) throws XMLStreamException {
        // Attributes
        String name = null;
        boolean allowCoreTimeout = false;
        boolean blocking = false;

        final int count = reader.getAttributeCount();
        final EnumSet<Attribute> required = EnumSet.of(Attribute.NAME);
        for (int i = 0; i < count; i ++) {
            if (reader.getAttributeNamespace(i) != null) {
                throw unexpectedAttribute(reader, i);
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case NAME: {
                    name = reader.getAttributeValue(i);
                    break;
                }
                case ALLOW_CORE_TIMEOUT: {
                    allowCoreTimeout = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                case BLOCKING: {
                    blocking = Boolean.parseBoolean(reader.getAttributeValue(i));
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (! required.isEmpty()) {
            throw missingRequired(reader, required);
        }

        if (! names.add(name)) {
            throw duplicateNamedElement(reader, name);
        }


    }

    protected static TimeSpec readTimeSpecElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        TimeUnit unit = null;
        long qty = -1L;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                case TIME: {
                    qty = reader.getLongAttributeValue(i);
                    break;
                }
                case UNIT: {
                    // BES 2010/09/28 - I replaced this because it fails with
                    // case sensitivity problems
                    //unit = reader.getAttributeValue(i, TimeUnit.class);
                    String val = reader.getAttributeValue(i);
                    unit = Enum.valueOf(TimeUnit.class, val.toUpperCase());
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (qty == -1L) throw missingRequired(reader, Collections.singleton(Attribute.TIME));
        if (unit == null) throw missingRequired(reader, Collections.singleton(Attribute.UNIT));
        requireNoContent(reader);
        return new TimeSpec(unit, qty);
    }

    protected static ScaledCount readScaledCountElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        BigDecimal perCpu = null;
        BigDecimal count = null;
        final int acnt = reader.getAttributeCount();
        for (int i = 0; i < acnt; i ++) {
            switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                case PER_CPU: {
                    perCpu = new BigDecimal(reader.getAttributeValue(i), MathContext.DECIMAL64);
                    break;
                }
                case COUNT: {
                    count = new BigDecimal(reader.getAttributeValue(i), MathContext.DECIMAL64);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (perCpu == null) throw missingRequired(reader, Collections.singleton(Attribute.PER_CPU));
        if (count == null) throw missingRequired(reader, Collections.singleton(Attribute.COUNT));
        requireNoContent(reader);
        return new ScaledCount(count, perCpu);
    }

    static final Map<String, String> UNIT_NICK_NAMES = stringMap(
        entry("S", "SECONDS"),
        entry("SEC", "SECONDS"),
        entry("SECOND", "SECONDS"),
        entry("SECONDS", "SECONDS"),
        entry("M", "MINUTES"),
        entry("MIN", "MINUTES"),
        entry("MINUTE", "MINUTES"),
        entry("MINUTES", "MINUTES"),
        entry("MS", "MILLISECONDS"),
        entry("MILLIS", "MILLISECONDS"),
        entry("MILLISECOND", "MILLISECONDS"),
        entry("MILLISECONDS", "MILLISECONDS"),
        entry("NS", "NANOSECONDS"),
        entry("NANOS", "NANOSECONDS"),
        entry("NANOSECOND", "NANOSECONDS"),
        entry("NANOSECONDS", "NANOSECONDS"),
        entry("H", "HOURS"),
        entry("HOUR", "HOURS"),
        entry("HOURS", "HOURS"),
        entry("D", "DAYS"),
        entry("DAY", "DAYS"),
        entry("DAYS", "DAYS"),
        entry("MON", "MONTHS"),
        entry("MONTH", "MONTHS"),
        entry("MONTHS", "MONTHS"),
        entry("W", "WEEKS"),
        entry("WEEK", "WEEKS"),
        entry("WEEKS", "WEEKS")
    );

    private static StringEntry entry(final String key, final String value) {
        return new StringEntry(key, value);
    }

    private static Map<String, String> stringMap(StringEntry... entries) {
        final HashMap<String, String> hashMap = new HashMap<String, String>(entries.length);
        for (Map.Entry<String, String> e : entries) {
            hashMap.put(e.getKey(), e.getValue());
        }
        return Collections.unmodifiableMap(hashMap);
    }

    private static final class StringEntry implements Map.Entry<String, String> {
        private final String key;
        private final String value;

        private StringEntry(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String setValue(final String value) {
            throw new UnsupportedOperationException();
        }
    }
}
