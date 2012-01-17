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

package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.threads.CommonAttributes.BLOCKING_BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.BLOCKING_QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.NAME;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Parser for the threads subsystem or for other subsystems that use pieces of the basic threads subsystem
 * xsd and resource structure.
 */
public final class ThreadsParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

    private static final Logger log = Logger.getLogger("org.jboss.as.threads");

    static final ThreadsParser INSTANCE = new ThreadsParser();

    private static final String SUBSYSTEM_NAME = "threads";

    public static ThreadsParser getInstance() {
        return INSTANCE;
    }

    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

        if (Element.forName(reader.getLocalName()) != Element.SUBSYSTEM) {
            throw unexpectedElement(reader);
        }

        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, SUBSYSTEM_NAME);
        address.protect();

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(address);
        list.add(subsystem);

        String readerNS = reader.getNamespaceURI();
        Namespace threadsNamespace = Namespace.forUri(readerNS);
        switch (threadsNamespace) {
            case THREADS_1_0:
                readElement1_0(reader, list, address);
                break;
            default:
                readElement1_1(reader, list, address, readerNS, threadsNamespace);
                break;
        }
    }

    private void readElement1_0(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode subsystemAddress) throws XMLStreamException {
        Namespace threadsNamespace = Namespace.THREADS_1_0;
        String readerNS = threadsNamespace.getUriString();

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, readerNS);
            switch (element) {
                case BOUNDED_QUEUE_THREAD_POOL: {
                    parseUnknownBoundedQueueThreadPool1_0(reader, readerNS, subsystemAddress, list);
                    break;
                }
                case THREAD_FACTORY: {
                    parseThreadFactory(reader, readerNS, threadsNamespace, subsystemAddress, list, THREAD_FACTORY, null);
                    break;
                }
                case QUEUELESS_THREAD_POOL: {
                    parseUnknownQueuelessThreadPool1_0(reader, readerNS, subsystemAddress, list);
                    break;
                }
                case SCHEDULED_THREAD_POOL: {
                    parseScheduledThreadPool(reader, readerNS, threadsNamespace, subsystemAddress, list, SCHEDULED_THREAD_POOL, null);
                    break;
                }
                case UNBOUNDED_QUEUE_THREAD_POOL: {
                    parseUnboundedQueueThreadPool(reader, readerNS, threadsNamespace, subsystemAddress, list, UNBOUNDED_QUEUE_THREAD_POOL, null);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    private void readElement1_1(final XMLExtendedStreamReader reader, final List<ModelNode> list, final ModelNode subsystemAddress,
                                final String readerNS, final Namespace threadsNamespace) throws XMLStreamException {

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, readerNS);
            switch (element) {
                case BLOCKING_BOUNDED_QUEUE_THREAD_POOL: {
                    parseBoundedQueueThreadPool1_1(reader, readerNS, threadsNamespace, subsystemAddress, list, BLOCKING_BOUNDED_QUEUE_THREAD_POOL, null, true);
                    break;
                }
                case BLOCKING_QUEUELESS_THREAD_POOL: {
                    parseQueuelessThreadPool1_1(reader, readerNS, threadsNamespace, subsystemAddress, list, BLOCKING_QUEUELESS_THREAD_POOL, null, true);
                    break;
                }
                case BOUNDED_QUEUE_THREAD_POOL: {
                    parseBoundedQueueThreadPool1_1(reader, readerNS, threadsNamespace, subsystemAddress, list, BOUNDED_QUEUE_THREAD_POOL, null, false);
                    break;
                }
                case THREAD_FACTORY: {
                    parseThreadFactory(reader, readerNS, threadsNamespace, subsystemAddress, list, THREAD_FACTORY, null);
                    break;
                }
                case QUEUELESS_THREAD_POOL: {
                    parseQueuelessThreadPool1_1(reader, readerNS, threadsNamespace, subsystemAddress, list, QUEUELESS_THREAD_POOL, null, false);
                    break;
                }
                case SCHEDULED_THREAD_POOL: {
                    parseScheduledThreadPool(reader, readerNS, threadsNamespace, subsystemAddress, list, SCHEDULED_THREAD_POOL, null);
                    break;
                }
                case UNBOUNDED_QUEUE_THREAD_POOL: {
                    parseUnboundedQueueThreadPool(reader, readerNS, threadsNamespace, subsystemAddress, list, UNBOUNDED_QUEUE_THREAD_POOL, null);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

    }

    public String parseThreadFactory(final XMLExtendedStreamReader reader, final String expectedNs, Namespace threadsNamespace,
                                     final ModelNode parentAddress, final List<ModelNode> list, final String childType,
                                     final String providedName) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);

        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case GROUP_NAME: {
                    PoolAttributeDefinitions.GROUP_NAME.parseAndSetParameter(value, op, reader);
                    break;
                }
                case THREAD_NAME_PATTERN: {
                    PoolAttributeDefinitions.THREAD_NAME_PATTERN.parseAndSetParameter(value, op, reader);
                    break;
                }
                case PRIORITY: {
                    PoolAttributeDefinitions.PRIORITY.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            switch (element) {
                case PROPERTIES: {
                    parseProperties(reader, threadsNamespace);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        return name;
    }

    public String parseBlockingBoundedQueueThreadPool(final XMLExtendedStreamReader reader, final String expectedNs,
                                                 final Namespace threadsNamespace, final ModelNode parentAddress,
                                                 final List<ModelNode> list, final String childType, final String providedName) throws XMLStreamException {
        switch (threadsNamespace) {
            case THREADS_1_0:
                return parseBoundedQueueThreadPool1_0(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, true);
            default:
                return parseBoundedQueueThreadPool1_1(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, true);
        }
    }

    public String parseBoundedQueueThreadPool(final XMLExtendedStreamReader reader, final String expectedNs,
                                                 final Namespace threadsNamespace, final ModelNode parentAddress,
                                                 final List<ModelNode> list, final String childType, final String providedName) throws XMLStreamException {
        switch (threadsNamespace) {
            case THREADS_1_0:
                return parseBoundedQueueThreadPool1_0(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, false);
            default:
                return parseBoundedQueueThreadPool1_1(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, false);
        }
    }

    private void parseUnknownBoundedQueueThreadPool1_0(final XMLExtendedStreamReader reader, final String expectedNs,
                                                       final ModelNode parentAddress,
                                                       final List<ModelNode> list) throws XMLStreamException {
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case BLOCKING: {
                    parseBoundedQueueThreadPool1_0(reader, expectedNs, Namespace.THREADS_1_0, parentAddress, list, BLOCKING_BOUNDED_QUEUE_THREAD_POOL, null, true);
                    return;
                }
                default:
                    break;
            }
        }
        parseBoundedQueueThreadPool1_0(reader, expectedNs, Namespace.THREADS_1_0, parentAddress, list, BOUNDED_QUEUE_THREAD_POOL, null, false);
    }

    private String parseBoundedQueueThreadPool1_0(final XMLExtendedStreamReader reader, final String expectedNs,
                                                 final Namespace threadsNamespace, final ModelNode parentAddress,
                                                 final List<ModelNode> list, final String childType, final String providedName,
                                                 final boolean blocking) throws XMLStreamException {

        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case BLOCKING: {
                    // we ignore this
                    break;
                }
                case ALLOW_CORE_TIMEOUT: {
                    PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        Set<Element> required = EnumSet.of(Element.MAX_THREADS, Element.QUEUE_LENGTH);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            required.remove(element);
            switch (element) {
                case CORE_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.CORE_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    break;
                }
                case HANDOFF_EXECUTOR: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    if (!blocking) {
                        PoolAttributeDefinitions.HANDOFF_EXECUTOR.parseAndSetParameter(ref, op, reader);
                    } // else we ignore TODO log a WARN
                    break;
                }
                case MAX_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.MAX_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.THREAD_FACTORY.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case PROPERTIES: {
                    parseProperties(reader, threadsNamespace);
                    break;
                }
                case QUEUE_LENGTH: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.QUEUE_LENGTH.parseAndSetParameter(scaledCount, op, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
        return name;
    }

    private String parseBoundedQueueThreadPool1_1(final XMLExtendedStreamReader reader, final String expectedNs,
                                                 final Namespace threadsNamespace, final ModelNode parentAddress,
                                                 final List<ModelNode> list, final String childType,
                                                 final String providedName, final boolean blocking) throws XMLStreamException {

        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case ALLOW_CORE_TIMEOUT: {
                    PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.parseAndSetParameter(value, op, reader);
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        Set<Element> required = EnumSet.of(Element.MAX_THREADS, Element.QUEUE_LENGTH);
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            required.remove(element);
            switch (element) {
                case CORE_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.CORE_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    break;
                }
                case HANDOFF_EXECUTOR: {
                    if (blocking) {
                        throw unexpectedElement(reader);
                    }
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.HANDOFF_EXECUTOR.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case MAX_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.MAX_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.THREAD_FACTORY.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case QUEUE_LENGTH: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.QUEUE_LENGTH.parseAndSetParameter(scaledCount, op, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!required.isEmpty()) {
            throw missingRequiredElement(reader, required);
        }
        return name;
    }

    public String parseUnboundedQueueThreadPool(final XMLExtendedStreamReader reader, String expectedNs, Namespace threadsNamespace, final ModelNode parentAddress,
                                                final List<ModelNode> list, final String childType, final String providedName) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            switch (element) {
                case MAX_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.MAX_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.THREAD_FACTORY.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case PROPERTIES: {
                    parseProperties(reader, threadsNamespace);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    public String parseScheduledThreadPool(final XMLExtendedStreamReader reader, String expectedNs, Namespace threadsNamespace, final ModelNode parentAddress,
                                           final List<ModelNode> list, final String childType, final String providedName) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            switch (element) {
                case MAX_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.MAX_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.THREAD_FACTORY.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case PROPERTIES: {
                    parseProperties(reader, threadsNamespace);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    public String parseBlockingQueuelessThreadPool(final XMLExtendedStreamReader reader, String expectedNs, Namespace threadsNamespace, final ModelNode parentAddress,
                                           final List<ModelNode> list, final String childType, final String providedName) throws XMLStreamException {
        switch (threadsNamespace) {
            case THREADS_1_0:
                return parseQueuelessThreadPool1_0(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, true);
            default:
                return parseQueuelessThreadPool1_1(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, true);
        }
    }

    public String parseQueuelessThreadPool(final XMLExtendedStreamReader reader, String expectedNs, Namespace threadsNamespace, final ModelNode parentAddress,
                                           final List<ModelNode> list, final String childType, final String providedName) throws XMLStreamException {
        switch (threadsNamespace) {
            case THREADS_1_0:
                return parseQueuelessThreadPool1_0(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, false);
            default:
                return parseQueuelessThreadPool1_1(reader, expectedNs, threadsNamespace, parentAddress, list, childType, providedName, false);
        }
    }

    private void parseUnknownQueuelessThreadPool1_0(XMLExtendedStreamReader reader, String readerNS, ModelNode subsystemAddress, List<ModelNode> list) throws XMLStreamException {

        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case BLOCKING: {
                    parseQueuelessThreadPool1_0(reader, readerNS, Namespace.THREADS_1_0, subsystemAddress, list, BLOCKING_QUEUELESS_THREAD_POOL, null, true);
                    return;
                }
                default:
                    break;
            }
        }
        parseQueuelessThreadPool1_0(reader, readerNS, Namespace.THREADS_1_0, subsystemAddress, list, QUEUELESS_THREAD_POOL, null, false);
    }

    private String parseQueuelessThreadPool1_0(final XMLExtendedStreamReader reader, String expectedNs, Namespace threadsNamespace, final ModelNode parentAddress,
                                                final List<ModelNode> list, final String childType, final String providedName,
                                                final boolean blocking) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case BLOCKING: {
                    // ignore
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            switch (element) {
                case HANDOFF_EXECUTOR: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    if (!blocking) {
                        PoolAttributeDefinitions.HANDOFF_EXECUTOR.parseAndSetParameter(ref, op, reader);
                    }
                    break;
                }
                case MAX_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.MAX_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.THREAD_FACTORY.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case PROPERTIES: {
                    parseProperties(reader, threadsNamespace);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    private String parseQueuelessThreadPool1_1(final XMLExtendedStreamReader reader, String expectedNs, Namespace threadsNamespace, final ModelNode parentAddress,
                                               final List<ModelNode> list, final String childType, final String providedName, boolean blocking) throws XMLStreamException {
        final ModelNode op = new ModelNode();
        list.add(op);
        op.get(OP).set(ADD);

        String name = null;
        int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (providedName != null) {
            name = providedName;
        } else if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }

        final ModelNode address = parentAddress.clone();
        address.add(childType, name);
        address.protect();
        op.get(OP_ADDR).set(address);

        boolean foundMaxThreads = false;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            Element element = nextElement(reader, expectedNs);
            switch (element) {
                case HANDOFF_EXECUTOR: {
                    if (blocking) {
                        throw unexpectedElement(reader);
                    }
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.HANDOFF_EXECUTOR.parseAndSetParameter(ref, op, reader);
                    break;
                }
                case MAX_THREADS: {
                    String scaledCount = parseCount(reader, threadsNamespace);
                    PoolAttributeDefinitions.MAX_THREADS.parseAndSetParameter(scaledCount, op, reader);
                    foundMaxThreads = true;
                    break;
                }
                case KEEPALIVE_TIME: {
                    op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                    break;
                }
                case THREAD_FACTORY: {
                    String ref = readStringAttributeElement(reader, Attribute.NAME.getLocalName());
                    PoolAttributeDefinitions.THREAD_FACTORY.parseAndSetParameter(ref, op, reader);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (!foundMaxThreads) {
            throw missingRequiredElement(reader, Collections.singleton(Element.MAX_THREADS));
        }
        return name;
    }

    private String parseCount(final XMLExtendedStreamReader reader, Namespace expectedNS) throws XMLStreamException {
        switch (expectedNS) {
            case THREADS_1_0:
            case THREADS_1_1: // still allow the legacy config even though it violates the schema
                return parseScaledCount(reader);
            default:
                return readStringAttributeElement(reader, Attribute.COUNT.getLocalName());
        }
    }

    private String parseScaledCount(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int attrCount = reader.getAttributeCount();
        BigDecimal count = null;
        BigDecimal perCpu = new BigDecimal(0);
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case COUNT: {
                    try {
                        count = new BigDecimal(value);
                        if (count.compareTo(BigDecimal.ZERO) < 0) {
                            throw new XMLStreamException(attribute.getLocalName() + " must be greater than or equal to zero", reader.getLocation());
                        }
                    } catch (NumberFormatException e) {
                        throw invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case PER_CPU: {
                    try {
                        perCpu = new BigDecimal(value);
                        if (perCpu.compareTo(BigDecimal.ZERO) < 0) {
                            throw new XMLStreamException(attribute.getLocalName() + " must be greater than or equal to zero", reader.getLocation());
                        }
                    } catch (NumberFormatException e) {
                        throw invalidAttributeValue(reader, i);
                    }
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (count == null) {
            throw missingRequired(reader, EnumSet.of(Attribute.COUNT));
        }

        ParseUtils.requireNoContent(reader);

        int fullCount = getScaledCount(count, perCpu);
        if (!perCpu.equals(new BigDecimal(0))) {
            log.warn(String.format("The '%s' attribute is no longer supported. The value [%f] of the '%s' attribute " +
                    "is being combined with the value [%f] of the '%s' attribute and the current processor count [%d] " +
                    "to derive a new value of [%d] for '%s'.", Attribute.PER_CPU, count, Attribute.COUNT,
                    perCpu, Attribute.PER_CPU, Runtime.getRuntime().availableProcessors(), fullCount, Attribute.COUNT));
        }

        return String.valueOf(fullCount);
    }

    private static int getScaledCount(BigDecimal count, BigDecimal perCpu) {
        return count.add(perCpu.multiply(BigDecimal.valueOf((long)Runtime.getRuntime().availableProcessors()), MathContext.DECIMAL64), MathContext.DECIMAL64).round(MathContext.DECIMAL64).intValueExact();
    }

    private ModelNode parseTimeSpec(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int attrCount = reader.getAttributeCount();
        TimeUnit unit = null;
        Long duration = null;
        Set<Attribute> required = EnumSet.of(Attribute.TIME, Attribute.UNIT);
        for (int i = 0; i < attrCount; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
            switch (attribute) {
                case TIME: {
                    duration = reader.getLongAttributeValue(i);
                    break;
                }
                case UNIT: {
                    unit = Enum.valueOf(TimeUnit.class, value.toUpperCase());
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (!required.isEmpty()) {
            throw missingRequired(reader, required);
        }
        assert unit != null;
        assert duration != null;

        ParseUtils.requireNoContent(reader);

        ModelNode node = new ModelNode();
        node.get(TIME).set(duration);
        node.get(UNIT).set(unit.toString());
        return node;
    }

    private void parseProperties(final XMLExtendedStreamReader reader, final Namespace threadsNamespace) throws XMLStreamException {
        if (threadsNamespace != Namespace.THREADS_1_0) {
                throw unexpectedElement(reader);
        }
        // else consume and discard the never implemented 1.0 "properties" element.
        // This code validates, which is a debatable given the data is going to be discarded

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Element.forName(reader.getLocalName())) {
                case PROPERTY: {
                    final int attrCount = reader.getAttributeCount();
                    String propName = null;
                    String propValue = null;
                    for (int i = 0; i < attrCount; i++) {
                        requireNoNamespaceAttribute(reader, i);
                        final String value = reader.getAttributeValue(i);
                        final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME: {
                                propName = value;
                                break;
                            }
                            case VALUE: {
                                propValue = value;
                            }
                                break;
                            default:
                                throw unexpectedAttribute(reader, i);
                        }
                    }
                    if (propName == null || propValue == null) {
                        Set<Attribute> missing = new HashSet<Attribute>();
                        if (propName == null) {
                            missing.add(Attribute.NAME);
                        }
                        if (propValue == null) {
                            missing.add(Attribute.VALUE);
                        }
                        throw missingRequired(reader, missing);
                    }

                    ParseUtils.requireNoContent(reader);
                }
            }
        }
    }

    /**
     * A variation of nextElement that verifies the nextElement is not in a different namespace.
     *
     * @param reader the XmlExtendedReader to read from.
     * @param expectedNamespace the namespace expected.
     * @return the element or null if the end is reached
     * @throws XMLStreamException if the namespace is wrong or there is a problem accessing the reader
     */
    private static Element nextElement(XMLExtendedStreamReader reader, String expectedNamespace) throws XMLStreamException {
        Element element = Element.forName(reader.getLocalName());

        if (element == null) {
            return element;
        } else if (expectedNamespace.equals(reader.getNamespaceURI())) {
            return element;
        }

        throw unexpectedElement(reader);
    }

    /** {@inheritDoc} */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context)
            throws XMLStreamException {

        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

        ModelNode node = context.getModelNode();

        writeThreadsElement(writer, node);

        writer.writeEndElement();
    }

    public void writeThreadsElement(final XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {
        if (node.hasDefined(THREAD_FACTORY)) {
            for (String name : node.get(THREAD_FACTORY).keys()) {
                final ModelNode child = node.get(THREAD_FACTORY, name);
                if (child.isDefined()) {
                    writeThreadFactory(writer, child);
                }
            }
        }
        if (node.hasDefined(UNBOUNDED_QUEUE_THREAD_POOL)) {
            for (String name : node.get(UNBOUNDED_QUEUE_THREAD_POOL).keys()) {
                final ModelNode child = node.get(UNBOUNDED_QUEUE_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeUnboundedQueueThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(BOUNDED_QUEUE_THREAD_POOL)) {
            for (String name : node.get(BOUNDED_QUEUE_THREAD_POOL).keys()) {
                final ModelNode child = node.get(BOUNDED_QUEUE_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeBoundedQueueThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(BLOCKING_BOUNDED_QUEUE_THREAD_POOL)) {
            for (String name : node.get(BLOCKING_BOUNDED_QUEUE_THREAD_POOL).keys()) {
                final ModelNode child = node.get(BLOCKING_BOUNDED_QUEUE_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeBlockingBoundedQueueThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(QUEUELESS_THREAD_POOL)) {
            for (String name : node.get(QUEUELESS_THREAD_POOL).keys()) {
                final ModelNode child = node.get(QUEUELESS_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeQueuelessThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(BLOCKING_QUEUELESS_THREAD_POOL)) {
            for (String name : node.get(BLOCKING_QUEUELESS_THREAD_POOL).keys()) {
                final ModelNode child = node.get(BLOCKING_QUEUELESS_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeBlockingQueuelessThreadPool(writer, child);
                }
            }
        }
        if (node.hasDefined(SCHEDULED_THREAD_POOL)) {
            for (String name : node.get(SCHEDULED_THREAD_POOL).keys()) {
                final ModelNode child = node.get(SCHEDULED_THREAD_POOL, name);
                if (child.isDefined()) {
                    writeScheduledQueueThreadPool(writer, child);
                }
            }
        }
    }

    public void writeThreadFactory(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeThreadFactory(writer, node, Element.THREAD_FACTORY.getLocalName(), true);
    }

    public void writeThreadFactory(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName) throws XMLStreamException {
        writer.writeStartElement(elementName);

        if (includeName && node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }

        PoolAttributeDefinitions.GROUP_NAME.marshallAsAttribute(node, writer);
        PoolAttributeDefinitions.THREAD_NAME_PATTERN.marshallAsAttribute(node, writer);
        PoolAttributeDefinitions.PRIORITY.marshallAsAttribute(node, writer);

        writer.writeEndElement();
    }

    public void writeBlockingBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeBoundedQueueThreadPool(writer, node, Element.BLOCKING_BOUNDED_QUEUE_THREAD_POOL.getLocalName(), true, true);
    }

    public void writeBlockingBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName)
            throws XMLStreamException {
        writeBoundedQueueThreadPool(writer, node, elementName, includeName, true);
    }

    public void writeBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeBoundedQueueThreadPool(writer, node, Element.BOUNDED_QUEUE_THREAD_POOL.getLocalName(), true, false);
    }

    public void writeBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName)
            throws XMLStreamException {
        writeBoundedQueueThreadPool(writer, node, elementName, includeName, false);
    }

    public void writeBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName,
                                            final boolean includeName, final boolean blocking)
            throws XMLStreamException {
        writer.writeStartElement(elementName);

        if (includeName && node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }

        PoolAttributeDefinitions.ALLOW_CORE_TIMEOUT.marshallAsAttribute(node, writer);

        writeCountElement(PoolAttributeDefinitions.CORE_THREADS, node, writer);
        writeCountElement(PoolAttributeDefinitions.QUEUE_LENGTH, node, writer);
        writeCountElement(PoolAttributeDefinitions.MAX_THREADS, node, writer);

        writeTime(writer, node, Element.KEEPALIVE_TIME);
        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
        if (!blocking) {
            writeRef(writer, node, Element.HANDOFF_EXECUTOR, HANDOFF_EXECUTOR);
        }

        writer.writeEndElement();
    }

    private void writeCountElement(AttributeDefinition attributeDefinition, ModelNode model, XMLExtendedStreamWriter writer) throws XMLStreamException {
        if (attributeDefinition.isMarshallable(model)) {
            writer.writeEmptyElement(attributeDefinition.getXmlName());
            writer.writeAttribute(Attribute.COUNT.getLocalName(), model.get(attributeDefinition.getName()).asString());
        }
    }

    public void writeBlockingQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeQueuelessThreadPool(writer, node, Element.BLOCKING_QUEUELESS_THREAD_POOL.getLocalName(), true, true);
    }

    public void writeBlockingQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName) throws XMLStreamException {
        writeQueuelessThreadPool(writer, node, elementName, includeName, true);
    }

    public void writeQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeQueuelessThreadPool(writer, node, Element.QUEUELESS_THREAD_POOL.getLocalName(), true, false);
    }

    public void writeQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName) throws XMLStreamException {
        writeQueuelessThreadPool(writer, node, elementName, includeName, false);
    }

    private void writeQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName,
                                         final boolean includeName, final boolean blocking) throws XMLStreamException {
        writer.writeStartElement(elementName);

        if (includeName && node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }

        writeCountElement(PoolAttributeDefinitions.MAX_THREADS, node, writer);

        writeTime(writer, node, Element.KEEPALIVE_TIME);
        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
        if (!blocking) {
            writeRef(writer, node, Element.HANDOFF_EXECUTOR, HANDOFF_EXECUTOR);
        }

        writer.writeEndElement();
    }

    public void writeScheduledQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeScheduledQueueThreadPool(writer, node, Element.SCHEDULED_THREAD_POOL.getLocalName(), true);
    }

    public void writeScheduledQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName)
            throws XMLStreamException {
        writer.writeStartElement(elementName);

        if (includeName && node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }

        writeCountElement(PoolAttributeDefinitions.MAX_THREADS, node, writer);
        writeTime(writer, node, Element.KEEPALIVE_TIME);
        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);

        writer.writeEndElement();
    }

    public void writeUnboundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
        writeUnboundedQueueThreadPool(writer, node, Element.UNBOUNDED_QUEUE_THREAD_POOL.getLocalName(), true);
    }

    public void writeUnboundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node, final String elementName, final boolean includeName)
            throws XMLStreamException {
        writer.writeStartElement(elementName);

        if (includeName && node.hasDefined(NAME)) {
            writeAttribute(writer, Attribute.NAME, node.get(NAME));
        }

        writeCountElement(PoolAttributeDefinitions.MAX_THREADS, node, writer);
        writeTime(writer, node, Element.KEEPALIVE_TIME);
        writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);

        writer.writeEndElement();
    }

    private void writeRef(final XMLExtendedStreamWriter writer, final ModelNode node, Element element, String name)
            throws XMLStreamException {
        if (node.hasDefined(name)) {
            writer.writeStartElement(element.getLocalName());
            writeAttribute(writer, Attribute.NAME, node.get(name));
            writer.writeEndElement();
        }
    }

    private void writeTime(final XMLExtendedStreamWriter writer, final ModelNode node, Element element)
            throws XMLStreamException {
        if (node.hasDefined(element.getLocalName())) {
            writer.writeStartElement(element.getLocalName());
            ModelNode keepalive = node.get(element.getLocalName());
            if (keepalive.hasDefined(TIME)) {
                writeAttribute(writer, Attribute.TIME, keepalive.get(TIME));
            }
            if (keepalive.hasDefined(UNIT)) {
                writeAttributeLowerCaseValue(writer, Attribute.UNIT, keepalive.get(UNIT));
            }
            writer.writeEndElement();
        }
    }

    private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
            throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString());
    }

    private void writeAttributeLowerCaseValue(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value)
            throws XMLStreamException {
        writer.writeAttribute(attr.getLocalName(), value.asString().toLowerCase());
    }
}
