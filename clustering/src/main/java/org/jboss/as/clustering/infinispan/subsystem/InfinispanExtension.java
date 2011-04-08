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
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Paul Ferraro
 */
public class InfinispanExtension implements Extension, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext>, DescriptionProvider {

    private static final String SUBSYSTEM_NAME = "infinispan";

    private static final PathElement containerPath = PathElement.pathElement(ModelKeys.CACHE_CONTAINER);
    private static final InfinispanSubsystemAdd add = new InfinispanSubsystemAdd();
    private static final InfinispanSubsystemDescribe describe = new InfinispanSubsystemDescribe();
    private static final CacheContainerAdd containerAdd = new CacheContainerAdd();
    private static final CacheContainerRemove containerRemove = new CacheContainerRemove();
    private static final DescriptionProvider containerDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return LocalDescriptions.getCacheContainerDescription(locale);
        }
    };

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        subsystem.registerXMLElementWriter(this);

        ModelNodeRegistration registration = subsystem.registerSubsystemModel(this);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, add, add, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, describe, describe, false, EntryType.PRIVATE);

        ModelNodeRegistration containers = registration.registerSubModel(containerPath, containerDescription);
        containers.registerOperationHandler(ModelDescriptionConstants.ADD, containerAdd, containerAdd, false);
        containers.registerOperationHandler(ModelDescriptionConstants.REMOVE, containerRemove, containerRemove, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUri(), this);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return LocalDescriptions.getSubsystemDescription(locale);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        address.protect();

        ModelNode subsystem = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_CONTAINER: {
                    subsystem.get(ModelKeys.DEFAULT_CONTAINER).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case INFINISPAN_1_0: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CACHE_CONTAINER: {
                            operations.add(this.parseContainer(reader, address));
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private ModelNode parseContainer(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        ModelNode container = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        container.get(ModelDescriptionConstants.OP_ADDR).set(address);
        String name = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                case DEFAULT_CACHE: {
                    container.get(ModelKeys.DEFAULT_CACHE).set(value);
                    break;
                }
                case LISTENER_EXECUTOR: {
                    container.get(ModelKeys.LISTENER_EXECUTOR).set(value);
                    break;
                }
                case EVICTION_EXECUTOR: {
                    container.get(ModelKeys.EVICTION_EXECUTOR).set(value);
                    break;
                }
                case REPLICATION_QUEUE_EXECUTOR: {
                    container.get(ModelKeys.REPLICATION_QUEUE_EXECUTOR).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        container.get(ModelDescriptionConstants.OP_ADDR).add(ModelKeys.CACHE_CONTAINER, name);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ALIAS: {
                    container.get(ModelKeys.ALIAS).add(reader.getElementText());
                    break;
                }
                case TRANSPORT: {
                    this.parseTransport(reader, container.get(ModelKeys.TRANSPORT));
                    break;
                }
                case LOCAL_CACHE: {
                    this.parseLocalCache(reader, container.get(ModelKeys.CACHE).add());
                    break;
                }
                case INVALIDATION_CACHE: {
                    this.parseInvalidationCache(reader, container.get(ModelKeys.CACHE).add());
                    break;
                }
                case REPLICATED_CACHE: {
                    this.parseReplicatedCache(reader, container.get(ModelKeys.CACHE).add());
                    break;
                }
                case DISTRIBUTED_CACHE: {
                    this.parseDistributedCache(reader, container.get(ModelKeys.CACHE).add());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        return container;
    }

    private void parseTransport(XMLExtendedStreamReader reader, ModelNode transport) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STACK: {
                    transport.get(ModelKeys.STACK).set(value);
                    break;
                }
                case EXECUTOR: {
                    transport.get(ModelKeys.EXECUTOR).set(value);
                    break;
                }
                case LOCK_TIMEOUT: {
                    transport.get(ModelKeys.LOCK_TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                case SITE: {
                    transport.get(ModelKeys.SITE).set(value);
                    break;
                }
                case RACK: {
                    transport.get(ModelKeys.RACK).set(value);
                    break;
                }
                case MACHINE: {
                    transport.get(ModelKeys.MACHINE).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache) throws XMLStreamException {
        switch (attribute) {
            case NAME: {
                cache.get(ModelKeys.NAME).set(value);
                break;
            }
            case BATCHING: {
                cache.get(ModelKeys.BATCHING).set(Boolean.parseBoolean(value));
                break;
            }
            case INDEXING: {
                try {
                    Indexing indexing = Indexing.valueOf(value);
                    cache.get(ModelKeys.INDEXING).set(indexing.name());
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache, CacheMode cacheMode) throws XMLStreamException {
        switch (attribute) {
            case MODE: {
                try {
                    Mode mode = Mode.valueOf(value);
                    cache.get(ModelKeys.MODE).set(mode.apply(cacheMode).name());
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                break;
            }
            case QUEUE_SIZE: {
                cache.get(ModelKeys.QUEUE_SIZE).set(Integer.parseInt(value));
                break;
            }
            case QUEUE_FLUSH_INTERVAL: {
                cache.get(ModelKeys.QUEUE_FLUSH_INTERVAL).set(Long.parseLong(value));
                break;
            }
            case REMOTE_TIMEOUT: {
                cache.get(ModelKeys.REMOTE_TIMEOUT).set(Long.parseLong(value));
                break;
            }
            default: {
                this.parseCacheAttribute(reader, index, attribute, value, cache);
            }
        }
    }

    private void parseLocalCache(XMLExtendedStreamReader reader, ModelNode cache) throws XMLStreamException {
        cache.get(ModelKeys.MODE).set(CacheMode.LOCAL.name());
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseCacheAttribute(reader, i, attribute, value, cache);
        }

        this.parseCache(reader, cache);
    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, ModelNode cache) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    cache.get(ModelKeys.OWNERS).set(Integer.parseInt(value));
                    break;
                }
                case L1_LIFESPAN: {
                    cache.get(ModelKeys.L1_LIFESPAN).set(Long.parseLong(value));
                }
                default: {
                    this.parseClusteredCacheAttribute(reader, i, attribute, value, cache, CacheMode.DIST_SYNC);
                }
            }
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REHASHING: {
                    this.parseRehashing(reader, cache.get(ModelKeys.REHASHING));
                    break;
                }
                default: {
                    this.parseCacheElement(reader, element, cache);
                }
            }
        }
    }

    private void parseReplicatedCache(XMLExtendedStreamReader reader, ModelNode cache) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache, CacheMode.REPL_SYNC);
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STATE_TRANSFER: {
                    this.parseStateTransfer(reader, cache.get(ModelKeys.STATE_TRANSFER));
                    break;
                }
                default: {
                    this.parseCacheElement(reader, element, cache);
                }
            }
        }
    }

    private void parseInvalidationCache(XMLExtendedStreamReader reader, ModelNode cache) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache, CacheMode.INVALIDATION_SYNC);
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseCacheElement(reader, element, cache);
        }
    }

    private void parseCache(XMLExtendedStreamReader reader, ModelNode cache) throws XMLStreamException {

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseCacheElement(reader, element, cache);
        }
    }

    private void parseCacheElement(XMLExtendedStreamReader reader, Element element, ModelNode cache) throws XMLStreamException {
        switch (element) {
            case LOCKING: {
                this.parseLocking(reader, cache.get(ModelKeys.LOCKING));
                break;
            }
            case TRANSACTION: {
                this.parseTransaction(reader, cache.get(ModelKeys.TRANSACTION));
                break;
            }
            case EVICTION: {
                this.parseEviction(reader, cache.get(ModelKeys.EVICTION));
                break;
            }
            case EXPIRATION: {
                this.parseExpiration(reader, cache.get(ModelKeys.EXPIRATION));
                break;
            }
            case STORE: {
                this.parseCustomStore(reader, cache.get(ModelKeys.STORE));
                break;
            }
            case FILE_STORE: {
                this.parseFileStore(reader, cache.get(ModelKeys.STORE));
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseRehashing(XMLExtendedStreamReader reader, ModelNode rehashing) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    rehashing.get(ModelKeys.ENABLED).set(Boolean.parseBoolean(value));
                    break;
                }
                case TIMEOUT: {
                    rehashing.get(ModelKeys.TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseStateTransfer(XMLExtendedStreamReader reader, ModelNode stateTransfer) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    stateTransfer.get(ModelKeys.ENABLED).set(Boolean.parseBoolean(value));
                    break;
                }
                case TIMEOUT: {
                    stateTransfer.get(ModelKeys.TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                case FLUSH_TIMEOUT: {
                    stateTransfer.get(ModelKeys.FLUSH_TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseLocking(XMLExtendedStreamReader reader, ModelNode locking) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    try {
                        IsolationLevel level = IsolationLevel.valueOf(value);
                        locking.get(ModelKeys.ISOLATION).set(level.name());
                    } catch (IllegalArgumentException e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case STRIPING: {
                    locking.get(ModelKeys.STRIPING).set(Boolean.parseBoolean(value));
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    locking.get(ModelKeys.ACQUIRE_TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    locking.get(ModelKeys.CONCURRENCY_LEVEL).set(Integer.parseInt(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseTransaction(XMLExtendedStreamReader reader, ModelNode transaction) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    transaction.get(ModelKeys.STOP_TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                case SYNC_PHASE: {
                    try {
                        SyncPhase phase = SyncPhase.valueOf(value);
                        transaction.get(ModelKeys.SYNC_PHASE).set(phase.name());
                    } catch (IllegalArgumentException e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case EAGER_LOCKING: {
                    try {
                        EagerLocking eager = EagerLocking.valueOf(value);
                        transaction.get(ModelKeys.EAGER_LOCKING).set(eager.name());
                    } catch (IllegalArgumentException e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseEviction(XMLExtendedStreamReader reader, ModelNode eviction) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STRATEGY: {
                    try {
                        EvictionStrategy strategy = EvictionStrategy.valueOf(value);
                        eviction.get(ModelKeys.STRATEGY).set(strategy.name());
                    } catch (IllegalArgumentException e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case MAX_ENTRIES: {
                    eviction.get(ModelKeys.MAX_ENTRIES).set(Integer.parseInt(value));
                    break;
                }
                case INTERVAL: {
                    eviction.get(ModelKeys.INTERVAL).set(Long.parseLong(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseExpiration(XMLExtendedStreamReader reader, ModelNode eviction) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    eviction.get(ModelKeys.MAX_IDLE).set(Long.parseLong(value));
                    break;
                }
                case LIFESPAN: {
                    eviction.get(ModelKeys.LIFESPAN).set(Long.parseLong(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseCustomStore(XMLExtendedStreamReader reader, ModelNode loader) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    loader.get(ModelKeys.CLASS).set(value);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, loader);
                }
            }
        }

        if (!loader.hasDefined(ModelKeys.CLASS)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.CLASS));
        }

        this.parseStoreProperties(reader, loader);
    }

    private void parseFileStore(XMLExtendedStreamReader reader, ModelNode store) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    store.get(ModelKeys.RELATIVE_TO).set(value);
                    break;
                }
                case PATH: {
                    store.get(ModelKeys.PATH).set(value);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        this.parseStoreProperties(reader, store);
    }

    private void parseStoreAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode store) throws XMLStreamException {
        switch (attribute) {
            case SHARED: {
                store.get(ModelKeys.SHARED).set(Boolean.parseBoolean(value));
                break;
            }
            case PRELOAD: {
                store.get(ModelKeys.PRELOAD).set(Boolean.parseBoolean(value));
                break;
            }
            case PASSIVATION: {
                store.get(ModelKeys.PASSIVATION).set(Boolean.parseBoolean(value));
                break;
            }
            case FETCH_STATE: {
                store.get(ModelKeys.FETCH_STATE).set(Boolean.parseBoolean(value));
                break;
            }
            case PURGE: {
                store.get(ModelKeys.PURGE).set(Boolean.parseBoolean(value));
                break;
            }
            case SINGLETON: {
                store.get(ModelKeys.SINGLETON).set(Boolean.parseBoolean(value));
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseStoreProperties(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROPERTY: {
                    int attributes = reader.getAttributeCount();
                    String property = null;
                    for (int i = 0; i < attributes; i++) {
                        String value = reader.getAttributeValue(i);
                        Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case NAME: {
                                property = value;
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    if (property == null) {
                        throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
                    }
                    String value = reader.getElementText();
                    node.get(ModelKeys.PROPERTY).add(property, value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementWriter#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter, java.lang.Object)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            if (model.hasDefined(ModelKeys.DEFAULT_CONTAINER)) {
                writer.writeAttribute(Attribute.DEFAULT_CONTAINER.getLocalName(), model.get(ModelKeys.DEFAULT_CONTAINER).asString());
            }
            for (Property entry: model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
                writer.writeStartElement(Element.CACHE_CONTAINER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getName());
                ModelNode container = entry.getValue();
                this.writeOptionalAttribute(writer, Attribute.DEFAULT_CACHE, container, ModelKeys.DEFAULT_CACHE);
                this.writeOptionalAttribute(writer, Attribute.LISTENER_EXECUTOR, container, ModelKeys.LISTENER_EXECUTOR);
                this.writeOptionalAttribute(writer, Attribute.EVICTION_EXECUTOR, container, ModelKeys.EVICTION_EXECUTOR);
                this.writeOptionalAttribute(writer, Attribute.REPLICATION_QUEUE_EXECUTOR, container, ModelKeys.REPLICATION_QUEUE_EXECUTOR);

                if (container.hasDefined(ModelKeys.ALIAS)) {
                    for (ModelNode alias: container.get(ModelKeys.ALIAS).asList()) {
                        writer.writeStartElement(Element.ALIAS.getLocalName());
                        writer.writeCharacters(alias.asString());
                        writer.writeEndElement();
                    }
                }

                if (container.hasDefined(ModelKeys.TRANSPORT)) {
                    writer.writeStartElement(Element.TRANSPORT.getLocalName());
                    ModelNode transport = container.get(ModelKeys.TRANSPORT);
                    this.writeOptionalAttribute(writer, Attribute.STACK, transport, ModelKeys.STACK);
                    this.writeOptionalAttribute(writer, Attribute.EXECUTOR, transport, ModelKeys.EXECUTOR);
                    this.writeOptionalAttribute(writer, Attribute.LOCK_TIMEOUT, transport, ModelKeys.LOCK_TIMEOUT);
                    this.writeOptionalAttribute(writer, Attribute.SITE, transport, ModelKeys.SITE);
                    this.writeOptionalAttribute(writer, Attribute.RACK, transport, ModelKeys.RACK);
                    this.writeOptionalAttribute(writer, Attribute.MACHINE, transport, ModelKeys.MACHINE);
                }

                for (ModelNode cache: container.get(ModelKeys.CACHE).asList()) {
                    CacheMode mode = CacheMode.valueOf(cache.get(ModelKeys.MODE).asString());
                    if (mode.isClustered()) {
                        if (mode.isDistributed()) {
                            writer.writeStartElement(Element.DISTRIBUTED_CACHE.getLocalName());
                            this.writeOptionalAttribute(writer, Attribute.OWNERS, cache, ModelKeys.OWNERS);
                            this.writeOptionalAttribute(writer, Attribute.L1_LIFESPAN, cache, ModelKeys.L1_LIFESPAN);
                        } else if (mode.isInvalidation()) {
                            writer.writeStartElement(Element.INVALIDATION_CACHE.getLocalName());
                        } else {
                            writer.writeStartElement(Element.REPLICATED_CACHE.getLocalName());
                        }
                        writer.writeAttribute(ModelKeys.MODE, String.valueOf(Mode.forCacheMode(mode).name()));
                        this.writeOptionalAttribute(writer, Attribute.QUEUE_SIZE, cache, ModelKeys.QUEUE_SIZE);
                        this.writeOptionalAttribute(writer, Attribute.QUEUE_FLUSH_INTERVAL, cache, ModelKeys.QUEUE_FLUSH_INTERVAL);
                        this.writeOptionalAttribute(writer, Attribute.REMOTE_TIMEOUT, cache, ModelKeys.REMOTE_TIMEOUT);
                    } else {
                        writer.writeStartElement(Element.LOCAL_CACHE.getLocalName());
                    }

                    if (cache.hasDefined(ModelKeys.LOCKING)) {
                        writer.writeStartElement(Element.LOCKING.getLocalName());
                        ModelNode locking = cache.get(ModelKeys.LOCKING);
                        this.writeOptionalAttribute(writer, Attribute.ISOLATION, locking, ModelKeys.ISOLATION);
                        this.writeOptionalAttribute(writer, Attribute.STRIPING, locking, ModelKeys.STRIPING);
                        this.writeOptionalAttribute(writer, Attribute.ACQUIRE_TIMEOUT, locking, ModelKeys.ACQUIRE_TIMEOUT);
                        this.writeOptionalAttribute(writer, Attribute.CONCURRENCY_LEVEL, locking, ModelKeys.CONCURRENCY_LEVEL);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.TRANSACTION)) {
                        writer.writeStartElement(Element.TRANSACTION.getLocalName());
                        ModelNode transaction = cache.get(ModelKeys.TRANSACTION);
                        this.writeOptionalAttribute(writer, Attribute.STOP_TIMEOUT, transaction, ModelKeys.STOP_TIMEOUT);
                        this.writeOptionalAttribute(writer, Attribute.SYNC_PHASE, transaction, ModelKeys.SYNC_PHASE);
                        this.writeOptionalAttribute(writer, Attribute.EAGER_LOCKING, transaction, ModelKeys.EAGER_LOCKING);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.EVICTION)) {
                        writer.writeStartElement(Element.EVICTION.getLocalName());
                        ModelNode eviction = cache.get(ModelKeys.EVICTION);
                        this.writeOptionalAttribute(writer, Attribute.STRATEGY, eviction, ModelKeys.STRATEGY);
                        this.writeOptionalAttribute(writer, Attribute.MAX_ENTRIES, eviction, ModelKeys.MAX_ENTRIES);
                        this.writeOptionalAttribute(writer, Attribute.INTERVAL, eviction, ModelKeys.INTERVAL);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.EXPIRATION)) {
                        writer.writeStartElement(Element.EXPIRATION.getLocalName());
                        ModelNode expiration = cache.get(ModelKeys.EXPIRATION);
                        this.writeOptionalAttribute(writer, Attribute.MAX_IDLE, expiration, ModelKeys.MAX_IDLE);
                        this.writeOptionalAttribute(writer, Attribute.LIFESPAN, expiration, ModelKeys.LIFESPAN);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.STORE)) {
                        ModelNode store = cache.get(ModelKeys.STORE);
                        if (store.hasDefined(ModelKeys.CLASS)) {
                            writer.writeStartElement(Element.STORE.getLocalName());
                            this.writeRequiredAttribute(writer, Attribute.CLASS, store, ModelKeys.CLASS);
                        } else {
                            writer.writeStartElement(Element.FILE_STORE.getLocalName());
                            this.writeOptionalAttribute(writer, Attribute.RELATIVE_TO, store, ModelKeys.RELATIVE_TO);
                            this.writeOptionalAttribute(writer, Attribute.PATH, store, ModelKeys.PATH);
                        }
                        this.writeOptionalAttribute(writer, Attribute.SHARED, store, ModelKeys.SHARED);
                        this.writeOptionalAttribute(writer, Attribute.PRELOAD, store, ModelKeys.PRELOAD);
                        this.writeOptionalAttribute(writer, Attribute.PASSIVATION, store, ModelKeys.PASSIVATION);
                        this.writeOptionalAttribute(writer, Attribute.FETCH_STATE, store, ModelKeys.FETCH_STATE);
                        this.writeOptionalAttribute(writer, Attribute.PURGE, store, ModelKeys.PURGE);
                        this.writeOptionalAttribute(writer, Attribute.SINGLETON, store, ModelKeys.SINGLETON);
                        if (store.hasDefined(ModelKeys.PROPERTY)) {
                            for (Property property: store.get(ModelKeys.PROPERTY).asPropertyList()) {
                                writer.writeStartElement(Element.PROPERTY.getLocalName());
                                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                                writer.writeCharacters(property.getValue().asString());
                                writer.writeEndElement();
                            }
                        }
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.STATE_TRANSFER)) {
                        ModelNode stateTransfer = cache.get(ModelKeys.STATE_TRANSFER);
                        writer.writeStartElement(Element.STATE_TRANSFER.getLocalName());
                        this.writeOptionalAttribute(writer, Attribute.ENABLED, stateTransfer, ModelKeys.ENABLED);
                        this.writeOptionalAttribute(writer, Attribute.TIMEOUT, stateTransfer, ModelKeys.TIMEOUT);
                        this.writeOptionalAttribute(writer, Attribute.FLUSH_TIMEOUT, stateTransfer, ModelKeys.FLUSH_TIMEOUT);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.REHASHING)) {
                        ModelNode rehashing = cache.get(ModelKeys.REHASHING);
                        writer.writeStartElement(Element.REHASHING.getLocalName());
                        this.writeOptionalAttribute(writer, Attribute.ENABLED, rehashing, ModelKeys.ENABLED);
                        this.writeOptionalAttribute(writer, Attribute.TIMEOUT, rehashing, ModelKeys.TIMEOUT);
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeOptionalAttribute(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        if (model.hasDefined(key)) {
            writer.writeAttribute(attribute.getLocalName(), model.get(key).asString());
        }
    }

    private void writeRequiredAttribute(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), model.require(key).asString());
    }
}
