/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.infinispan.InfinispanLogger.ROOT_LOGGER;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * XML reader for the Infinispan subsystem.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class InfinispanSubsystemXMLReader implements XMLElementReader<List<ModelNode>> {

    private final InfinispanSchema schema;

    InfinispanSubsystemXMLReader(InfinispanSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> result) throws XMLStreamException {

        Map<PathAddress, ModelNode> operations = new LinkedHashMap<>();

        PathAddress address = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case CACHE_CONTAINER: {
                    this.parseContainer(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.addAll(operations.values());
    }

    private void parseContainer(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress address = subsystemAddress.append(CacheContainerResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case DEFAULT_CACHE: {
                    CacheContainerResourceDefinition.DEFAULT_CACHE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case JNDI_NAME: {
                    CacheContainerResourceDefinition.JNDI_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case LISTENER_EXECUTOR: {
                    CacheContainerResourceDefinition.LISTENER_EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case EVICTION_EXECUTOR: {
                    CacheContainerResourceDefinition.EVICTION_EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case REPLICATION_QUEUE_EXECUTOR: {
                    CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case START: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1) && !this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        // Ignore - we no longer support EAGER mode
                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    break;
                }
                case ALIASES: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        for (String alias: reader.getListAttributeValue(i)) {
                            CacheContainerResourceDefinition.ALIASES.parseAndAddParameterElement(alias, operation, reader);
                        }
                        break;
                    }
                }
                case MODULE: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_3)) {
                        CacheContainerResourceDefinition.MODULE.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                case STATISTICS_ENABLED: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_5)) {
                        CacheContainerResourceDefinition.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (!this.schema.since(InfinispanSchema.VERSION_1_5)) {
            operation.get(CacheContainerResourceDefinition.STATISTICS_ENABLED.getName()).set(true);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ALIAS: {
                    if (InfinispanSchema.VERSION_1_0.since(this.schema)) {
                        CacheContainerResourceDefinition.ALIASES.parseAndAddParameterElement(reader.getElementText(), operation, reader);
                        break;
                    }
                }
                case TRANSPORT: {
                    this.parseTransport(reader, address, operations);
                    break;
                }
                case LOCAL_CACHE: {
                    this.parseLocalCache(reader, address, operations);
                    break;
                }
                case INVALIDATION_CACHE: {
                    this.parseInvalidationCache(reader, address, operations);
                    break;
                }
                case REPLICATED_CACHE: {
                    this.parseReplicatedCache(reader, address, operations);
                    break;
                }
                case DISTRIBUTED_CACHE: {
                    this.parseDistributedCache(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseTransport(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = containerAddress.append(TransportResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        String stack = null;
        String cluster = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STACK: {
                    if (!this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        stack = value;
                        break;
                    }
                }
                case EXECUTOR: {
                    TransportResourceDefinition.EXECUTOR.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case LOCK_TIMEOUT: {
                    TransportResourceDefinition.LOCK_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SITE: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        ROOT_LOGGER.topologyAttributeDeprecated(ModelKeys.SITE);
                        break;
                    }
                }
                case RACK: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        ROOT_LOGGER.topologyAttributeDeprecated(ModelKeys.RACK);
                        break;
                    }
                }
                case MACHINE: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        ROOT_LOGGER.topologyAttributeDeprecated(ModelKeys.MACHINE);
                        break;
                    }
                }
                case CLUSTER: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_2) && !this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        cluster = value;
                        break;
                    }
                }
                case CHANNEL: {
                    if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        TransportResourceDefinition.CHANNEL.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!this.schema.since(InfinispanSchema.VERSION_3_0)) {
            // We need to create a corresponding channel add operation
            String channel = (cluster != null) ? cluster : containerAddress.getLastElement().getValue();
            TransportResourceDefinition.CHANNEL.parseAndSetParameter(channel, operation, reader);
            PathAddress channelAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH, ChannelResourceDefinition.pathElement(channel));
            ModelNode channelOperation = Util.createAddOperation(channelAddress);
            if (stack != null) {
                ChannelResourceDefinition.STACK.parseAndSetParameter(stack, channelOperation, reader);
            }
            operations.put(channelAddress, channelOperation);
        }

        ParseUtils.requireNoContent(reader);
    }

    private void parseLocalCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress address = containerAddress.append(CacheType.LOCAL.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseCacheAttribute(reader, i, address, operations);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseCacheElement(reader, address, operations);
        }
    }

    private void parseReplicatedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress address = containerAddress.append(CacheType.REPLICATED.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseClusteredCacheAttribute(reader, i, address, operations);
        }

        if (!operation.hasDefined(ClusteredCacheResourceDefinition.MODE.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseSharedStateCacheElement(reader, address, operations);
        }
    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress address = containerAddress.append(CacheType.DISTRIBUTED.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    DistributedCacheResourceDefinition.OWNERS.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case L1_LIFESPAN: {
                    DistributedCacheResourceDefinition.L1_LIFESPAN.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case VIRTUAL_NODES: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_4)) {
                        // AS7-5753: convert any non-expression virtual nodes value to a segments value,
                        String segments = SegmentsAndVirtualNodeConverter.virtualNodesToSegments(value);
                        DistributedCacheResourceDefinition.SEGMENTS.parseAndSetParameter(segments, operation, reader);
                        break;
                    }
                }
                case SEGMENTS: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_4)) {
                        DistributedCacheResourceDefinition.SEGMENTS.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                case CAPACITY_FACTOR: {
                    if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        DistributedCacheResourceDefinition.CAPACITY_FACTOR.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                case CONSISTENT_HASH_STRATEGY: {
                    if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        DistributedCacheResourceDefinition.CONSISTENT_HASH_STRATEGY.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                default: {
                    this.parseClusteredCacheAttribute(reader, i, address, operations);
                }
            }
        }

        if (!operation.hasDefined(ClusteredCacheResourceDefinition.MODE.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                this.parseSharedStateCacheElement(reader, address, operations);
            } else {
                Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case REHASHING: {
                        this.parseStateTransfer(reader, address, operations);
                        break;
                    }
                    default: {
                        this.parseCacheElement(reader, address, operations);
                    }
                }
            }
        }
    }

    private void parseInvalidationCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, Attribute.NAME);
        PathAddress address = containerAddress.append(CacheType.INVALIDATION.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseClusteredCacheAttribute(reader, i, address, operations);
        }

        if (!operation.hasDefined(ClusteredCacheResourceDefinition.MODE.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.MODE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseCacheElement(reader, address, operations);
        }
    }

    private void parseCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        String value = reader.getAttributeValue(index);
        Attribute attribute = Attribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case NAME: {
                // Already read
                break;
            }
            case START: {
                if (!this.schema.since(InfinispanSchema.VERSION_3_0)) {
                    // Ignore - we no longer support EAGER mode
                } else {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                break;
            }
            case BATCHING: {
                if (!this.schema.since(InfinispanSchema.VERSION_3_0)) {
                    PathAddress transactionAddress = address.append(TransactionResourceDefinition.PATH);
                    ModelNode transactionOperation = Util.createAddOperation(transactionAddress);
                    transactionOperation.get(TransactionResourceDefinition.MODE.getName()).set(new ModelNode(TransactionMode.BATCH.name()));
                    operations.put(transactionAddress, transactionOperation);
                } else {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                break;
            }
            case INDEXING: {
                if (!this.schema.since(InfinispanSchema.VERSION_1_4)) {
                    CacheResourceDefinition.INDEXING.parseAndSetParameter(value, operation, reader);
                } else {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                break;
            }
            case JNDI_NAME: {
                if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                    CacheResourceDefinition.JNDI_NAME.parseAndSetParameter(value, operation, reader);
                    break;
                }
            }
            case MODULE: {
                if (this.schema.since(InfinispanSchema.VERSION_1_3)) {
                    CacheResourceDefinition.MODULE.parseAndSetParameter(value, operation, reader);
                    break;
                }
            }
            case STATISTICS_ENABLED: {
                if (this.schema.since(InfinispanSchema.VERSION_1_5)) {
                    CacheResourceDefinition.STATISTICS_ENABLED.parseAndSetParameter(value, operation, reader);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
        if (!this.schema.since(InfinispanSchema.VERSION_1_5)) {
            // We need to explicitly enable statistics (to reproduce old behavior), since the new attribute defaults to false.
            operation.get(CacheResourceDefinition.STATISTICS_ENABLED.getName()).set(true);
        }
    }

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        String value = reader.getAttributeValue(index);
        Attribute attribute = Attribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case MODE: {
                // note the use of ClusteredCacheAdd.MODE
                ClusteredCacheResourceDefinition.MODE.parseAndSetParameter(value, operation, reader);
                break;
            }
            case QUEUE_SIZE: {
                ClusteredCacheResourceDefinition.QUEUE_SIZE.parseAndSetParameter(value, operation, reader);
                break;
            }
            case QUEUE_FLUSH_INTERVAL: {
                ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.parseAndSetParameter(value, operation, reader);
                break;
            }
            case REMOTE_TIMEOUT: {
                ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.parseAndSetParameter(value, operation, reader);
                break;
            }
            case ASYNC_MARSHALLING: {
                if (this.schema.since(InfinispanSchema.VERSION_1_2)) {
                    ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.parseAndSetParameter(value, operation, reader);
                    break;
                }
            }
            default: {
                this.parseCacheAttribute(reader, index, address, operations);
            }
        }
    }

    private void parseCacheElement(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        Element element = Element.forName(reader.getLocalName());
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
            case REMOTE_STORE: {
                this.parseRemoteStore(reader, cacheAddress, operations);
                break;
            }
            case JDBC_STORE: {
                if (!this.schema.since(InfinispanSchema.VERSION_1_2)) {
                    this.parseJDBCStore(reader, cacheAddress, operations);
                    break;
                }
            }
            case STRING_KEYED_JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_1_2)) {
                    this.parseStringKeyedJDBCStore(reader, cacheAddress, operations);
                    break;
                }
            }
            case BINARY_KEYED_JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_1_2)) {
                    this.parseBinaryKeyedJDBCStore(reader, cacheAddress, operations);
                    break;
                }
            }
            case MIXED_KEYED_JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_1_2)) {
                    this.parseMixedKeyedJDBCStore(reader, cacheAddress, operations);
                    break;
                }
            }
            case INDEXING: {
                if (this.schema.since(InfinispanSchema.VERSION_1_4)) {
                    this.parseIndexing(reader, operations.get(cacheAddress));
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseSharedStateCacheElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case STATE_TRANSFER: {
                this.parseStateTransfer(reader, address, operations);
                break;
            }
            case BACKUPS: {
                if (this.schema.since(InfinispanSchema.VERSION_2_0)) {
                    this.parseBackups(reader, address, operations);
                    break;
                }
            }
            case BACKUP_FOR: {
                if (this.schema.since(InfinispanSchema.VERSION_2_0)) {
                    this.parseBackupFor(reader, address, operations);
                    break;
                }
            }
            default: {
                this.parseCacheElement(reader, address, operations);
            }
        }
    }

    private void parseStateTransfer(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(StateTransferResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    StateTransferResourceDefinition.ENABLED.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TIMEOUT: {
                    StateTransferResourceDefinition.TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case FLUSH_TIMEOUT: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        // Ignore
                        break;
                    }
                }
                case CHUNK_SIZE: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        StateTransferResourceDefinition.CHUNK_SIZE.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseBackups(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

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

    private void parseBackup(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String site = require(reader, Attribute.SITE);
        PathAddress address = cacheAddress.append(BackupSiteResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    // Already parsed
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

    private void parseBackupFor(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(BackupForResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case REMOTE_CACHE: {
                    BackupForResourceDefinition.REMOTE_CACHE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case REMOTE_SITE: {
                    BackupForResourceDefinition.REMOTE_SITE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseLocking(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(LockingResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    LockingResourceDefinition.ISOLATION.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case STRIPING: {
                    LockingResourceDefinition.STRIPING.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    LockingResourceDefinition.ACQUIRE_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    LockingResourceDefinition.CONCURRENCY_LEVEL.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseTransaction(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(TransactionResourceDefinition.PATH);
        ModelNode operation = operations.get(address);
        if (operation == null) {
            operation = Util.createAddOperation(address);
            operations.put(address, operation);
        }

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    TransactionResourceDefinition.STOP_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MODE: {
                    TransactionResourceDefinition.MODE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case LOCKING: {
                    TransactionResourceDefinition.LOCKING.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case EAGER_LOCKING: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        ROOT_LOGGER.eagerAttributeDeprecated();
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseEviction(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(EvictionResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STRATEGY: {
                    EvictionResourceDefinition.STRATEGY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MAX_ENTRIES: {
                    EvictionResourceDefinition.MAX_ENTRIES.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case INTERVAL: {
                    if (!this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        // Ignore
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseExpiration(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(ExpirationResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    ExpirationResourceDefinition.MAX_IDLE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case LIFESPAN: {
                    ExpirationResourceDefinition.LIFESPAN.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case INTERVAL: {
                    ExpirationResourceDefinition.INTERVAL.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseIndexing(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case INDEX: {
                    CacheResourceDefinition.INDEXING.parseAndSetParameter(value, operation, reader);
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
                    CacheResourceDefinition.INDEXING_PROPERTIES.parseAndAddParameterElement(name, reader.getElementText(), operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseCustomStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(CustomStoreResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    CustomStoreResourceDefinition.CLASS.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        if (!operation.hasDefined(CustomStoreResourceDefinition.CLASS.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.CLASS));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    private void parseFileStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(FileStoreResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    FileStoreResourceDefinition.RELATIVE_TO.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case PATH: {
                    FileStoreResourceDefinition.RELATIVE_PATH.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    private void parseRemoteStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(RemoteStoreResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    RemoteStoreResourceDefinition.CACHE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    RemoteStoreResourceDefinition.SOCKET_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case TCP_NO_DELAY: {
                    RemoteStoreResourceDefinition.TCP_NO_DELAY.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_SERVER: {
                    this.parseRemoteServer(reader, operation);
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }

        if (!operation.hasDefined(RemoteStoreResourceDefinition.REMOTE_SERVERS.getName())) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Element.REMOTE_SERVER));
        }
    }

    private void parseRemoteServer(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        ModelNode server = operation.get(RemoteStoreResourceDefinition.REMOTE_SERVERS.getName()).add();
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

    private void parseJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        // We don't know the path yet
        PathAddress address = null;
        ModelNode operation = Util.createAddOperation();

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ENTRY_TABLE: {
                    if (address != null) {
                        operations.remove(address);
                    }
                    address = cacheAddress.append((address == null) ? BinaryKeyedJDBCStoreResourceDefinition.PATH : MixedKeyedJDBCStoreResourceDefinition.PATH);
                    operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
                    operations.put(address, operation);
                    this.parseJDBCStoreTable(reader, operation.get(JDBCStoreResourceDefinition.STRING_KEYED_TABLE.getName()).setEmptyObject());
                    break;
                }
                case BUCKET_TABLE: {
                    if (address != null) {
                        operations.remove(address);
                    }
                    address = cacheAddress.append((address == null) ? StringKeyedJDBCStoreResourceDefinition.PATH : MixedKeyedJDBCStoreResourceDefinition.PATH);
                    operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
                    operations.put(address, operation);
                    this.parseJDBCStoreTable(reader, operation.get(JDBCStoreResourceDefinition.BINARY_KEYED_TABLE.getName()).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseStringKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(StringKeyedJDBCStoreResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, operation.get(JDBCStoreResourceDefinition.STRING_KEYED_TABLE.getName()).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseBinaryKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(BinaryKeyedJDBCStoreResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, operation.get(JDBCStoreResourceDefinition.BINARY_KEYED_TABLE.getName()).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseMixedKeyedJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(MixedKeyedJDBCStoreResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, operation.get(JDBCStoreResourceDefinition.STRING_KEYED_TABLE.getName()).setEmptyObject());
                    break;
                }
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreTable(reader, operation.get(JDBCStoreResourceDefinition.BINARY_KEYED_TABLE.getName()).setEmptyObject());
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseJDBCStoreAttributes(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    JDBCStoreResourceDefinition.DATA_SOURCE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case DIALECT: {
                    if (this.schema.since(InfinispanSchema.VERSION_2_0)) {
                        JDBCStoreResourceDefinition.DIALECT.parseAndSetParameter(value, operation, reader);
                        break;
                    }
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        if (!operation.hasDefined(JDBCStoreResourceDefinition.DATA_SOURCE.getName())) {
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
                    this.parseJDBCStoreColumn(reader, table.get(JDBCStoreResourceDefinition.ID_COLUMN.getName()).setEmptyObject());
                    break;
                }
                case DATA_COLUMN: {
                    this.parseJDBCStoreColumn(reader, table.get(JDBCStoreResourceDefinition.DATA_COLUMN.getName()).setEmptyObject());
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    this.parseJDBCStoreColumn(reader, table.get(JDBCStoreResourceDefinition.TIMESTAMP_COLUMN.getName()).setEmptyObject());
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

    private void parseStoreAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        String value = reader.getAttributeValue(index);
        Attribute attribute = Attribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SHARED: {
                StoreResourceDefinition.SHARED.parseAndSetParameter(value, operation, reader);
                break;
            }
            case PRELOAD: {
                StoreResourceDefinition.PRELOAD.parseAndSetParameter(value, operation, reader);
                break;
            }
            case PASSIVATION: {
                StoreResourceDefinition.PASSIVATION.parseAndSetParameter(value, operation, reader);
                break;
            }
            case FETCH_STATE: {
                StoreResourceDefinition.FETCH_STATE.parseAndSetParameter(value, operation, reader);
                break;
            }
            case PURGE: {
                StoreResourceDefinition.PURGE.parseAndSetParameter(value, operation, reader);
                break;
            }
            case SINGLETON: {
                StoreResourceDefinition.SINGLETON.parseAndSetParameter(value, operation, reader);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseStoreElement(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(storeAddress);

        Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case PROPERTY: {
                ParseUtils.requireSingleAttribute(reader, Attribute.NAME.getLocalName());
                String name = require(reader, Attribute.NAME);
                StoreResourceDefinition.PROPERTIES.parseAndAddParameterElement(name, reader.getElementText(), operation, reader);
                break;
            }
            case WRITE_BEHIND: {
                if (this.schema.since(InfinispanSchema.VERSION_1_2)) {
                    this.parseStoreWriteBehind(reader, storeAddress, operations);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseStoreWriteBehind(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = storeAddress.append(StoreWriteBehindResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FLUSH_LOCK_TIMEOUT: {
                    StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case MODIFICATION_QUEUE_SIZE: {
                    StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case SHUTDOWN_TIMEOUT: {
                    StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT.parseAndSetParameter(value, operation, reader);
                    break;
                }
                case THREAD_POOL_SIZE: {
                    StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE.parseAndSetParameter(value, operation, reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private static String require(XMLExtendedStreamReader reader, Attribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }
}
