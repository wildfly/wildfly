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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Infinispan subsystem parsing code.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 * @author Tristan Tarrant
 * @author Radoslav Husar
 */
public final class InfinispanSubsystemXMLReader_2_0 implements XMLElementReader<List<ModelNode>> {

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        PathAddress subsystemAddress = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
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

        String name = require(reader, Attribute.NAME);
        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.pathElement(name));
        ModelNode container = Util.createAddOperation(containerAddress);
        operations.add(container);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already read
                    break;
                }
                case ALIASES: {
                    for (String alias: reader.getListAttributeValue(i)) {
                        container.get(ModelKeys.ALIASES).add(alias);
                    }
                    break;
                }
                case DEFAULT_CACHE: {
                    CacheContainerResourceDefinition.DEFAULT_CACHE.parseAndSetParameter(value, container, reader);
                    break;
                }
                case JNDI_NAME: {
                    CacheContainerResourceDefinition.JNDI_NAME.parseAndSetParameter(value, container, reader);
                    break;
                }
                case START: {
                    CacheContainerResourceDefinition.START.parseAndSetParameter(value, container, reader);
                    break;
                }
                case LISTENER_EXECUTOR: {
                    CacheContainerResourceDefinition.LISTENER_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case EVICTION_EXECUTOR: {
                    CacheContainerResourceDefinition.EVICTION_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case REPLICATION_QUEUE_EXECUTOR: {
                    CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.parseAndSetParameter(value, container, reader);
                    break;
                }
                case MODULE: {
                    CacheContainerResourceDefinition.MODULE.parseAndSetParameter(value, container, reader);
                    break;
                }
                case STATISTICS_ENABLED: {
                    CacheContainerResourceDefinition.STATISTICS_ENABLED.parseAndSetParameter(value, container, reader);
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
        operations.add(transport);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STACK: {
                    TransportResourceDefinition.STACK.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case CLUSTER: {
                    TransportResourceDefinition.CLUSTER.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case EXECUTOR: {
                    TransportResourceDefinition.EXECUTOR.parseAndSetParameter(value, transport, reader);
                    break;
                }
                case LOCK_TIMEOUT: {
                    TransportResourceDefinition.LOCK_TIMEOUT.parseAndSetParameter(value, transport, reader);
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
                // Already read
                break;
            }
            case START: {
                CacheResourceDefinition.START.parseAndSetParameter(value, cache, reader);
                break;
            }
            case JNDI_NAME: {
                CacheResourceDefinition.JNDI_NAME.parseAndSetParameter(value, cache, reader);
                break;
            }
            case BATCHING: {
                CacheResourceDefinition.BATCHING.parseAndSetParameter(value, cache, reader);
                break;
            }
            case MODULE: {
                CacheResourceDefinition.MODULE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case STATISTICS_ENABLED: {
                CacheResourceDefinition.STATISTICS_ENABLED.parseAndSetParameter(value, cache, reader);
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
                ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.parseAndSetParameter(value, cache, reader);
                break;
            }
            case MODE: {
                // note the use of ClusteredCacheAdd.MODE
                ClusteredCacheResourceDefinition.MODE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case QUEUE_SIZE: {
                ClusteredCacheResourceDefinition.QUEUE_SIZE.parseAndSetParameter(value, cache, reader);
                break;
            }
            case QUEUE_FLUSH_INTERVAL: {
                ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.parseAndSetParameter(value, cache, reader);
                break;
            }
            case REMOTE_TIMEOUT: {
                ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.parseAndSetParameter(value, cache, reader);
                break;
            }
            default: {
                this.parseCacheAttribute(reader, index, attribute, value, cache);
            }
        }
    }

    private void parseLocalCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress cacheAddress = containerAddress.append(LocalCacheResourceDefinition.pathElement(name));
        ModelNode cache = Util.createAddOperation(cacheAddress);
        operations.add(cache);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseCacheAttribute(reader, i, attribute, value, cache);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseCacheElement(reader, element, cacheAddress, cache, operations);
        }
    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress cacheAddress = containerAddress.append(DistributedCacheResourceDefinition.pathElement(name));
        ModelNode cache = Util.createAddOperation(cacheAddress);
        operations.add(cache);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    DistributedCacheResourceDefinition.OWNERS.parseAndSetParameter(value, cache, reader);
                    break;
                }
                case SEGMENTS: {
                    DistributedCacheResourceDefinition.SEGMENTS.parseAndSetParameter(value, cache, reader);
                    break;
                }
                case L1_LIFESPAN: {
                    DistributedCacheResourceDefinition.L1_LIFESPAN.parseAndSetParameter(value, cache, reader);
                    break;
                }
                default: {
                    this.parseClusteredCacheAttribute(reader, i, attribute, value, cache);
                }
            }
        }

        if (!cache.hasDefined(ModelKeys.MODE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseSharedStateCacheElement(reader, element, cacheAddress, cache, operations);
        }
    }

    private void parseReplicatedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress cacheAddress = containerAddress.append(ReplicatedCacheResourceDefinition.pathElement(name));
        ModelNode cache = Util.createAddOperation(cacheAddress);
        operations.add(cache);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache);
        }

        if (!cache.hasDefined(ModelKeys.MODE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseSharedStateCacheElement(reader, element, cacheAddress, cache, operations);
        }
    }

    private void parseInvalidationCache(XMLExtendedStreamReader reader, PathAddress containerAddress, List<ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress cacheAddress = containerAddress.append(InvalidationCacheResourceDefinition.pathElement(name));
        ModelNode cache = Util.createAddOperation(cacheAddress);
        operations.add(cache);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            this.parseClusteredCacheAttribute(reader, i, attribute, value, cache);
        }

        if (!cache.hasDefined(ModelKeys.MODE)) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                default: {
                    this.parseCacheElement(reader, element, cacheAddress, cache, operations);
                }
            }
        }
    }

    private void parseSharedStateCacheElement(XMLExtendedStreamReader reader, Element element, PathAddress cacheAddress, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        switch (element) {
            case STATE_TRANSFER: {
                this.parseStateTransfer(reader, cacheAddress, operations);
                break;
            }
            case BACKUPS: {
                this.parseBackups(reader, cacheAddress, operations);
                break;
            }
            case BACKUP_FOR: {
                this.parseBackupFor(reader, cacheAddress, operations);
                break;
            }
            default: {
                this.parseCacheElement(reader, element, cacheAddress, cache, operations);
            }
        }
    }

    private void parseCacheElement(XMLExtendedStreamReader reader, Element element, PathAddress cacheAddress, ModelNode cache, List<ModelNode> operations) throws XMLStreamException {
        switch (element) {
            case LOCKING: {
                this.parseLocking(reader, cacheAddress, operations);
                break;
            }
            case TRANSACTION: {
                this.parseTransaction(reader, cacheAddress, operations);
                break;
            }
            case EVICTION: {
                this.parseEviction(reader, cacheAddress, operations);
                break;
            }
            case EXPIRATION: {
                this.parseExpiration(reader, cacheAddress, operations);
                break;
            }
            case STORE: {
                this.parseCustomStore(reader, cacheAddress, operations);
                break;
            }
            case FILE_STORE: {
                this.parseFileStore(reader, cacheAddress, operations);
                break;
            }
            case STRING_KEYED_JDBC_STORE: {
                this.parseStringKeyedJDBCStore(reader, cacheAddress, operations);
                break;
            }
            case BINARY_KEYED_JDBC_STORE: {
                this.parseBinaryKeyedJDBCStore(reader, cacheAddress, operations);
                break;
            }
            case MIXED_KEYED_JDBC_STORE: {
                this.parseMixedKeyedJDBCStore(reader, cacheAddress, operations);
                break;
            }
            case REMOTE_STORE: {
                this.parseRemoteStore(reader, cacheAddress, operations);
                break;
            }
            case INDEXING: {
                this.parseIndexing(reader, cache);
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseStateTransfer(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress stateTransferAddress = cacheAddress.append(StateTransferResourceDefinition.PATH);
        ModelNode stateTransfer = Util.createAddOperation(stateTransferAddress);
        operations.add(stateTransfer);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    StateTransferResourceDefinition.ENABLED.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                case TIMEOUT: {
                    StateTransferResourceDefinition.TIMEOUT.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                case CHUNK_SIZE: {
                    StateTransferResourceDefinition.CHUNK_SIZE.parseAndSetParameter(value, stateTransfer, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseBackupFor(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress backupForAddress = cacheAddress.append(BackupForResourceDefinition.PATH);
        ModelNode backupFor = Util.createAddOperation(backupForAddress);
        operations.add(backupFor);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case REMOTE_CACHE: {
                    BackupForResourceDefinition.REMOTE_CACHE.parseAndSetParameter(value, backupFor, reader);
                    break;
                }
                case REMOTE_SITE: {
                    BackupForResourceDefinition.REMOTE_SITE.parseAndSetParameter(value, backupFor, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseLocking(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress lockingAddress = cacheAddress.append(LockingResourceDefinition.PATH);
        ModelNode locking = Util.createAddOperation(lockingAddress);
        operations.add(locking);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    LockingResourceDefinition.ISOLATION.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case STRIPING: {
                    LockingResourceDefinition.STRIPING.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    LockingResourceDefinition.ACQUIRE_TIMEOUT.parseAndSetParameter(value, locking, reader);
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    LockingResourceDefinition.CONCURRENCY_LEVEL.parseAndSetParameter(value, locking, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseTransaction(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress transactionAddress = cacheAddress.append(TransactionResourceDefinition.PATH);
        ModelNode transaction = Util.createAddOperation(transactionAddress);
        operations.add(transaction);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    TransactionResourceDefinition.STOP_TIMEOUT.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                case MODE: {
                    TransactionResourceDefinition.MODE.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                case LOCKING: {
                    TransactionResourceDefinition.LOCKING.parseAndSetParameter(value, transaction, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseEviction(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress evictionAddress = cacheAddress.append(EvictionResourceDefinition.PATH);
        ModelNode eviction = Util.createAddOperation(evictionAddress);
        operations.add(eviction);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STRATEGY: {
                    EvictionResourceDefinition.STRATEGY.parseAndSetParameter(value, eviction, reader);
                    break;
                }
                case MAX_ENTRIES: {
                    EvictionResourceDefinition.MAX_ENTRIES.parseAndSetParameter(value, eviction, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseExpiration(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress expirationAddress = cacheAddress.append(ExpirationResourceDefinition.PATH);
        ModelNode expiration = Util.createAddOperation(expirationAddress);
        operations.add(expiration);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    ExpirationResourceDefinition.MAX_IDLE.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                case LIFESPAN: {
                    ExpirationResourceDefinition.LIFESPAN.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                case INTERVAL: {
                    ExpirationResourceDefinition.INTERVAL.parseAndSetParameter(value, expiration, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseCustomStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = cacheAddress.append(CustomStoreResourceDefinition.PATH);
        ModelNode store = Util.createAddOperation(storeAddress);
        operations.add(store);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    CustomStoreResourceDefinition.CLASS.parseAndSetParameter(value, store, reader);
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

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseStoreElement(reader, element, storeAddress, operations);
        }
    }

    private void parseFileStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = cacheAddress.append(FileStoreResourceDefinition.PATH);
        ModelNode store = Util.createAddOperation(storeAddress);
        operations.add(store);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    FileStoreResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, store, reader);
                    break;
                }
                case PATH: {
                    FileStoreResourceDefinition.RELATIVE_PATH.parseAndSetParameter(value, store, reader);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, attribute, value, store);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            this.parseStoreElement(reader, element, storeAddress, operations);
        }
    }

    private void parseRemoteStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = cacheAddress.append(RemoteStoreResourceDefinition.PATH);
        ModelNode store = Util.createAddOperation(storeAddress);
        operations.add(store);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    RemoteStoreResourceDefinition.CACHE.parseAndSetParameter(value, store, reader);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    RemoteStoreResourceDefinition.SOCKET_TIMEOUT.parseAndSetParameter(value, store, reader);
                    break;
                }
                case TCP_NO_DELAY: {
                    RemoteStoreResourceDefinition.TCP_NO_DELAY.parseAndSetParameter(value, store, reader);
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
                    this.parseStoreElement(reader, element, storeAddress, operations);
                }
            }
        }

        if (!store.hasDefined(ModelKeys.REMOTE_SERVERS)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Element.REMOTE_SERVER));
        }
    }

    private void parseRemoteServer(XMLExtendedStreamReader reader, ModelNode server) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTBOUND_SOCKET_BINDING: {
                    RemoteStoreResourceDefinition.OUTBOUND_SOCKET_BINDING.parseAndSetParameter(value, server, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseStringKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = cacheAddress.append(StringKeyedJDBCStoreResourceDefinition.PATH);
        ModelNode store = Util.createAddOperation(storeAddress);
        operations.add(store);

        this.parseJDBCStoreAttributes(reader, store);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.STRING_KEYED_TABLE).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreElement(reader, element, storeAddress, operations);
                }
            }
        }
    }

    private void parseBinaryKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = cacheAddress.append(BinaryKeyedJDBCStoreResourceDefinition.PATH);
        ModelNode store = Util.createAddOperation(storeAddress);
        operations.add(store);

        this.parseJDBCStoreAttributes(reader, store);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, store.get(ModelKeys.BINARY_KEYED_TABLE).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreElement(reader, element, storeAddress, operations);
                }
            }
        }
    }

    private void parseMixedKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress storeAddress = cacheAddress.append(MixedKeyedJDBCStoreResourceDefinition.PATH);
        ModelNode store = Util.createAddOperation(storeAddress);
        operations.add(store);

        this.parseJDBCStoreAttributes(reader, store);

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
                default: {
                    this.parseStoreElement(reader, element, storeAddress, operations);
                }
            }
        }
    }

    private void parseJDBCStoreAttributes(XMLExtendedStreamReader reader, ModelNode store) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    JDBCStoreResourceDefinition.DATA_SOURCE.parseAndSetParameter(value, store, reader);
                    break;
                }
                case DIALECT: {
                    JDBCStoreResourceDefinition.DIALECT.parseAndSetParameter(value, store, reader);
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
    }

    private void parseJDBCStoreTable(XMLExtendedStreamReader reader, ModelNode table) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    JDBCStoreResourceDefinition.PREFIX.parseAndSetParameter(value, table, reader);
                    break;
                }
                case FETCH_SIZE: {
                    JDBCStoreResourceDefinition.FETCH_SIZE.parseAndSetParameter(value, table, reader);
                    break;
                }
                case BATCH_SIZE: {
                    JDBCStoreResourceDefinition.BATCH_SIZE.parseAndSetParameter(value, table, reader);
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
                    JDBCStoreResourceDefinition.COLUMN_NAME.parseAndSetParameter(value, column, reader);
                    break;
                }
                case TYPE: {
                    JDBCStoreResourceDefinition.COLUMN_TYPE.parseAndSetParameter(value, column, reader);
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
                StoreResourceDefinition.SHARED.parseAndSetParameter(value, store, reader);
                break;
            }
            case PRELOAD: {
                StoreResourceDefinition.PRELOAD.parseAndSetParameter(value, store, reader);
                break;
            }
            case PASSIVATION: {
                StoreResourceDefinition.PASSIVATION.parseAndSetParameter(value, store, reader);
                break;
            }
            case FETCH_STATE: {
                StoreResourceDefinition.FETCH_STATE.parseAndSetParameter(value, store, reader);
                break;
            }
            case PURGE: {
                StoreResourceDefinition.PURGE.parseAndSetParameter(value, store, reader);
                break;
            }
            case SINGLETON: {
                StoreResourceDefinition.SINGLETON.parseAndSetParameter(value, store, reader);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseStoreElement(XMLExtendedStreamReader reader, Element element, PathAddress storeAddress, List<ModelNode> operations) throws XMLStreamException {
        switch (element) {
            case WRITE_BEHIND: {
                parseStoreWriteBehind(reader, storeAddress, operations);
                break;
            }
            case PROPERTY: {
                parseStoreProperty(reader, storeAddress, operations);
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseStoreWriteBehind(XMLExtendedStreamReader reader, PathAddress storeAddress, List<ModelNode> operations) throws XMLStreamException {

        PathAddress writeBehindAddress = storeAddress.append(StoreWriteBehindResourceDefinition.PATH);
        ModelNode writeBehind = Util.createAddOperation(writeBehindAddress);
        operations.add(writeBehind);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FLUSH_LOCK_TIMEOUT: {
                    StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                case MODIFICATION_QUEUE_SIZE: {
                    StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                case SHUTDOWN_TIMEOUT: {
                    StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                case THREAD_POOL_SIZE: {
                    StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE.parseAndSetParameter(value, writeBehind, reader);
                    break;
                }
                default:
                    throw ParseUtils.unexpectedAttribute(reader, i);
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseStoreProperty(XMLExtendedStreamReader reader, PathAddress storeAddress, final List<ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        String value = reader.getElementText();

        PathAddress propertyAddress = storeAddress.append(StorePropertyResourceDefinition.pathElement(name));
        ModelNode property = Util.createAddOperation(propertyAddress);
        operations.add(property);

        // represent the value as a ModelNode to cater for expressions
        StorePropertyResourceDefinition.VALUE.parseAndSetParameter(value, property, reader);
    }

    private void parseIndexing(XMLExtendedStreamReader reader, ModelNode cache) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case INDEX: {
                    CacheResourceDefinition.INDEXING.parseAndSetParameter(value, cache, reader);
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
                case PROPERTY: {
                    String name = require(reader, Attribute.NAME);
                    String value = reader.getElementText();
                    CacheResourceDefinition.INDEXING_PROPERTIES.parseAndAddParameterElement(name, value, cache, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseBackups(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case BACKUP: {
                    this.parseBackup(reader, cacheAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseBackup(XMLExtendedStreamReader reader, PathAddress cacheAddress, List<ModelNode> operations) throws XMLStreamException {

        String site = require(reader, Attribute.SITE);
        PathAddress backupSiteAddress = cacheAddress.append(BackupSiteResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(backupSiteAddress);
        operations.add(operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    // Already read
                    break;
                }
                case STRATEGY: {
                    BackupSiteResourceDefinition.STRATEGY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case BACKUP_FAILURE_POLICY: {
                    BackupSiteResourceDefinition.FAILURE_POLICY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TIMEOUT: {
                    BackupSiteResourceDefinition.REPLICATION_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case ENABLED: {
                    BackupSiteResourceDefinition.ENABLED.parseAndSetParameter(value, operation, reader);
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
                case TAKE_OFFLINE: {
                    this.parseTakeOffline(reader, operation);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseTakeOffline(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TAKE_BACKUP_OFFLINE_AFTER_FAILURES: {
                    BackupSiteResourceDefinition.TAKE_OFFLINE_AFTER_FAILURES.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TAKE_BACKUP_OFFLINE_MIN_WAIT: {
                    BackupSiteResourceDefinition.TAKE_OFFLINE_MIN_WAIT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private String require(XMLExtendedStreamReader reader, Attribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }
}
