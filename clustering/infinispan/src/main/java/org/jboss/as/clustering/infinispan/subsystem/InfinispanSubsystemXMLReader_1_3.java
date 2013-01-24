/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
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
public final class InfinispanSubsystemXMLReader_1_3 implements XMLElementReader<List<ModelNode>> {

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        PathAddress subsystemAddress = PathAddress.pathAddress(InfinispanExtension.SUBSYSTEM_PATH);
        ModelNode subsystem = Util.createAddOperation(subsystemAddress);

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

    private void parseContainer(XMLExtendedStreamReader reader, PathAddress subsystemAddress, List<ModelNode> operations) throws XMLStreamException {

        ModelNode container = Util.getEmptyOperation(ADD, null);
        String name = null;
        final Set<Attribute> required = EnumSet.of(Attribute.NAME);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            required.remove(attribute);
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
                    CacheContainerResource.DEFAULT_CACHE.parseAndSetParameter(value, container, reader);
                    break;
                }
                case JNDI_NAME: {
                    CacheContainerResource.JNDI_NAME.parseAndSetParameter(value, container, reader);
                    break;
                }
                case START: {
                    CacheContainerResource.START.parseAndSetParameter(value, container, reader);
                    break;
                }
                case LISTENER_EXECUTOR: {
                    CacheContainerResource.LISTENER_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case EVICTION_EXECUTOR: {
                    CacheContainerResource.EVICTION_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case REPLICATION_QUEUE_EXECUTOR: {
                    CacheContainerResource.REPLICATION_QUEUE_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case MODULE: {
                    CacheContainerResource.CACHE_CONTAINER_MODULE.parseAndSetParameter(value, container, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!required.isEmpty()) {
            throw ParseUtils.missingRequired(reader, required);
        }

        PathAddress containerAddress = subsystemAddress.append(ModelKeys.CACHE_CONTAINER, name);
        container.get(OP_ADDR).set(containerAddress.toModelNode());

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

    private void parseTransport(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress transportAddress = containerAddress.append(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
        ModelNode transport = Util.createAddOperation(transportAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STACK: {
                    TransportResource.STACK.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case CLUSTER: {
                    TransportResource.CLUSTER.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case EXECUTOR: {
                    TransportResource.EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case LOCK_TIMEOUT: {
                    TransportResource.LOCK_TIMEOUT.parseAndSetParameter(value, transport, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);

        operations.add(transport);
    }

    private void parseCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache) throws XMLStreamException {
        switch (attribute) {
            case NAME: {
                CacheResource.NAME.parseAndSetParameter(value, cache, reader);
                break;
            }
            case START: {
                CacheResource.START.parseAndSetParameter(value, cache, reader);
                break;
            }
            case JNDI_NAME: {
                CacheResource.JNDI_NAME.parseAndSetParameter(value, cache, reader);
                break;
            }
            case BATCHING: {
                CacheResource.BATCHING.parseAndSetParameter(value, cache, reader);
                break;
            }
            case INDEXING: {
                CacheResource.INDEXING.parseAndSetParameter(value, cache, reader);
                break;
            }
            case MODULE: {
                CacheResource.CACHE_MODULE.parseAndSetParameter(value, cache, reader);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute, String value, ModelNode cache) throws XMLStreamException {
        switch (attribute) {
            case ASYNC_MARSHALLING: {
                ClusteredCacheResource.ASYNC_MARSHALLING.parseAndSetParameter(value, cache, reader);
                break;
            }
            case MODE: {
                // note the use of ClusteredCacheAdd.MODE
                ClusteredCacheResource.MODE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case QUEUE_SIZE: {
                ClusteredCacheResource.QUEUE_SIZE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case QUEUE_FLUSH_INTERVAL: {
                ClusteredCacheResource.QUEUE_FLUSH_INTERVAL.parseAndSetParameter(value, cache, reader);
                break;
            }
            case REMOTE_TIMEOUT: {
                ClusteredCacheResource.REMOTE_TIMEOUT.parseAndSetParameter(value, cache, reader);
                break;
            }
            default: {
                this.parseCacheAttribute(reader, index, attribute, value, cache);
            }
        }
    }

    private void parseLocalCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ADD, null);
        // NOTE: this list is used to avoid lost attribute updates to the cache
        // object once it has been added to the operations list
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

    private void parseDistributedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        // ModelNode for the cache add operation
        ModelNode cache = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    DistributedCacheResource.OWNERS.parseAndSetParameter(value, cache, reader);
                    break;
                }
                case VIRTUAL_NODES: {
                    // AS7-5753: convert any non-expression virtual nodes value to a segments value,
                    DistributedCacheResource.SEGMENTS.parseAndSetParameter(
                            InfinispanResourceAndOperationTransformer_1_3.virtualNodesToSegments(value), cache, reader);
                    break;
                }
                case L1_LIFESPAN: {
                    DistributedCacheResource.L1_LIFESPAN.parseAndSetParameter(value, cache, reader);
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

    private void parseReplicatedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

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

    private void parseInvalidationCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

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

    private void addCacheNameToAddress(ModelNode cache, PathAddress containerAddress, String cacheType) {

        String name = cache.get(ModelKeys.NAME).asString();
        // setup the cache address
        PathAddress cacheAddress = containerAddress.append(cacheType, name) ;
        cache.get(ModelDescriptionConstants.OP_ADDR).set(cacheAddress.toModelNode());

        // get rid of NAME now that we are finished with it
        cache.remove(ModelKeys.NAME);
    }


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
            case STRING_KEYED_JDBC_STORE: {
                this.parseStringKeyedJDBCStore(reader, cache, operations);
                break;
            }
            case BINARY_KEYED_JDBC_STORE: {
                this.parseBinaryKeyedJDBCStore(reader, cache, operations);
                break;
            }
            case MIXED_KEYED_JDBC_STORE: {
                this.parseMixedKeyedJDBCStore(reader, cache, operations);
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

        PathAddress stateTransferAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);
        ModelNode stateTransfer = Util.createAddOperation(stateTransferAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    StateTransferResource.ENABLED.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                case TIMEOUT: {
                    StateTransferResource.TIMEOUT.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                case CHUNK_SIZE: {
                    StateTransferResource.CHUNK_SIZE.parseAndSetParameter(value, stateTransfer, reader);
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

        PathAddress lockingAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);
        ModelNode locking = Util.createAddOperation(lockingAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    LockingResource.ISOLATION.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case STRIPING: {
                    LockingResource.STRIPING.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    LockingResource.ACQUIRE_TIMEOUT.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    LockingResource.CONCURRENCY_LEVEL.parseAndSetParameter(value, locking, reader);
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

        PathAddress transactionAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);
        ModelNode transaction = Util.createAddOperation(transactionAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    TransactionResource.STOP_TIMEOUT.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                case MODE: {
                    TransactionResource.MODE.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                case LOCKING: {
                    TransactionResource.LOCKING.parseAndSetParameter(value, transaction, reader);
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

        PathAddress evictionAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);
        ModelNode eviction = Util.createAddOperation(evictionAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STRATEGY: {
                    EvictionResource.EVICTION_STRATEGY.parseAndSetParameter(value, eviction, reader);
                    break;
                }
                case MAX_ENTRIES: {
                    EvictionResource.MAX_ENTRIES.parseAndSetParameter(value, eviction, reader);
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

        PathAddress expirationAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);
        ModelNode expiration = Util.createAddOperation(expirationAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    ExpirationResource.MAX_IDLE.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                case LIFESPAN: {
                    ExpirationResource.LIFESPAN.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                case INTERVAL: {
                    ExpirationResource.INTERVAL.parseAndSetParameter(value, expiration, reader);
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

        PathAddress storeAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.STORE, ModelKeys.STORE_NAME);
        ModelNode store = Util.createAddOperation(storeAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    StoreResource.CLASS.parseAndSetParameter(value, store, reader);
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

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();
        this.parseStoreElements(reader, store, additionalConfigurationOperations);
        operations.add(store);
        operations.addAll(additionalConfigurationOperations);
    }

    private void parseFileStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);
        ModelNode store = Util.createAddOperation(storeAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    FileStoreResource.RELATIVE_TO.parseAndSetParameter(value, store, reader);
                    break;
                }
                case PATH: {
                    FileStoreResource.PATH.parseAndSetParameter(value, store, reader);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();
        this.parseStoreElements(reader, store, additionalConfigurationOperations);
        operations.add(store);
        operations.addAll(additionalConfigurationOperations);
    }

    private void parseRemoteStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);
        ModelNode store = Util.createAddOperation(storeAddress);

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    RemoteStoreResource.CACHE.parseAndSetParameter(value, store, reader);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    RemoteStoreResource.SOCKET_TIMEOUT.parseAndSetParameter(value, store, reader);
                    break;
                }
                case TCP_NO_DELAY: {
                    RemoteStoreResource.TCP_NO_DELAY.parseAndSetParameter(value, store, reader);
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
                case WRITE_BEHIND: {
                    parseStoreWriteBehind(reader, store, additionalConfigurationOperations);
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store, additionalConfigurationOperations);
                }
            }
        }

        if (!store.hasDefined(ModelKeys.REMOTE_SERVERS)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Element.REMOTE_SERVER));
        }
        operations.add(store);
        operations.addAll(additionalConfigurationOperations);
    }

    private void parseRemoteServer(XMLExtendedStreamReader reader, ModelNode server) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUND_SOCKET_BINDING: {
                    RemoteStoreResource.OUTBOUND_SOCKET_BINDING.parseAndSetParameter(value, server, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseStringKeyedJDBCStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME);
        ModelNode store = Util.createAddOperation(storeAddress);

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    BaseJDBCStoreResource.DATA_SOURCE.parseAndSetParameter(value, store, reader);
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
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.STRING_KEYED_TABLE).setEmptyObject());
                    break;
                }
                case WRITE_BEHIND: {
                    parseStoreWriteBehind(reader, store, additionalConfigurationOperations);
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store, additionalConfigurationOperations);
                }
            }
        }
        operations.add(store);
        operations.addAll(additionalConfigurationOperations);
    }

    private void parseBinaryKeyedJDBCStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME);
        ModelNode store = Util.createAddOperation(storeAddress);

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    BaseJDBCStoreResource.DATA_SOURCE.parseAndSetParameter(value, store, reader);
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
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.BINARY_KEYED_TABLE).setEmptyObject());
                    break;
                }
                case WRITE_BEHIND: {
                    parseStoreWriteBehind(reader, store, additionalConfigurationOperations);
                    break;
                }
                default: {
                    this.parseStoreProperty(reader, store, additionalConfigurationOperations);
                }
            }
        }
        operations.add(store);
        operations.addAll(additionalConfigurationOperations);
    }

    private void parseMixedKeyedJDBCStore(XMLExtendedStreamReader reader, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = PathAddress.pathAddress(cache.get(OP_ADDR)).append(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME);
        ModelNode store = Util.createAddOperation(storeAddress);

        List<ModelNode> additionalConfigurationOperations = new ArrayList<ModelNode>();

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    BaseJDBCStoreResource.DATA_SOURCE.parseAndSetParameter(value, store, reader);
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
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.STRING_KEYED_TABLE).setEmptyObject());
                    break;
                }
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.BINARY_KEYED_TABLE).setEmptyObject());
                    break;
                }
                case WRITE_BEHIND: {
                    parseStoreWriteBehind(reader, store, additionalConfigurationOperations);
                    break;
                }
                case PROPERTY: {
                    parseStoreProperty(reader, store, additionalConfigurationOperations);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
        operations.add(store);
        operations.addAll(additionalConfigurationOperations);
    }


    private void parseJDBCStoreTable(XMLExtendedStreamReader reader, ModelNode table) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    BaseJDBCStoreResource.PREFIX.parseAndSetParameter(value, table, reader);
                    break;
                }
                case FETCH_SIZE: {
                    BaseJDBCStoreResource.FETCH_SIZE.parseAndSetParameter(value, table, reader);
                    break;
                }
                case BATCH_SIZE: {
                    BaseJDBCStoreResource.BATCH_SIZE.parseAndSetParameter(value, table, reader);
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
                    BaseJDBCStoreResource.COLUMN_NAME.parseAndSetParameter(value, column, reader);
                    break;
                }
                case TYPE: {
                    BaseJDBCStoreResource.COLUMN_TYPE.parseAndSetParameter(value, column, reader);
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
                BaseStoreResource.SHARED.parseAndSetParameter(value, store, reader);
                break;
            }
            case PRELOAD: {
                BaseStoreResource.PRELOAD.parseAndSetParameter(value, store, reader);
                break;
            }
            case PASSIVATION: {
                BaseStoreResource.PASSIVATION.parseAndSetParameter(value, store, reader);
                break;
            }
            case FETCH_STATE: {
                BaseStoreResource.FETCH_STATE.parseAndSetParameter(value, store, reader);
                break;
            }
            case PURGE: {
                BaseStoreResource.PURGE.parseAndSetParameter(value, store, reader);
                break;
            }
            case SINGLETON: {
                BaseStoreResource.SINGLETON.parseAndSetParameter(value, store, reader);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseStoreElements(XMLExtendedStreamReader reader, ModelNode store, List<ModelNode> operations) throws XMLStreamException {
        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case WRITE_BEHIND: {
                    parseStoreWriteBehind(reader, store, operations);
                    break;
                }
                case PROPERTY: {
                    parseStoreProperty(reader, store, operations);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseStoreWriteBehind(XMLExtendedStreamReader reader, ModelNode store, List<ModelNode> operations) throws XMLStreamException {

        PathAddress writeBehindAddress = PathAddress.pathAddress(store.get(OP_ADDR)).append(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME);
        ModelNode writeBehind = Util.createAddOperation(writeBehindAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FLUSH_LOCK_TIMEOUT: {
                    StoreWriteBehindResource.FLUSH_LOCK_TIMEOUT.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                case MODIFICATION_QUEUE_SIZE: {
                    StoreWriteBehindResource.MODIFICATION_QUEUE_SIZE.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                case SHUTDOWN_TIMEOUT: {
                    StoreWriteBehindResource.SHUTDOWN_TIMEOUT.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                case THREAD_POOL_SIZE: {
                    StoreWriteBehindResource.THREAD_POOL_SIZE.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
        operations.add(writeBehind);
    }

    private void parseStoreProperty(XMLExtendedStreamReader reader, ModelNode node, final List<ModelNode> operations) throws XMLStreamException {
        int attributes = reader.getAttributeCount();
        String propertyName = null;
        for (int i = 0; i < attributes; i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    propertyName = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (propertyName == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        String propertyValue = reader.getElementText();

        PathAddress propertyAddress = PathAddress.pathAddress(node.get(OP_ADDR)).append(ModelKeys.PROPERTY, propertyName);
        ModelNode property = Util.createAddOperation(propertyAddress);

        // represent the value as a ModelNode to cater for expressions
        StorePropertyResource.VALUE.parseAndSetParameter(propertyValue, property, reader);

        operations.add(property);
    }
}
