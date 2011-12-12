package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.InfinispanLogger.ROOT_LOGGER;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.infinispan.config.Configuration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Infinispan subsystem parsing code.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanSubsystemParser_1_0 implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    private static final InfinispanSubsystemParser_1_0 INSTANCE = new InfinispanSubsystemParser_1_0();

    public static InfinispanSubsystemParser_1_0 getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode subsystemAddress = new ModelNode();
        subsystemAddress.add(ModelDescriptionConstants.SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME);
        subsystemAddress.protect();

        ModelNode subsystem = Util.getEmptyOperation(ModelDescriptionConstants.ADD, subsystemAddress);

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

        // command to add the subsystem
        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case INFINISPAN_1_0:
                case INFINISPAN_1_1: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CACHE_CONTAINER: {
                            parseContainer(reader, subsystemAddress, operations);
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

    private void parseContainer(XMLExtendedStreamReader reader, ModelNode subsystemAddress, List<ModelNode> operations) throws XMLStreamException {

        ModelNode container = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
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

        ModelNode containerAddress = subsystemAddress.clone() ;
        containerAddress.add(ModelKeys.CACHE_CONTAINER, name);
        containerAddress.protect() ;
        container.get(ModelDescriptionConstants.OP_ADDR).set(containerAddress);

        // operation to add the container
        operations.add(container);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ALIAS: {
                    container.get(ModelKeys.ALIAS).add(reader.getElementText());
                    break;
                }
                case TRANSPORT: {
                    parseTransport(reader, container);
                    break;
                }
                case LOCAL_CACHE: {
                    parseLocalCache(reader, containerAddress, operations);
                    break;
                }
                case INVALIDATION_CACHE: {
                    parseInvalidationCache(reader, containerAddress, operations);
                    break;
                }
                case REPLICATED_CACHE: {
                    parseReplicatedCache(reader, containerAddress, operations);
                    break;
                }
                case DISTRIBUTED_CACHE: {
                    parseDistributedCache(reader, containerAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseTransport(XMLExtendedStreamReader reader, ModelNode container) throws XMLStreamException {

        ModelNode transport = new ModelNode() ;
        transport.setEmptyObject();

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

        container.get(ModelKeys.TRANSPORT).set(transport) ;
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

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache, Configuration.CacheMode cacheMode) throws XMLStreamException {
        switch (attribute) {
            case MODE: {
                /*
                // move MODE processing into ADD handlers
                // it is now based on cache type (based on path address) and so
                // must apply to both CLI and parsing operation ModelNodes
                try {
                    Mode mode = Mode.valueOf(value);
                    cache.get(ModelKeys.MODE).set(mode.apply(cacheMode).name());
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                */
                cache.get(ModelKeys.MODE).set(value);
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

    private void parseLocalCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

        // set the cache mode to local
        // cache.get(ModelKeys.MODE).set(Configuration.CacheMode.LOCAL.name());

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

        String name = cache.get(ModelKeys.NAME).asString();
        // setup the cache address
        ModelNode cacheAddress = containerAddress.clone() ;
        cacheAddress.add(ModelKeys.LOCAL_CACHE, name);
        cacheAddress.protect() ;
        cache.get(ModelDescriptionConstants.OP_ADDR).set(cacheAddress);

        // get rid of NAME now that we are finished with it
        cache.remove(ModelKeys.NAME);

        operations.add(cache);
    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

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
                    this.parseClusteredCacheAttribute(reader, i, attribute, value, cache, Configuration.CacheMode.DIST_SYNC);
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

        String name = cache.get(ModelKeys.NAME).asString();
        // setup the cache address
        ModelNode cacheAddress = containerAddress.clone() ;
        cacheAddress.add(ModelKeys.DISTRIBUTED_CACHE, name);
        cacheAddress.protect() ;
        cache.get(ModelDescriptionConstants.OP_ADDR).set(cacheAddress);

        // get rid of NAME now that we are finished with it
        cache.remove(ModelKeys.NAME);

        operations.add(cache);
    }

    private void parseReplicatedCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache, Configuration.CacheMode.REPL_SYNC);
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
        String name = cache.get(ModelKeys.NAME).asString();
        // setup the cache address
        ModelNode cacheAddress = containerAddress.clone() ;
        cacheAddress.add(ModelKeys.REPLICATED_CACHE, name);
        cacheAddress.protect() ;
        cache.get(ModelDescriptionConstants.OP_ADDR).set(cacheAddress);

        // get rid of NAME now that we are finished with it
        cache.remove(ModelKeys.NAME);

        operations.add(cache);
    }

    private void parseInvalidationCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache, Configuration.CacheMode.INVALIDATION_SYNC);
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseCacheElement(reader, element, cache);
        }

        String name = cache.get(ModelKeys.NAME).asString();
        // setup the cache address
        ModelNode cacheAddress = containerAddress.clone() ;
        cacheAddress.add(ModelKeys.INVALIDATION_CACHE, name);
        cacheAddress.protect() ;
        cache.get(ModelDescriptionConstants.OP_ADDR).set(cacheAddress);

        // get rid of NAME now that we are finished with it
        cache.remove(ModelKeys.NAME);

        operations.add(cache);
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
                this.parseFileStore(reader, cache.get(ModelKeys.FILE_STORE).setEmptyObject());
                break;
            }
            case JDBC_STORE: {
                this.parseJDBCStore(reader, cache.get(ModelKeys.JDBC_STORE).setEmptyObject());
                break;
            }
            case REMOTE_STORE: {
                this.parseRemoteStore(reader, cache.get(ModelKeys.REMOTE_STORE).setEmptyObject());
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
                case WAIT: {
                    rehashing.get(ModelKeys.WAIT).set(Long.parseLong(value));
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
                        transaction.get(ModelKeys.MODE).set(TransactionMode.valueOf(value).name());
                    } catch (IllegalArgumentException e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case LOCKING: {
                    try {
                        transaction.get(ModelKeys.LOCKING).set(LockingMode.valueOf(value).name());
                    } catch (IllegalArgumentException e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case EAGER_LOCKING: {
                    ROOT_LOGGER.eagerAttributeDeprecated();
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
                    ROOT_LOGGER.deprecatedAttribute(attribute.getLocalName(), Element.EVICTION.getLocalName(), "ISPN-1268");
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseExpiration(XMLExtendedStreamReader reader, ModelNode expiration) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    expiration.get(ModelKeys.MAX_IDLE).set(Long.parseLong(value));
                    break;
                }
                case LIFESPAN: {
                    expiration.get(ModelKeys.LIFESPAN).set(Long.parseLong(value));
                    break;
                }
                case INTERVAL: {
                    expiration.get(ModelKeys.INTERVAL).set(Long.parseLong(value));
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

    private void parseRemoteStore(XMLExtendedStreamReader reader, ModelNode store) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    store.get(ModelKeys.CACHE).set(value);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    store.get(ModelKeys.SOCKET_TIMEOUT).set(Long.parseLong(value));
                    break;
                }
                case TCP_NO_DELAY: {
                    store.get(ModelKeys.TCP_NO_DELAY).set(Boolean.valueOf(value));
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_SERVER: {
                    this.parseRemoteServer(reader, store.get(ModelKeys.REMOTE_SERVER).add());
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store);
                }
            }
        }

        if (!store.hasDefined(ModelKeys.REMOTE_SERVER)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Element.REMOTE_SERVER));
        }
    }

    private void parseRemoteServer(XMLExtendedStreamReader reader, ModelNode server) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUND_SOCKET_BINDING: {
                    server.get(ModelKeys.OUTBOUND_SOCKET_BINDING).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseJDBCStore(XMLExtendedStreamReader reader, ModelNode store) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    store.get(ModelKeys.DATASOURCE).set(value);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        if (!store.hasDefined(ModelKeys.DATASOURCE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.DATASOURCE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ENTRY_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.ENTRY_TABLE).setEmptyObject());
                    break;
                }
                case BUCKET_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.BUCKET_TABLE).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store);
                }
            }
        }
    }

    private void parseJDBCStoreTable(XMLExtendedStreamReader reader, ModelNode table) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    table.get(ModelKeys.PREFIX).set(value);
                    break;
                }
                case FETCH_SIZE: {
                    table.get(ModelKeys.FETCH_SIZE).set(Integer.parseInt(value));
                    break;
                }
                case BATCH_SIZE: {
                    table.get(ModelKeys.BATCH_SIZE).set(Integer.parseInt(value));
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ID_COLUMN: {
                    this.parseJDBCStoreColumn(reader, table.get(ModelKeys.ID_COLUMN).setEmptyObject());
                    break;
                }
                case DATA_COLUMN: {
                    this.parseJDBCStoreColumn(reader, table.get(ModelKeys.DATA_COLUMN).setEmptyObject());
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    this.parseJDBCStoreColumn(reader, table.get(ModelKeys.TIMESTAMP_COLUMN).setEmptyObject());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseJDBCStoreColumn(XMLExtendedStreamReader reader, ModelNode column) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    column.get(ModelKeys.NAME).set(value);
                    break;
                }
                case TYPE: {
                    column.get(ModelKeys.TYPE).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
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
            this.parseStoreProperty(reader, node);
        }
    }

    private void parseStoreProperty(XMLExtendedStreamReader reader, ModelNode node) throws XMLStreamException {
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

                String containerName = entry.getName();
                ModelNode container = entry.getValue();

                writer.writeStartElement(Element.CACHE_CONTAINER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), containerName);
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


                // create a list of all caches in the model
                List<Property> cachesPL = new ArrayList<Property>();
                String[] cacheTypes = {ModelKeys.LOCAL_CACHE, ModelKeys.INVALIDATION_CACHE, ModelKeys.REPLICATED_CACHE, ModelKeys.DISTRIBUTED_CACHE};
                for (String cacheType : cacheTypes) {
                    List<Property> cachesOfAType = getCachesAsPropertyList(model, containerName, cacheType);
                    if (cachesOfAType != null)
                        cachesPL.addAll(cachesOfAType);
                }

                List<ModelNode> caches = new ArrayList<ModelNode>();
                for (Property cacheEntry : cachesPL) {
                    caches.add(cacheEntry.getValue());
                }

                // for (ModelNode cache: container.get(ModelKeys.CACHE).asList()) {
                for (ModelNode cache: caches) {
                    Configuration.CacheMode mode = Configuration.CacheMode.valueOf(cache.get(ModelKeys.CACHE_MODE).asString());
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
                        this.writeOptional(writer, Attribute.LOCKING, transaction, ModelKeys.LOCKING);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.EVICTION)) {
                        writer.writeStartElement(Element.EVICTION.getLocalName());
                        ModelNode eviction = cache.get(ModelKeys.EVICTION);
                        this.writeOptional(writer, Attribute.STRATEGY, eviction, ModelKeys.STRATEGY);
                        this.writeOptional(writer, Attribute.MAX_ENTRIES, eviction, ModelKeys.MAX_ENTRIES);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.EXPIRATION)) {
                        writer.writeStartElement(Element.EXPIRATION.getLocalName());
                        ModelNode expiration = cache.get(ModelKeys.EXPIRATION);
                        this.writeOptional(writer, Attribute.MAX_IDLE, expiration, ModelKeys.MAX_IDLE);
                        this.writeOptional(writer, Attribute.LIFESPAN, expiration, ModelKeys.LIFESPAN);
                        this.writeOptional(writer, Attribute.INTERVAL, expiration, ModelKeys.INTERVAL);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.STORE)) {
                        ModelNode store = cache.get(ModelKeys.STORE);
                        writer.writeStartElement(Element.STORE.getLocalName());
                        this.writeRequired(writer, Attribute.CLASS, store, ModelKeys.CLASS);
                        this.writeStoreAttributes(writer, store);
                        this.writeStoreProperties(writer, store);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.FILE_STORE)) {
                        ModelNode store = cache.get(ModelKeys.FILE_STORE);
                        writer.writeStartElement(Element.FILE_STORE.getLocalName());
                        this.writeOptional(writer, Attribute.RELATIVE_TO, store, ModelKeys.RELATIVE_TO);
                        this.writeOptional(writer, Attribute.PATH, store, ModelKeys.PATH);
                        this.writeStoreAttributes(writer, store);
                        this.writeStoreProperties(writer, store);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.JDBC_STORE)) {
                        ModelNode store = cache.get(ModelKeys.JDBC_STORE);
                        writer.writeStartElement(Element.JDBC_STORE.getLocalName());
                        this.writeRequired(writer, Attribute.DATASOURCE, store, ModelKeys.DATASOURCE);
                        this.writeStoreAttributes(writer, store);
                        this.writeJDBCStoreTable(writer, Element.ENTRY_TABLE, store, ModelKeys.ENTRY_TABLE);
                        this.writeJDBCStoreTable(writer, Element.BUCKET_TABLE, store, ModelKeys.BUCKET_TABLE);
                        this.writeStoreProperties(writer, store);
                        writer.writeEndElement();
                    }

                    if (cache.hasDefined(ModelKeys.REMOTE_STORE)) {
                        ModelNode store = cache.get(ModelKeys.REMOTE_STORE);
                        writer.writeStartElement(Element.REMOTE_STORE.getLocalName());
                        this.writeOptional(writer, Attribute.CACHE, store, ModelKeys.CACHE);
                        this.writeOptional(writer, Attribute.SOCKET_TIMEOUT, store, ModelKeys.SOCKET_TIMEOUT);
                        this.writeOptional(writer, Attribute.TCP_NO_DELAY, store, ModelKeys.TCP_NO_DELAY);
                        this.writeStoreAttributes(writer, store);
                        for (ModelNode remoteServer : store.get(ModelKeys.REMOTE_SERVER).asList()) {
                            writer.writeStartElement(Element.REMOTE_SERVER.getLocalName());
                            writer.writeAttribute(Attribute.OUTBOUND_SOCKET_BINDING.getLocalName(), remoteServer.get(ModelKeys.OUTBOUND_SOCKET_BINDING).asString());
                            writer.writeEndElement();
                        }
                        this.writeStoreProperties(writer, store);
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
                        this.writeOptional(writer, Attribute.WAIT, rehashing, ModelKeys.WAIT);
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    /**
     * Obtains a list of all caches in the model of a particular type, as a property list.
     *
     * @param model
     * @param containerName
     * @param cacheType  LOCAL_CACHE, INVALIDATION_CACHE, REPLICATED_CACHE, DISTRIBUTED_CACHE
     * @return
     */
    public static List<Property> getCachesAsPropertyList(ModelNode model, String containerName, String cacheType) {
        ModelNode caches = model.get(ModelKeys.CACHE_CONTAINER, containerName, cacheType);
        if (caches.isDefined() && caches.getType() == ModelType.OBJECT) {
            return caches.asPropertyList();
        }
        return null ;
    }

    private void writeJDBCStoreTable(XMLExtendedStreamWriter writer, Element element, ModelNode store, String key) throws XMLStreamException {
        if (store.hasDefined(key)) {
            ModelNode table = store.get(key);
            writer.writeStartElement(element.getLocalName());
            this.writeOptional(writer, Attribute.PREFIX, table, ModelKeys.PREFIX);
            this.writeOptional(writer, Attribute.BATCH_SIZE, table, ModelKeys.BATCH_SIZE);
            this.writeOptional(writer, Attribute.FETCH_SIZE, table, ModelKeys.FETCH_SIZE);
            this.writeJDBCStoreColumn(writer, Element.ID_COLUMN, table, ModelKeys.ID_COLUMN);
            this.writeJDBCStoreColumn(writer, Element.DATA_COLUMN, table, ModelKeys.DATA_COLUMN);
            this.writeJDBCStoreColumn(writer, Element.TIMESTAMP_COLUMN, table, ModelKeys.TIMESTAMP_COLUMN);
            writer.writeEndElement();
        }
    }

    private void writeJDBCStoreColumn(XMLExtendedStreamWriter writer, Element element, ModelNode table, String key) throws XMLStreamException {
        if (table.hasDefined(key)) {
            ModelNode column = table.get(key);
            writer.writeStartElement(element.getLocalName());
            this.writeOptional(writer, Attribute.NAME, column, ModelKeys.NAME);
            this.writeOptional(writer, Attribute.TYPE, column, ModelKeys.TYPE);
            writer.writeEndElement();
        }
    }

    private void writeStoreAttributes(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        this.writeOptional(writer, Attribute.SHARED, store, ModelKeys.SHARED);
        this.writeOptional(writer, Attribute.PRELOAD, store, ModelKeys.PRELOAD);
        this.writeOptional(writer, Attribute.PASSIVATION, store, ModelKeys.PASSIVATION);
        this.writeOptional(writer, Attribute.FETCH_STATE, store, ModelKeys.FETCH_STATE);
        this.writeOptional(writer, Attribute.PURGE, store, ModelKeys.PURGE);
        this.writeOptional(writer, Attribute.SINGLETON, store, ModelKeys.SINGLETON);
    }

    private void writeStoreProperties(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(ModelKeys.PROPERTY)) {
            for (Property property: store.get(ModelKeys.PROPERTY).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                writer.writeCharacters(property.getValue().asString());
                writer.writeEndElement();
            }
        }
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
