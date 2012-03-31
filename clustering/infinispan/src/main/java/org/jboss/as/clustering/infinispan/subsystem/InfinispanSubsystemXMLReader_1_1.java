package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Infinispan subsystem parsing code.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Tristan Tarrant
 */
public class InfinispanSubsystemXMLReader_1_1 implements XMLElementReader<List<ModelNode>> {

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode subsystemAddress = new ModelNode();
        subsystemAddress.add(ModelDescriptionConstants.SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME);
        subsystemAddress.protect();

        ModelNode subsystem = Util.getEmptyOperation(ModelDescriptionConstants.ADD, subsystemAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_CACHE_CONTAINER: {
                    // Ignore
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        // command to add the subsystem
        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
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
                case ALIASES: {
                    for (String alias: reader.getListAttributeValue(i)) {
                        container.get(ModelKeys.ALIASES).add(alias);
                    }
                    break;
                }
                case DEFAULT_CACHE: {
                    CommonAttributes.DEFAULT_CACHE.parseAndSetParameter(value, container, reader);
                    break;
                }
                case JNDI_NAME: {
                    CommonAttributes.JNDI_NAME.parseAndSetParameter(value, container, reader);
                    break;
                }
                case START: {
                    CommonAttributes.START.parseAndSetParameter(value, container, reader);
                    break;
                }
                case LISTENER_EXECUTOR: {
                    CommonAttributes.LISTENER_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case EVICTION_EXECUTOR: {
                    CommonAttributes.EVICTION_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case REPLICATION_QUEUE_EXECUTOR: {
                    CommonAttributes.REPLICATION_QUEUE_EXECUTOR.parseAndSetParameter(value, container, reader);
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

        // Backwards compatible default module
        CommonAttributes.CACHE_CONTAINER_MODULE.parseAndSetParameter("org.jboss.as.jpa.hibernate:4", container, reader);

        ModelNode containerAddress = subsystemAddress.clone() ;
        containerAddress.add(ModelKeys.CACHE_CONTAINER, name);
        containerAddress.protect() ;
        container.get(ModelDescriptionConstants.OP_ADDR).set(containerAddress);

        // operation to add the container
        operations.add(container);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case TRANSPORT: {
                    parseTransport(reader, containerAddress, operations);
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

    private void parseTransport(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the transport add operation
        ModelNode transport = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STACK: {
                    CommonAttributes.STACK.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case EXECUTOR: {
                    CommonAttributes.EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case LOCK_TIMEOUT: {
                    CommonAttributes.LOCK_TIMEOUT.parseAndSetParameter(value, transport, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);

        // setup the transport address
        ModelNode transportAddress = containerAddress.clone() ;
        transportAddress.add(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
        transportAddress.protect() ;
        transport.get(ModelDescriptionConstants.OP_ADDR).set(transportAddress);

        operations.add(transport);
    }

    private void parseCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache) throws XMLStreamException {
        switch (attribute) {
            case NAME: {
                CommonAttributes.NAME.parseAndSetParameter(value, cache, reader);
                break;
            }
            case START: {
                CommonAttributes.START.parseAndSetParameter(value, cache, reader);
                break;
            }
            case JNDI_NAME: {
                CommonAttributes.JNDI_NAME.parseAndSetParameter(value, cache, reader);
                break;
            }
            case BATCHING: {
                CommonAttributes.BATCHING.parseAndSetParameter(value, cache, reader);
                break;
            }
            case INDEXING: {
                CommonAttributes.INDEXING.parseAndSetParameter(value, cache, reader);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache) throws XMLStreamException {
        switch (attribute) {
            case MODE: {
                // note the use of ClusteredCacheAdd.MODE
                ClusteredCacheAdd.MODE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case QUEUE_SIZE: {
                CommonAttributes.QUEUE_SIZE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case QUEUE_FLUSH_INTERVAL: {
                CommonAttributes.QUEUE_FLUSH_INTERVAL.parseAndSetParameter(value, cache, reader);
                break;
            }
            case REMOTE_TIMEOUT: {
                CommonAttributes.REMOTE_TIMEOUT.parseAndSetParameter(value, cache, reader);
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
        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

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

        // update the cache address with the cache name
        addCacheNameToAddress(cache, containerAddress, ModelKeys.LOCAL_CACHE) ;

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseCacheElement(reader, element, cache, additionalConfigurationOperations);
        }

        operations.add(cache);
        // add operations to create configuration resources
        for (ModelNode additionalOperation : additionalConfigurationOperations) {
            operations.add(additionalOperation);
        }

    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    CommonAttributes.OWNERS.parseAndSetParameter(value, cache, reader);
                    break;
                }
                case VIRTUAL_NODES: {
                    CommonAttributes.VIRTUAL_NODES.parseAndSetParameter(value, cache, reader);
                    break;
                }
                case L1_LIFESPAN: {
                    CommonAttributes.L1_LIFESPAN.parseAndSetParameter(value, cache, reader);
                    break;
                }
                default: {
                    this.parseClusteredCacheAttribute(reader, i, attribute, value, cache);
                }
            }
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }
        if (!cache.hasDefined(ModelKeys.MODE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        // update the cache address with the cache name
        addCacheNameToAddress(cache, containerAddress, ModelKeys.DISTRIBUTED_CACHE) ;

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STATE_TRANSFER: {
                    this.parseStateTransfer(reader, cache, additionalConfigurationOperations);
                    break;
                }
                default: {
                    this.parseCacheElement(reader, element, cache, additionalConfigurationOperations);
                }
            }
        }

        operations.add(cache);
        // add operations to create configuration resources
        for (ModelNode additionalOperation : additionalConfigurationOperations) {
            operations.add(additionalOperation);
        }
    }

    private void parseReplicatedCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache);
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }
        if (!cache.hasDefined(ModelKeys.MODE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        // update the cache address with the cache name
        addCacheNameToAddress(cache, containerAddress, ModelKeys.REPLICATED_CACHE) ;

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STATE_TRANSFER: {
                    this.parseStateTransfer(reader, cache, additionalConfigurationOperations);
                    break;
                }
                default: {
                    this.parseCacheElement(reader, element, cache, additionalConfigurationOperations);
                }
            }
        }

        operations.add(cache);
        // add operations to create configuration resources
        for (ModelNode additionalOperation : additionalConfigurationOperations) {
            operations.add(additionalOperation);
        }
    }

    private void parseInvalidationCache(XMLExtendedStreamReader reader, ModelNode containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache);
        }

        if (!cache.hasDefined(ModelKeys.NAME)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }
        if (!cache.hasDefined(ModelKeys.MODE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        // update the cache address with the cache name
        addCacheNameToAddress(cache, containerAddress, ModelKeys.INVALIDATION_CACHE) ;

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                default: {
                    this.parseCacheElement(reader, element, cache, additionalConfigurationOperations);
                }
            }
        }

        operations.add(cache);
        // add operations to create configuration resources
        for (ModelNode additionalOperation : additionalConfigurationOperations) {
            operations.add(additionalOperation);
        }
    }

    private void addCacheNameToAddress(ModelNode cache, ModelNode containerAddress, String cacheType) {

        String name = cache.get(ModelKeys.NAME).asString();
        // setup the cache address
        ModelNode cacheAddress = containerAddress.clone() ;
        cacheAddress.add(cacheType, name);
        cacheAddress.protect() ;
        cache.get(ModelDescriptionConstants.OP_ADDR).set(cacheAddress);

        // get rid of NAME now that we are finished with it
        cache.remove(ModelKeys.NAME);
    }


    @SuppressWarnings("deprecation")
    private void parseCacheElement(XMLExtendedStreamReader reader, Element element, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        switch (element) {
            case LOCKING: {
                this.parseLocking(reader, cache, operations);
                break;
            }
            case TRANSACTION: {
                this.parseTransaction(reader, cache, operations);
                break;
            }
            case EVICTION: {
                this.parseEviction(reader, cache, operations);
                break;
            }
            case EXPIRATION: {
                this.parseExpiration(reader, cache, operations);
                break;
            }
            case STORE: {
                this.parseCustomStore(reader, cache, operations);
                break;
            }
            case FILE_STORE: {
                this.parseFileStore(reader, cache, operations);
                break;
            }
            case JDBC_STORE: {
                this.parseJDBCStore(reader, cache, operations);
                break;
            }
            case REMOTE_STORE: {
                this.parseRemoteStore(reader, cache, operations);
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseStateTransfer(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        // ModelNode for the state transfer add operation
        ModelNode stateTransferAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone();
        stateTransferAddress.add(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);
        stateTransferAddress.protect();
        ModelNode stateTransfer = Util.getEmptyOperation(ModelDescriptionConstants.ADD, stateTransferAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    CommonAttributes.ENABLED.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                case TIMEOUT: {
                    CommonAttributes.TIMEOUT.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                case CHUNK_SIZE: {
                    CommonAttributes.CHUNK_SIZE.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        operations.add(stateTransfer);
    }

    private void parseLocking(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode lockingAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone();
        lockingAddress.add(ModelKeys.LOCKING,ModelKeys.LOCKING_NAME);
        lockingAddress.protect();
        ModelNode locking = Util.getEmptyOperation(ModelDescriptionConstants.ADD, lockingAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    CommonAttributes.ISOLATION.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case STRIPING: {
                    CommonAttributes.STRIPING.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    CommonAttributes.ACQUIRE_TIMEOUT.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    CommonAttributes.CONCURRENCY_LEVEL.parseAndSetParameter(value, locking, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        operations.add(locking);
    }

    private void parseTransaction(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the transaction add operation
        ModelNode transactionAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone();
        transactionAddress.add(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);
        transactionAddress.protect();
        ModelNode transaction = Util.getEmptyOperation(ModelDescriptionConstants.ADD, transactionAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    CommonAttributes.STOP_TIMEOUT.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                case MODE: {
                    CommonAttributes.MODE.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                case LOCKING: {
                    CommonAttributes.LOCKING.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        operations.add(transaction);
    }

    private void parseEviction(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        // ModelNode for the eviction add operation
        ModelNode evictionAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone();
        evictionAddress.add(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);
        evictionAddress.protect();
        ModelNode eviction = Util.getEmptyOperation(ModelDescriptionConstants.ADD, evictionAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STRATEGY: {
                    CommonAttributes.EVICTION_STRATEGY.parseAndSetParameter(value, eviction, reader);
                    break;
                }
                case MAX_ENTRIES: {
                    CommonAttributes.MAX_ENTRIES.parseAndSetParameter(value, eviction, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        operations.add(eviction);
    }

    private void parseExpiration(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the expiration add operation
        ModelNode expirationAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone();
        expirationAddress.add(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);
        expirationAddress.protect();
        ModelNode expiration = Util.getEmptyOperation(ModelDescriptionConstants.ADD, expirationAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    CommonAttributes.MAX_IDLE.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                case LIFESPAN: {
                    CommonAttributes.LIFESPAN.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                case INTERVAL: {
                    CommonAttributes.INTERVAL.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        operations.add(expiration);
    }

    private void parseCustomStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        // ModelNode for the store add operation
        ModelNode storeAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone() ;
        storeAddress.add(ModelKeys.STORE,ModelKeys.STORE_NAME) ;
        storeAddress.protect();
        ModelNode store = Util.getEmptyOperation(ModelDescriptionConstants.ADD, storeAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    CommonAttributes.CLASS.parseAndSetParameter(value, store, reader);
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
        operations.add(store);
    }

    private void parseFileStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        // ModelNode for the file store add operation
        ModelNode storeAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone() ;
        storeAddress.add(ModelKeys.FILE_STORE,ModelKeys.FILE_STORE_NAME) ;
        storeAddress.protect();
        ModelNode store = Util.getEmptyOperation(ModelDescriptionConstants.ADD, storeAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    CommonAttributes.RELATIVE_TO.parseAndSetParameter(value, store, reader);
                    break;
                }
                case PATH: {
                    CommonAttributes.PATH.parseAndSetParameter(value, store, reader);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        this.parseStoreProperties(reader, store);
        operations.add(store);
    }

    private void parseRemoteStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        // ModelNode for the remote store add operation
        ModelNode storeAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone() ;
        storeAddress.add(ModelKeys.REMOTE_STORE,ModelKeys.REMOTE_STORE_NAME) ;
        storeAddress.protect();
        ModelNode store = Util.getEmptyOperation(ModelDescriptionConstants.ADD, storeAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    CommonAttributes.CACHE.parseAndSetParameter(value, store, reader);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    CommonAttributes.SOCKET_TIMEOUT.parseAndSetParameter(value, store, reader);
                    break;
                }
                case TCP_NO_DELAY: {
                    CommonAttributes.TCP_NO_DELAY.parseAndSetParameter(value, store, reader);
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
                    this.parseRemoteServer(reader, store.get(ModelKeys.REMOTE_SERVERS).add());
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store);
                }
            }
        }

        if (!store.hasDefined(ModelKeys.REMOTE_SERVERS)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Element.REMOTE_SERVER));
        }
        operations.add(store);
    }

    private void parseRemoteServer(XMLExtendedStreamReader reader, ModelNode server) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUND_SOCKET_BINDING: {
                    CommonAttributes.OUTBOUND_SOCKET_BINDING.parseAndSetParameter(value, server, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    @SuppressWarnings("deprecation")
    private void parseJDBCStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        // ModelNode for the jdbc store add operation
        // this needs to create a management command for the 1.2 schema
        ModelNode storeAddress = cache.get(ModelDescriptionConstants.OP_ADDR).clone() ;
        // we can't determine the full address until we know which tables are present
        ModelNode store = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    CommonAttributes.DATA_SOURCE.parseAndSetParameter(value, store, reader);
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
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.STRING_KEYED_TABLE).setEmptyObject());
                    break;
                }
                case BUCKET_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.BINARY_KEYED_TABLE).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store);
                }
            }
        }

        // we determine the store address by the presence / absence of tables
        boolean isStringTableDefined = store.get(ModelKeys.STRING_KEYED_TABLE).isDefined();
        boolean isBinaryTableDefined = store.get(ModelKeys.BINARY_KEYED_TABLE).isDefined();

        // if no tables are defined, we default to mixed store
        if (isStringTableDefined && !isBinaryTableDefined) {
            storeAddress.add(ModelKeys.STRING_KEYED_JDBC_STORE,ModelKeys.STRING_KEYED_JDBC_STORE_NAME) ;
            storeAddress.protect();
        } else if (!isStringTableDefined && isBinaryTableDefined) {
            storeAddress.add(ModelKeys.BINARY_KEYED_JDBC_STORE,ModelKeys.BINARY_KEYED_JDBC_STORE_NAME) ;
            storeAddress.protect();
        } else {
            storeAddress.add(ModelKeys.MIXED_KEYED_JDBC_STORE,ModelKeys.MIXED_KEYED_JDBC_STORE_NAME) ;
            storeAddress.protect();
        }
        store.get(OP_ADDR).set(storeAddress);
        operations.add(store);
    }

    private void parseJDBCStoreTable(XMLExtendedStreamReader reader, ModelNode table) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    CommonAttributes.PREFIX.parseAndSetParameter(value, table, reader);
                    break;
                }
                case FETCH_SIZE: {
                    CommonAttributes.FETCH_SIZE.parseAndSetParameter(value, table, reader);
                    break;
                }
                case BATCH_SIZE: {
                    CommonAttributes.BATCH_SIZE.parseAndSetParameter(value, table, reader);
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
                CommonAttributes.SHARED.parseAndSetParameter(value, store, reader);
                break;
            }
            case PRELOAD: {
                CommonAttributes.PRELOAD.parseAndSetParameter(value, store, reader);
                break;
            }
            case PASSIVATION: {
                CommonAttributes.PASSIVATION.parseAndSetParameter(value, store, reader);
                break;
            }
            case FETCH_STATE: {
                CommonAttributes.FETCH_STATE.parseAndSetParameter(value, store, reader);
                break;
            }
            case PURGE: {
                CommonAttributes.PURGE.parseAndSetParameter(value, store, reader);
                break;
            }
            case SINGLETON: {
                CommonAttributes.SINGLETON.parseAndSetParameter(value, store, reader);
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
                node.get(ModelKeys.PROPERTIES).add(property, value);
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }
}
