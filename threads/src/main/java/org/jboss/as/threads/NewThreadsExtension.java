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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.threads.Constants.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.Constants.BLOCKING;
import static org.jboss.as.threads.Constants.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.Constants.CORE_THREADS_COUNT;
import static org.jboss.as.threads.Constants.CORE_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.GROUP_NAME;
import static org.jboss.as.threads.Constants.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_DURATION;
import static org.jboss.as.threads.Constants.KEEPALIVE_TIME_UNIT;
import static org.jboss.as.threads.Constants.MAX_THREADS_COUNT;
import static org.jboss.as.threads.Constants.MAX_THREADS_PER_CPU;
import static org.jboss.as.threads.Constants.NAME;
import static org.jboss.as.threads.Constants.PRIORITY;
import static org.jboss.as.threads.Constants.PROPERTIES;
import static org.jboss.as.threads.Constants.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.Constants.QUEUE_LENGTH_COUNT;
import static org.jboss.as.threads.Constants.QUEUE_LENGTH_PER_CPU;
import static org.jboss.as.threads.Constants.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.Constants.THREADS;
import static org.jboss.as.threads.Constants.THREAD_FACTORY;
import static org.jboss.as.threads.Constants.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.Constants.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.COUNT;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.NAME;
import static org.jboss.as.threads.CommonAttributes.PER_CPU;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREADS;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.UNIT;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.ADD_BOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.ADD_QUEUELESS_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.ADD_SCHEDULED_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.ADD_THREAD_FACTORY_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.ADD_UNBOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.BOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.QUEUELESS_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.REMOVE_BOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.REMOVE_QUEUELESS_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.REMOVE_SCHEDULED_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.REMOVE_THREAD_FACTORY_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.REMOVE_UNBOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.SCHEDULED_THREAD_POOL_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.SUBSYSTEM_PROVIDER;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.THREAD_FACTORY_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.UNBOUNDED_QUEUE_THREAD_POOL_DESC;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewThreadsExtension implements NewExtension {

    private static String SUBSYSTEM_NAME = "threads";

    @Override
    public void initialize(final NewExtensionContext context) {


        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(THREADS);
        registration.registerXMLElementWriter(NewThreadsSubsystemParser.INSTANCE);
        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM_PROVIDER);
        subsystem.registerOperationHandler(ADD, NewThreadsSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);
        subsystem.registerOperationHandler(DESCRIBE, ThreadsSubsystemDescribeHandler.INSTANCE, ThreadsSubsystemDescribeHandler.INSTANCE, false);

        final ModelNodeRegistration threadFactories = subsystem.registerSubModel(PathElement.pathElement(THREAD_FACTORY), THREAD_FACTORY_DESC);
        threadFactories.registerOperationHandler(ADD, NewThreadFactoryAdd.INSTANCE, ADD_THREAD_FACTORY_DESC, false);
        threadFactories.registerOperationHandler(REMOVE, NewThreadFactoryRemove.INSTANCE, REMOVE_THREAD_FACTORY_DESC, false);

        final ModelNodeRegistration boundedQueueThreadPools = subsystem.registerSubModel(PathElement.pathElement(BOUNDED_QUEUE_THREAD_POOL), BOUNDED_QUEUE_THREAD_POOL_DESC);
        boundedQueueThreadPools.registerOperationHandler(ADD, NewBoundedQueueThreadPoolAdd.INSTANCE, ADD_BOUNDED_QUEUE_THREAD_POOL_DESC, false);
        boundedQueueThreadPools.registerOperationHandler(REMOVE, NewBoundedQueueThreadPoolRemove.INSTANCE, REMOVE_BOUNDED_QUEUE_THREAD_POOL_DESC, false);

        final ModelNodeRegistration unboundedQueueThreadPools = subsystem.registerSubModel(PathElement.pathElement(UNBOUNDED_QUEUE_THREAD_POOL), UNBOUNDED_QUEUE_THREAD_POOL_DESC);
        unboundedQueueThreadPools.registerOperationHandler(ADD, NewUnboundedQueueThreadPoolAdd.INSTANCE, ADD_UNBOUNDED_QUEUE_THREAD_POOL_DESC, false);
        unboundedQueueThreadPools.registerOperationHandler(REMOVE, NewUnboundedQueueThreadPoolRemove.INSTANCE, REMOVE_UNBOUNDED_QUEUE_THREAD_POOL_DESC, false);

        final ModelNodeRegistration queuelessThreadPools = subsystem.registerSubModel(PathElement.pathElement(QUEUELESS_THREAD_POOL), QUEUELESS_THREAD_POOL_DESC);
        queuelessThreadPools.registerOperationHandler(ADD, NewQueuelessThreadPoolAdd.INSTANCE, ADD_QUEUELESS_THREAD_POOL_DESC, false);
        queuelessThreadPools.registerOperationHandler(REMOVE, NewQueuelessThreadPoolRemove.INSTANCE, REMOVE_QUEUELESS_THREAD_POOL_DESC, false);

        final ModelNodeRegistration scheduledThreadPools = subsystem.registerSubModel(PathElement.pathElement(SCHEDULED_THREAD_POOL), SCHEDULED_THREAD_POOL_DESC);
        scheduledThreadPools.registerOperationHandler(ADD, NewScheduledThreadPoolAdd.INSTANCE, ADD_SCHEDULED_THREAD_POOL_DESC, false);
        scheduledThreadPools.registerOperationHandler(REMOVE, NewScheduledThreadPoolRemove.INSTANCE, REMOVE_SCHEDULED_THREAD_POOL_DESC, false);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewThreadsSubsystemParser.INSTANCE);
    }


    static final class NewThreadsSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        static final NewThreadsSubsystemParser INSTANCE = new NewThreadsSubsystemParser();

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            final ModelNode address = new ModelNode();
            address.add(SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            // Handle elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                case THREADS_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case BOUNDED_QUEUE_THREAD_POOL: {
                            parseBoundedQueueThreadPool(reader, address, list);
                            break;
                        }
                        case THREAD_FACTORY: {
                            // Add connector updates
                            parseThreadFactory(reader, address, list);
                            break;
                        }
                        case QUEUELESS_THREAD_POOL: {
                            parseQueuelessThreadPool(reader, address, list);
                            break;
                        }
                        case SCHEDULED_THREAD_POOL: {
                            parseScheduledThreadPool(reader, address, list);
                            break;
                        }
                        case UNBOUNDED_QUEUE_THREAD_POOL: {
                            parseUnboundedQueueThreadPool(reader, address, list);
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
            }
        }

        private void parseThreadFactory(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    op.get(NAME).set(value);
                    name = value;
                    break;
                }
                case GROUP_NAME: {
                    op.get(GROUP_NAME).set(value);
                    break;
                }
                case THREAD_NAME_PATTERN: {
                    op.get(THREAD_NAME_PATTERN).set(value);
                    break;
                }
                case PRIORITY: {
                    try {
                        int priority = Integer.valueOf(value);
                        op.get(PRIORITY).set(priority);
                    } catch (NumberFormatException e) {
                        invalidAttributeValue(reader, i);
                    }
                }
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode address = parentAddress.clone();
            address.add(THREAD_FACTORY, name);
            address.protect();
            op.get(ADDRESS).set(address);



            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case PROPERTIES: {
                        ModelNode props = parseProperties(reader);
                        if (props.isDefined()) {
                            op.get(PROPERTIES).set(props);
                        }
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
                break;
            }
        }

        void parseBoundedQueueThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    op.get(NAME).set(value);
                    name = value;
                    break;
                } case BLOCKING : {
                    op.get(BLOCKING).set(Boolean.valueOf(value));
                    break;
                } case ALLOW_CORE_TIMEOUT: {
                    op.get(ALLOW_CORE_TIMEOUT).set(Boolean.valueOf(value));
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            final ModelNode address = parentAddress.clone();
            address.add(BOUNDED_QUEUE_THREAD_POOL, name);
            address.protect();
            op.get(ADDRESS).set(address);

            boolean foundQueueLength = false;
            boolean foundMaxThreads = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case CORE_THREADS: {
                        op.get(CORE_THREADS).set(parseScaledCount(reader));
                        break;
                    }
                    case HANDOFF_EXECUTOR: {
                        op.get(HANDOFF_EXECUTOR).set(parseRef(reader));
                        break;
                    }
                    case MAX_THREADS: {
                        op.get(MAX_THREADS).set(parseScaledCount(reader));
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        ModelNode props = parseProperties(reader);
                        if (props.isDefined()) {
                            op.get(PROPERTIES).set(props);
                        }
                        break;
                    }
                    case QUEUE_LENGTH: {
                        op.get(QUEUE_LENGTH).set(parseScaledCount(reader));
                        foundQueueLength = true;
                        break;
                    }
                    default: {
                        throw unexpectedElement(reader);
                    }
                }
            }
            if (!foundMaxThreads || !foundQueueLength) {
                Set<Element> missing = new HashSet<Element>();
                if (!foundMaxThreads) {
                    missing.add(Element.MAX_THREADS);
                }
                if (!foundQueueLength) {
                    missing.add(Element.QUEUE_LENGTH);
                }
                throw missingRequiredElement(reader, missing);
            }
        }


        void parseUnboundedQueueThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    op.get(NAME).set(value);
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            //FIXME Make relative and use this scheme to add the addresses
            //address.add("profile", "test).add("subsystem", "threads")
            final ModelNode address = parentAddress.clone();
            address.add(UNBOUNDED_QUEUE_THREAD_POOL, name);
            address.protect();
            op.get(ADDRESS).set(address);

            boolean foundMaxThreads = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case MAX_THREADS: {
                        op.get(MAX_THREADS).set(parseScaledCount(reader));
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        ModelNode props = parseProperties(reader);
                        if (props.isDefined()) {
                            op.get(PROPERTIES).set(props);
                        }
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
        }

        void parseScheduledThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    op.get(NAME).set(value);
                    name = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            //FIXME Make relative and use this scheme to add the addresses
            //address.add("profile", "test).add("subsystem", "threads")
            final ModelNode address = parentAddress.clone();
            address.add(SCHEDULED_THREAD_POOL, name);
            address.protect();
            op.get(ADDRESS).set(address);

            boolean foundMaxThreads = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case MAX_THREADS: {
                        op.get(MAX_THREADS).set(parseScaledCount(reader));
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        ModelNode props = parseProperties(reader);
                        if (props.isDefined()) {
                            op.get(PROPERTIES).set(props);
                        }
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
        }

        void parseQueuelessThreadPool(final XMLExtendedStreamReader reader, final ModelNode parentAddress, final List<ModelNode> list) throws XMLStreamException {
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
                    op.get(NAME).set(value);
                    name = value;
                    break;
                } case BLOCKING : {
                    op.get(BLOCKING).set(Boolean.valueOf(value));
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            //FIXME Make relative and use this scheme to add the addresses
            //address.add("profile", "test).add("subsystem", "threads")
            final ModelNode address = parentAddress.clone();
            address.add(QUEUELESS_THREAD_POOL, name);
            address.protect();
            op.get(ADDRESS).set(address);

            boolean foundMaxThreads = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case HANDOFF_EXECUTOR: {
                        op.get(HANDOFF_EXECUTOR).set(parseRef(reader));
                        break;
                    }
                    case MAX_THREADS: {
                        op.get(MAX_THREADS).set(parseScaledCount(reader));
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        op.get(KEEPALIVE_TIME).set(parseTimeSpec(reader));
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(CommonAttributes.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        ModelNode props = parseProperties(reader);
                        if (props.isDefined()) {
                            op.get(PROPERTIES).set(props);
                        }
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
        }

        private ModelNode parseScaledCount(final XMLExtendedStreamReader reader) throws XMLStreamException {
            final int attrCount = reader.getAttributeCount();
            BigDecimal count = null;
            BigDecimal perCpu = null;
            for (int i = 0; i < attrCount; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case COUNT: {
                    try {
                        count = new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        throw invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case PER_CPU: {
                    try {
                        perCpu = new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        throw invalidAttributeValue(reader, i);
                    }
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (count == null || perCpu == null) {
                Set<Attribute> missing = new HashSet<Attribute>();
                if (count == null) {
                    missing.add(Attribute.COUNT);
                }
                if (perCpu == null) {
                    missing.add(Attribute.PER_CPU);
                }
                throw missingRequired(reader, missing);
            }

            if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                throw unexpectedElement(reader);
            }

            ModelNode node = new ModelNode();
            node.get(COUNT).set(count);
            node.get(PER_CPU).set(perCpu);

            return node;
        }


        private ModelNode parseTimeSpec(final XMLExtendedStreamReader reader) throws XMLStreamException {
            final int attrCount = reader.getAttributeCount();
            TimeUnit unit = null;
            Long duration = null;
            for (int i = 0; i < attrCount; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
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

            if (duration == null || unit == null) {
                Set<Attribute> missing = new HashSet<Attribute>();
                if (duration == null) {
                    missing.add(Attribute.TIME);
                }
                if (unit == null) {
                    missing.add(Attribute.UNIT);
                }
            }

            if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                throw unexpectedElement(reader);
            }

            ModelNode node = new ModelNode();
            node.get(TIME).set(duration);
            node.get(UNIT).set(unit.toString());
            return node;
        }

        private String parseRef(XMLExtendedStreamReader reader) throws XMLStreamException {
            final int attrCount = reader.getAttributeCount();
            String refName = null;
            for (int i = 0; i < attrCount; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case NAME: {
                    refName = value;
                    break;
                }
                default:
                    throw unexpectedAttribute(reader, i);
                }
            }

            if (refName == null) {
                throw missingRequired(reader, Collections.singleton(NAME));
            }
            if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                throw unexpectedElement(reader);
            }

            return refName;
        }

        private ModelNode parseProperties(final XMLExtendedStreamReader reader) throws XMLStreamException {
            ModelNode node = new ModelNode();
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
                        node.add(propName, propValue);

                        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                            throw unexpectedElement(reader);
                        }
                    }
                }
            }
            return node;
        }

        /** {@inheritDoc} */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {

            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);

            ModelNode node = context.getModelNode();

            if (node.hasDefined(THREAD_FACTORY)) {
                for (String name : node.get(THREAD_FACTORY).keys()) {
                    final ModelNode child = node.get(THREAD_FACTORY, name);
                    if (child.isDefined()) {
                        writeThreadFactory(writer, child);
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
            if (node.hasDefined(QUEUELESS_THREAD_POOL)) {
                for (String name : node.get(QUEUELESS_THREAD_POOL).keys()) {
                    final ModelNode child = node.get(QUEUELESS_THREAD_POOL, name);
                    if (child.isDefined()) {
                        writeQueuelessThreadPool(writer, child);
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
            if (node.hasDefined(UNBOUNDED_QUEUE_THREAD_POOL)) {
                for (String name : node.get(UNBOUNDED_QUEUE_THREAD_POOL).keys()) {
                    final ModelNode child = node.get(UNBOUNDED_QUEUE_THREAD_POOL, name);
                    if (child.isDefined()) {
                        writeUnboundedQueueThreadPool(writer, child);
                    }
                }
            }

            writer.writeEndElement();
        }

        private void writeThreadFactory(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.THREAD_FACTORY.getLocalName());
            if (node.hasDefined(NAME)) {
                writeAttribute(writer, Attribute.NAME, node.get(NAME));
            }
            if (node.hasDefined(GROUP_NAME)) {
                writeAttribute(writer, Attribute.GROUP_NAME, node.get(GROUP_NAME));
            }
            if (node.hasDefined(THREAD_NAME_PATTERN)) {
                writeAttribute(writer, Attribute.THREAD_NAME_PATTERN, node.get(THREAD_NAME_PATTERN));
            }
            if (node.hasDefined(PRIORITY)) {
                writeAttribute(writer, Attribute.PRIORITY, node.get(PRIORITY));
            }
            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }

            writer.writeEndElement();
        }

        private void writeBoundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.BOUNDED_QUEUE_THREAD_POOL.getLocalName());

            if (node.hasDefined(NAME)) {
                writeAttribute(writer, Attribute.NAME, node.get(NAME));
            }
            if (node.hasDefined(BLOCKING)) {
                writeAttribute(writer, Attribute.BLOCKING, node.get(BLOCKING));
            }
            if (node.hasDefined(ALLOW_CORE_TIMEOUT)) {
                writeAttribute(writer, Attribute.ALLOW_CORE_TIMEOUT, node.get(ALLOW_CORE_TIMEOUT));
            }
            writeRef(writer, node, Element.HANDOFF_EXECUTOR, HANDOFF_EXECUTOR);
            writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
            writeThreads(writer, node, Element.CORE_THREADS);
            writeThreads(writer, node, Element.QUEUE_LENGTH);
            writeThreads(writer, node, Element.MAX_THREADS);
            writeTime(writer, node, Element.KEEPALIVE_TIME);

            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }

            writer.writeEndElement();
        }

        private void writeQueuelessThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.QUEUELESS_THREAD_POOL.getLocalName());

            if (node.hasDefined(NAME)) {
                writeAttribute(writer, Attribute.BLOCKING, node.get(NAME));
            }
            if (node.hasDefined(BLOCKING)) {
                writeAttribute(writer, Attribute.BLOCKING, node.get(BLOCKING));
            }
            writeRef(writer, node, Element.HANDOFF_EXECUTOR, HANDOFF_EXECUTOR);
            writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
            writeThreads(writer, node, Element.MAX_THREADS);
            writeTime(writer, node, Element.KEEPALIVE_TIME);

            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }

            writer.writeEndElement();
        }


        private void writeScheduledQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.SCHEDULED_THREAD_POOL.getLocalName());

            if (node.hasDefined(NAME)) {
                writeAttribute(writer, Attribute.NAME, node.get(NAME));
            }

            writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
            writeThreads(writer, node, Element.MAX_THREADS);
            writeTime(writer, node, Element.KEEPALIVE_TIME);

            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }

            writer.writeEndElement();
        }


        private void writeUnboundedQueueThreadPool(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.UNBOUNDED_QUEUE_THREAD_POOL.getLocalName());

            if (node.hasDefined(NAME)) {
                writeAttribute(writer, Attribute.BLOCKING, node.get(NAME));
            }

            writeRef(writer, node, Element.THREAD_FACTORY, THREAD_FACTORY);
            writeThreads(writer, node, Element.MAX_THREADS);
            writeTime(writer, node, Element.KEEPALIVE_TIME);

            if (node.hasDefined(PROPERTIES)) {
                writeProperties(writer, node.get(PROPERTIES));
            }

            writer.writeEndElement();
        }

        private void writeRef(final XMLExtendedStreamWriter writer, final ModelNode node, Element element, String name) throws XMLStreamException {
            if (node.hasDefined(name)) {
                writer.writeStartElement(element.getLocalName());
                writeAttribute(writer, Attribute.NAME, node.get(name));
                writer.writeEndElement();
            }
        }

        private void writeThreads(final XMLExtendedStreamWriter writer, final ModelNode node, Element element) throws XMLStreamException {
            if (node.hasDefined(element.getLocalName())) {
                writer.writeStartElement(element.getLocalName());
                ModelNode threads = node.get(element.getLocalName());
                if (node.hasDefined(COUNT)) {
                    writeAttribute(writer, Attribute.COUNT, threads.get(COUNT));
                }
                if (node.hasDefined(PER_CPU)) {
                    writeAttribute(writer, Attribute.PER_CPU, threads.get(PER_CPU));
                }
                writer.writeEndElement();
            }
        }

        private void writeTime(final XMLExtendedStreamWriter writer, final ModelNode node, Element element) throws XMLStreamException {
            if (node.hasDefined(element.getLocalName())) {
                writer.writeStartElement(element.getLocalName());
                ModelNode keepalive = node.get(element.getLocalName());
                if (keepalive.hasDefined(TIME)) {
                    writeAttribute(writer, Attribute.TIME, keepalive.get(TIME));
                }
                if (keepalive.hasDefined(UNIT)) {
                    writeAttribute(writer, Attribute.UNIT, keepalive.get(UNIT));
                }
                writer.writeEndElement();
            }
        }

        private void writeProperties(final XMLExtendedStreamWriter writer, final ModelNode node) throws XMLStreamException {
            writer.writeStartElement(Element.PROPERTIES.getLocalName());

            if (node.getType() == ModelType.LIST) {
                for (ModelNode prop : node.asList()) {
                    if (prop.getType() == ModelType.PROPERTY) {
                        writer.writeStartElement(Element.PROPERTY.getLocalName());

                        final Property property = prop.asProperty();
                        writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                        writeAttribute(writer, Attribute.VALUE, property.getValue());

                        writer.writeEndElement();
                    }
                }
            }
            writer.writeEndElement();
        }

        private void writeAttribute(final XMLExtendedStreamWriter writer, final Attribute attr, final ModelNode value) throws XMLStreamException {
            writer.writeAttribute(attr.getLocalName(), value.asString());
        }
    }

    private static class ThreadsSubsystemDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final ThreadsSubsystemDescribeHandler INSTANCE = new ThreadsSubsystemDescribeHandler();
        @Override
        public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            ModelNode result = new ModelNode();

            result.add(Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME))));

            final ModelNode model = context.getSubModel();
            addBoundedQueueThreadPools(result, model);
            addQueuelessThreadPools(result, model);
            addScheduledThreadPools(result, model);
            addThreadFactories(result, model);
            addUnboundedQueueThreadPools(result, model);

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete(new ModelNode());
            return Cancellable.NULL;
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }

        private void addBoundedQueueThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(BOUNDED_QUEUE_THREAD_POOL)) {
                ModelNode pools = model.get(BOUNDED_QUEUE_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(BOUNDED_QUEUE_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    if (pool.hasDefined(BLOCKING)) {
                        operation.get(BLOCKING).set(pool.get(BLOCKING));
                    }
                    if (pool.hasDefined(HANDOFF_EXECUTOR)) {
                        operation.get(HANDOFF_EXECUTOR).set(pool.get(HANDOFF_EXECUTOR));
                    }
                    if (pool.hasDefined(ALLOW_CORE_TIMEOUT)) {
                        operation.get(ALLOW_CORE_TIMEOUT).set(pool.get(ALLOW_CORE_TIMEOUT));
                    }
                    if (pool.hasDefined(QUEUE_LENGTH)) {
                        operation.get(QUEUE_LENGTH).set(pool.get(QUEUE_LENGTH));
                    }
                    if (pool.hasDefined(CORE_THREADS)) {
                        operation.get(CORE_THREADS).set(pool.get(CORE_THREADS));
                    }
                    result.add(operation);
                }
            }
        }

        private void addQueuelessThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(QUEUELESS_THREAD_POOL)) {
                ModelNode pools = model.get(QUEUELESS_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(QUEUELESS_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    if (pool.hasDefined(BLOCKING)) {
                        operation.get(BLOCKING).set(pool.get(BLOCKING));
                    }
                    if (pool.hasDefined(HANDOFF_EXECUTOR)) {
                        operation.get(HANDOFF_EXECUTOR).set(pool.get(HANDOFF_EXECUTOR));
                    }
                    result.add(operation);
                }
            }
        }

        private void addThreadFactories(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(THREAD_FACTORY)) {
                ModelNode pools = model.get(THREAD_FACTORY);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(THREAD_FACTORY, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(GROUP_NAME)) {
                        operation.get(GROUP_NAME).set(pool.get(GROUP_NAME));
                    }
                    if (pool.hasDefined(THREAD_NAME_PATTERN)) {
                        operation.get(THREAD_NAME_PATTERN).set(pool.get(THREAD_NAME_PATTERN));
                    }
                    if (pool.hasDefined(PRIORITY)) {
                        operation.get(PRIORITY).set(pool.get(PRIORITY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    result.add(operation);
                }
            }
        }

        private void addScheduledThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(SCHEDULED_THREAD_POOL)) {
                ModelNode pools = model.get(SCHEDULED_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(SCHEDULED_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    result.add(operation);
                }
            }
        }

        private void addUnboundedQueueThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(UNBOUNDED_QUEUE_THREAD_POOL)) {
                ModelNode pools = model.get(UNBOUNDED_QUEUE_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(UNBOUNDED_QUEUE_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    result.add(operation);
                }
            }
        }

        private ModelNode pathAddress(PathElement...elements) {
            return PathAddress.pathAddress(elements).toModelNode();
        }
    }

}
