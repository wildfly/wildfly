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
import org.jboss.as.controller.registry.ManagementResourceRegistration;
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

    static final String SUBSYSTEM_NAME = "infinispan";

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

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(this);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, add, add, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, describe, describe, false, EntryType.PRIVATE);

        ManagementResourceRegistration containers = registration.registerSubModel(containerPath, containerDescription);
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
                case DEFAULT_CACHE_CONTAINER: {
                    subsystem.get(ModelKeys.DEFAULT_CACHE_CONTAINER).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!subsystem.hasDefined(ModelKeys.DEFAULT_CACHE_CONTAINER)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.DEFAULT_CACHE_CONTAINER));
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
                case JNDI_NAME: {
                    container.get(ModelKeys.JNDI_NAME).set(value);
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

        if ((name == null) || !container.hasDefined(ModelKeys.DEFAULT_CACHE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME, Attribute.DEFAULT_CACHE));
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
                    this.parseTransport(reader, container.get(ModelKeys.TRANSPORT).setEmptyObject());
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
            case START: {
                try {
                    StartMode mode = StartMode.valueOf(value);
                    cache.get(ModelKeys.START).set(mode.name());
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
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

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseCacheElement(reader, element, cache);
        }
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
                case VIRTUAL_NODES: {
                    cache.get(ModelKeys.VIRTUAL_NODES).set(Integer.parseInt(value));
                    break;
                }
                case L1_LIFESPAN: {
                    cache.get(ModelKeys.L1_LIFESPAN).set(Long.parseLong(value));
                    break;
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
                    this.parseRehashing(reader, cache.get(ModelKeys.REHASHING).setEmptyObject());
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
                    this.parseStateTransfer(reader, cache.get(ModelKeys.STATE_TRANSFER).setEmptyObject());
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

    private void parseCacheElement(XMLExtendedStreamReader reader, Element element, ModelNode cache) throws XMLStreamException {
        switch (element) {
            case LOCKING: {
                this.parseLocking(reader, cache.get(ModelKeys.LOCKING).setEmptyObject());
                break;
            }
            case TRANSACTION: {
                this.parseTransaction(reader, cache.get(ModelKeys.TRANSACTION).setEmptyObject());
                break;
            }
            case EVICTION: {
                this.parseEviction(reader, cache.get(ModelKeys.EVICTION).setEmptyObject());
                break;
            }
            case EXPIRATION: {
                this.parseExpiration(reader, cache.get(ModelKeys.EXPIRATION).setEmptyObject());
                break;
            }
            case STORE: {
                this.parseCustomStore(reader, cache.get(ModelKeys.STORE).setEmptyObject());
                break;
            }
            case FILE_STORE: {
                this.parseFileStore(reader, cache.get(ModelKeys.STORE).setEmptyObject());
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
                case MODE: {
                    try {
                        TransactionMode mode = TransactionMode.valueOf(value);
                        transaction.get(ModelKeys.MODE).set(mode.name());
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

    private void parseCustomStore(XMLExtendedStreamReader reader, ModelNode store) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    store.get(ModelKeys.CLASS).set(value);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        if (!store.hasDefined(ModelKeys.CLASS)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.CLASS));
        }

        this.parseStoreProperties(reader, store);
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
            writer.writeAttribute(Attribute.DEFAULT_CACHE_CONTAINER.getLocalName(), model.require(ModelKeys.DEFAULT_CACHE_CONTAINER).asString());
            for (Property entry: model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
                writer.writeStartElement(Element.CACHE_CONTAINER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), entry.getName());
                ModelNode container = entry.getValue();
                this.writeRequired(writer, Attribute.DEFAULT_CACHE, container, ModelKeys.DEFAULT_CACHE);
                this.writeOptional(writer, Attribute.JNDI_NAME, container, ModelKeys.JNDI_NAME);
                this.writeOptional(writer, Attribute.LISTENER_EXECUTOR, container, ModelKeys.LISTENER_EXECUTOR);
                this.writeOptional(writer, Attribute.EVICTION_EXECUTOR, container, ModelKeys.EVICTION_EXECUTOR);
                this.writeOptional(writer, Attribute.REPLICATION_QUEUE_EXECUTOR, container, ModelKeys.REPLICATION_QUEUE_EXECUTOR);

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
                    this.writeOptional(writer, Attribute.STACK, transport, ModelKeys.STACK);
                    this.writeOptional(writer, Attribute.EXECUTOR, transport, ModelKeys.EXECUTOR);
                    this.writeOptional(writer, Attribute.LOCK_TIMEOUT, transport, ModelKeys.LOCK_TIMEOUT);
                    this.writeOptional(writer, Attribute.SITE, transport, ModelKeys.SITE);
                    this.writeOptional(writer, Attribute.RACK, transport, ModelKeys.RACK);
                    this.writeOptional(writer, Attribute.MACHINE, transport, ModelKeys.MACHINE);
                    writer.writeEndElement();
                }

                for (ModelNode cache: container.get(ModelKeys.CACHE).asList()) {
                    CacheMode mode = CacheMode.valueOf(cache.get(ModelKeys.MODE).asString());
                    if (mode.isClustered()) {
                        if (mode.isDistributed()) {
                            writer.writeStartElement(Element.DISTRIBUTED_CACHE.getLocalName());
                            this.writeOptional(writer, Attribute.OWNERS, cache, ModelKeys.OWNERS);
                            this.writeOptional(writer, Attribute.VIRTUAL_NODES, cache, ModelKeys.VIRTUAL_NODES);
                            this.writeOptional(writer, Attribute.L1_LIFESPAN, cache, ModelKeys.L1_LIFESPAN);
                        } else if (mode.isInvalidation()) {
                            writer.writeStartElement(Element.INVALIDATION_CACHE.getLocalName());
                        } else {
                            writer.writeStartElement(Element.REPLICATED_CACHE.getLocalName());
                        }
                        writer.writeAttribute(Attribute.MODE.getLocalName(), Mode.forCacheMode(mode).name());
                        this.writeOptional(writer, Attribute.QUEUE_SIZE, cache, ModelKeys.QUEUE_SIZE);
                        this.writeOptional(writer, Attribute.QUEUE_FLUSH_INTERVAL, cache, ModelKeys.QUEUE_FLUSH_INTERVAL);
                        this.writeOptional(writer, Attribute.REMOTE_TIMEOUT, cache, ModelKeys.REMOTE_TIMEOUT);
                    } else {
                        writer.writeStartElement(Element.LOCAL_CACHE.getLocalName());
                    }
                    this.writeRequired(writer, Attribute.NAME, cache, ModelKeys.NAME);
                    this.writeOptional(writer, Attribute.START, cache, ModelKeys.START);
                    this.writeOptional(writer, Attribute.BATCHING, cache, ModelKeys.BATCHING);
                    this.writeOptional(writer, Attribute.INDEXING, cache, ModelKeys.INDEXING);
                    if (cache.hasDefined(ModelKeys.LOCKING)) {
                        writer.writeStartElement(Element.LOCKING.getLocalName());
                        ModelNode locking = cache.get(ModelKeys.LOCKING);
                        this.writeOptional(writer, Attribute.ISOLATION, locking, ModelKeys.ISOLATION);
                        this.writeOptional(writer, Attribute.STRIPING, locking, ModelKeys.STRIPING);
                        this.writeOptional(writer, Attribute.ACQUIRE_TIMEOUT, locking, ModelKeys.ACQUIRE_TIMEOUT);
                        this.writeOptional(writer, Attribute.CONCURRENCY_LEVEL, locking, ModelKeys.CONCURRENCY_LEVEL);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.TRANSACTION)) {
                        writer.writeStartElement(Element.TRANSACTION.getLocalName());
                        ModelNode transaction = cache.get(ModelKeys.TRANSACTION);
                        this.writeOptional(writer, Attribute.STOP_TIMEOUT, transaction, ModelKeys.STOP_TIMEOUT);
                        this.writeOptional(writer, Attribute.MODE, transaction, ModelKeys.MODE);
                        this.writeOptional(writer, Attribute.EAGER_LOCKING, transaction, ModelKeys.EAGER_LOCKING);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.EVICTION)) {
                        writer.writeStartElement(Element.EVICTION.getLocalName());
                        ModelNode eviction = cache.get(ModelKeys.EVICTION);
                        this.writeOptional(writer, Attribute.STRATEGY, eviction, ModelKeys.STRATEGY);
                        this.writeOptional(writer, Attribute.MAX_ENTRIES, eviction, ModelKeys.MAX_ENTRIES);
                        this.writeOptional(writer, Attribute.INTERVAL, eviction, ModelKeys.INTERVAL);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.EXPIRATION)) {
                        writer.writeStartElement(Element.EXPIRATION.getLocalName());
                        ModelNode expiration = cache.get(ModelKeys.EXPIRATION);
                        this.writeOptional(writer, Attribute.MAX_IDLE, expiration, ModelKeys.MAX_IDLE);
                        this.writeOptional(writer, Attribute.LIFESPAN, expiration, ModelKeys.LIFESPAN);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.STORE)) {
                        ModelNode store = cache.get(ModelKeys.STORE);
                        if (store.hasDefined(ModelKeys.CLASS)) {
                            writer.writeStartElement(Element.STORE.getLocalName());
                            this.writeRequired(writer, Attribute.CLASS, store, ModelKeys.CLASS);
                        } else {
                            writer.writeStartElement(Element.FILE_STORE.getLocalName());
                            this.writeOptional(writer, Attribute.RELATIVE_TO, store, ModelKeys.RELATIVE_TO);
                            this.writeOptional(writer, Attribute.PATH, store, ModelKeys.PATH);
                        }
                        this.writeOptional(writer, Attribute.SHARED, store, ModelKeys.SHARED);
                        this.writeOptional(writer, Attribute.PRELOAD, store, ModelKeys.PRELOAD);
                        this.writeOptional(writer, Attribute.PASSIVATION, store, ModelKeys.PASSIVATION);
                        this.writeOptional(writer, Attribute.FETCH_STATE, store, ModelKeys.FETCH_STATE);
                        this.writeOptional(writer, Attribute.PURGE, store, ModelKeys.PURGE);
                        this.writeOptional(writer, Attribute.SINGLETON, store, ModelKeys.SINGLETON);
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
                        this.writeOptional(writer, Attribute.ENABLED, stateTransfer, ModelKeys.ENABLED);
                        this.writeOptional(writer, Attribute.TIMEOUT, stateTransfer, ModelKeys.TIMEOUT);
                        this.writeOptional(writer, Attribute.FLUSH_TIMEOUT, stateTransfer, ModelKeys.FLUSH_TIMEOUT);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.REHASHING)) {
                        ModelNode rehashing = cache.get(ModelKeys.REHASHING);
                        writer.writeStartElement(Element.REHASHING.getLocalName());
                        this.writeOptional(writer, Attribute.ENABLED, rehashing, ModelKeys.ENABLED);
                        this.writeOptional(writer, Attribute.TIMEOUT, rehashing, ModelKeys.TIMEOUT);
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeOptional(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        if (model.hasDefined(key)) {
            writer.writeAttribute(attribute.getLocalName(), model.get(key).asString());
        }
    }

    private void writeRequired(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), model.require(key).asString());
    }
}
