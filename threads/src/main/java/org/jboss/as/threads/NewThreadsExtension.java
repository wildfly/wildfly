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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.model.ParseUtils.invalidAttributeValue;
import static org.jboss.as.model.ParseUtils.missingRequired;
import static org.jboss.as.model.ParseUtils.missingRequiredElement;
import static org.jboss.as.model.ParseUtils.unexpectedAttribute;
import static org.jboss.as.model.ParseUtils.unexpectedElement;
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
import static org.jboss.as.threads.NewThreadsSubsystemProviders.SUBSYSTEM;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.SUBSYSTEM_ADD_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.THREAD_FACTORY_DESC;
import static org.jboss.as.threads.NewThreadsSubsystemProviders.UNBOUNDED_QUEUE_THREAD_POOL_DESC;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.model.ParseUtils;
import org.jboss.dmr.ModelNode;
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

    @Override
    public void initialize(final NewExtensionContext context) {
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(THREADS);

        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM);
        subsystem.registerOperationHandler("add", NewThreadsSubsystemAdd.INSTANCE, SUBSYSTEM_ADD_DESC, false);

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
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewThreadsSubsystemParser.INSTANCE, NewThreadsSubsystemParser.INSTANCE);
    }

    static final class NewThreadsSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<ModelNode> {

        static final NewThreadsSubsystemParser INSTANCE = new NewThreadsSubsystemParser();

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, ModelNode node) throws XMLStreamException {

        }

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {

            // FIXME this should come from somewhere
            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, "threads");
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
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
                        Map<String, String> props = parseProperties(reader);
                        if (props != null && props.size() > 0) {
                            ModelNode properties = op.get(PROPERTIES);
                            for (Map.Entry<String, String> prop : props.entrySet()) {
                                properties.add(prop.getKey(), prop.getValue());
                            }
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
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
            }

            if (name == null) {
                throw missingRequired(reader, Collections.singleton(Attribute.NAME));
            }

            //FIXME Make relative and use this scheme to add the addresses
            //address.add("profile", "test).add("subsystem", "threads")
            final ModelNode address = parentAddress.clone();
            address.add(BOUNDED_QUEUE_THREAD_POOL, name);
            address.protect();
            op.get(ADDRESS).set(address);

            boolean foundQueueLength = false;
            boolean foundMaxThreads = false;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case CORE_THREADS: {
                        ScaledCount core = parseScaledCount(reader);
                        op.get(CORE_THREADS_COUNT).set(core.getCount());
                        op.get(CORE_THREADS_PER_CPU).set(core.getPerCpu());
                        break;
                    }
                    case HANDOFF_EXECUTOR: {
                        op.get(HANDOFF_EXECUTOR).set(parseRef(reader));
                        break;
                    }
                    case MAX_THREADS: {
                        ScaledCount maxThreads = parseScaledCount(reader);
                        op.get(MAX_THREADS_COUNT).set(maxThreads.getCount());
                        op.get(MAX_THREADS_PER_CPU).set(maxThreads.getPerCpu());
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        TimeSpec keepAliveTime = parseTimeSpec(reader);
                        op.get(KEEPALIVE_TIME_DURATION).set(keepAliveTime.getDuration());
                        op.get(KEEPALIVE_TIME_UNIT).set(keepAliveTime.getUnit().toString());
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(Constants.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        Map<String, String> props = parseProperties(reader);
                        if (props != null && props.size() > 0) {
                            ModelNode properties = op.get(PROPERTIES);
                            for (Map.Entry<String, String> prop : props.entrySet()) {
                                properties.add(prop.getKey(), prop.getValue());
                            }
                        }
                        break;
                    }
                    case QUEUE_LENGTH: {
                        ScaledCount core = parseScaledCount(reader);
                        op.get(QUEUE_LENGTH_COUNT).set(core.getCount());
                        op.get(QUEUE_LENGTH_PER_CPU).set(core.getPerCpu());
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
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
                        ScaledCount maxThreads = parseScaledCount(reader);
                        op.get(MAX_THREADS_COUNT).set(maxThreads.getCount());
                        op.get(MAX_THREADS_PER_CPU).set(maxThreads.getPerCpu());
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        TimeSpec keepAliveTime = parseTimeSpec(reader);
                        op.get(KEEPALIVE_TIME_DURATION).set(keepAliveTime.getDuration());
                        op.get(KEEPALIVE_TIME_UNIT).set(keepAliveTime.getUnit().toString());
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(Constants.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        Map<String, String> props = parseProperties(reader);
                        if (props != null && props.size() > 0) {
                            ModelNode properties = op.get(PROPERTIES);
                            for (Map.Entry<String, String> prop : props.entrySet()) {
                                properties.add(prop.getKey(), prop.getValue());
                            }
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
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
                        ScaledCount maxThreads = parseScaledCount(reader);
                        op.get(MAX_THREADS_COUNT).set(maxThreads.getCount());
                        op.get(MAX_THREADS_PER_CPU).set(maxThreads.getPerCpu());
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        TimeSpec keepAliveTime = parseTimeSpec(reader);
                        op.get(KEEPALIVE_TIME_DURATION).set(keepAliveTime.getDuration());
                        op.get(KEEPALIVE_TIME_UNIT).set(keepAliveTime.getUnit().toString());
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(Constants.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        Map<String, String> props = parseProperties(reader);
                        if (props != null && props.size() > 0) {
                            ModelNode properties = op.get(PROPERTIES);
                            for (Map.Entry<String, String> prop : props.entrySet()) {
                                properties.add(prop.getKey(), prop.getValue());
                            }
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
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
                        ScaledCount maxThreads = parseScaledCount(reader);
                        op.get(MAX_THREADS_COUNT).set(maxThreads.getCount());
                        op.get(MAX_THREADS_PER_CPU).set(maxThreads.getPerCpu());
                        foundMaxThreads = true;
                        break;
                    }
                    case KEEPALIVE_TIME: {
                        TimeSpec keepAliveTime = parseTimeSpec(reader);
                        op.get(KEEPALIVE_TIME_DURATION).set(keepAliveTime.getDuration());
                        op.get(KEEPALIVE_TIME_UNIT).set(keepAliveTime.getUnit().toString());
                        break;
                    }
                    case THREAD_FACTORY: {
                        op.get(Constants.THREAD_FACTORY).set(parseRef(reader));
                        break;
                    }
                    case PROPERTIES: {
                        Map<String, String> props = parseProperties(reader);
                        if (props != null && props.size() > 0) {
                            ModelNode properties = op.get(PROPERTIES);
                            for (Map.Entry<String, String> prop : props.entrySet()) {
                                properties.add(prop.getKey(), prop.getValue());
                            }
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

        private ScaledCount parseScaledCount(final XMLExtendedStreamReader reader) throws XMLStreamException {
            final int attrCount = reader.getAttributeCount();
            BigDecimal count = null;
            BigDecimal perCpu = null;
            for (int i = 0; i < attrCount; i++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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

            return new ScaledCount(count, perCpu);
        }


        private TimeSpec parseTimeSpec(final XMLExtendedStreamReader reader) throws XMLStreamException {
            final int attrCount = reader.getAttributeCount();
            TimeUnit unit = null;
            Long duration = null;
            for (int i = 0; i < attrCount; i++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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

            return new TimeSpec(unit, duration);
        }

        private String parseRef(XMLExtendedStreamReader reader) throws XMLStreamException {
            final int attrCount = reader.getAttributeCount();
            String refName = null;
            for (int i = 0; i < attrCount; i++) {
                final String value = reader.getAttributeValue(i);
                if (reader.getAttributeNamespace(i) != null) {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                } else {
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
            }

            if (refName == null) {
                throw missingRequired(reader, Collections.singleton(NAME));
            }
            if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                throw unexpectedElement(reader);
            }

            return refName;
        }

        private Map<String, String> parseProperties(final XMLExtendedStreamReader reader) throws XMLStreamException {
            final Map<String, String> props = new HashMap<String, String>();
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Element.forName(reader.getLocalName())) {
                    case PROPERTY: {
                        final int attrCount = reader.getAttributeCount();
                        String propName = null;
                        String propValue = null;
                        for (int i = 0; i < attrCount; i++) {
                            final String value = reader.getAttributeValue(i);
                            if (reader.getAttributeNamespace(i) != null) {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            } else {
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
                        props.put(propName, propValue);

                        if (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                            throw unexpectedElement(reader);
                        }
                    }
                }
            }
            return props;
        }
    }
}
