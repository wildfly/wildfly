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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.infinispan.subsystem.TableResourceDefinition.ColumnAttribute;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.InvalidationNearCacheResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Element;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * XML reader for the Infinispan subsystem.
 *
 * @author Paul Ferraro
 */
@SuppressWarnings({ "deprecation", "static-method" })
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
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case CACHE_CONTAINER: {
                    this.parseContainer(reader, address, operations);
                    break;
                }
                case REMOTE_CACHE_CONTAINER: {
                    if (this.schema.since(InfinispanSchema.VERSION_6_0)) {
                        this.parseRemoteContainer(reader, address, operations);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.addAll(operations.values());
    }

    private void parseContainer(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(CacheContainerResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case DEFAULT_CACHE: {
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE);
                    break;
                }
                case JNDI_NAME: {
                    if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.DeprecatedAttribute.JNDI_NAME);
                    break;
                }
                case LISTENER_EXECUTOR: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.ExecutorAttribute.LISTENER);
                    ROOT_LOGGER.executorIgnored(CacheContainerResourceDefinition.ExecutorAttribute.LISTENER.getName());
                    break;
                }
                case EVICTION_EXECUTOR: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.ExecutorAttribute.EVICTION);
                    ROOT_LOGGER.executorIgnored(CacheContainerResourceDefinition.ExecutorAttribute.EVICTION.getName());
                    break;
                }
                case REPLICATION_QUEUE_EXECUTOR: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, CacheContainerResourceDefinition.ExecutorAttribute.REPLICATION_QUEUE);
                    ROOT_LOGGER.executorIgnored(CacheContainerResourceDefinition.ExecutorAttribute.REPLICATION_QUEUE.getName());
                    break;
                }
                case START: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1) && !this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    } else {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    break;
                }
                case ALIASES: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.ALIASES);
                        break;
                    }
                }
                case MODULE: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_3)) {
                        readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.MODULE);
                        break;
                    }
                }
                case STATISTICS_ENABLED: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_5)) {
                        readAttribute(reader, i, operation, CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!this.schema.since(InfinispanSchema.VERSION_1_5)) {
            operation.get(CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED.getName()).set(true);
        }

        List<String> aliases = new LinkedList<>();

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ALIAS: {
                    if (InfinispanSchema.VERSION_1_0.since(this.schema)) {
                        aliases.add(reader.getElementText());
                        break;
                    }
                    throw ParseUtils.unexpectedElement(reader);
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
                case EXPIRATION_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        this.parseScheduledThreadPool(ScheduledThreadPoolResourceDefinition.EXPIRATION, reader, address, operations);
                        break;
                    }
                }
                case ASYNC_OPERATIONS_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.ASYNC_OPERATIONS, reader, address, operations);
                        break;
                    }
                }
                case LISTENER_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.LISTENER, reader, address, operations);
                        break;
                    }
                }
                case PERSISTENCE_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        if (this.schema.since(InfinispanSchema.VERSION_7_0)) {
                            this.parseScheduledThreadPool(ThreadPoolResourceDefinition.PERSISTENCE, reader, address, operations);
                        } else {
                            this.parseThreadPool(ThreadPoolResourceDefinition.PERSISTENCE, reader, address, operations);
                        }
                        break;
                    }
                }
                case REMOTE_COMMAND_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.REMOTE_COMMAND, reader, address, operations);
                        break;
                    }
                }
                case STATE_TRANSFER_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.STATE_TRANSFER, reader, address, operations);
                        break;
                    }
                }
                case TRANSPORT_THREAD_POOL: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        this.parseThreadPool(ThreadPoolResourceDefinition.TRANSPORT, reader, address, operations);
                        break;
                    }
                }
                case SCATTERED_CACHE: {
                    if (this.schema.since(InfinispanSchema.VERSION_6_0)) {
                        this.parseScatteredCache(reader, address, operations);
                        break;
                    }
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        if (!aliases.isEmpty()) {
            // Adapt aliases parsed from legacy schema into format expected by the current attribute parser
            setAttribute(reader, String.join(" ", aliases), operation, CacheContainerResourceDefinition.Attribute.ALIASES);
        }
    }

    private void parseTransport(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = containerAddress.append(JGroupsTransportResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(containerAddress.append(TransportResourceDefinition.WILDCARD_PATH), operation);

        String stack = null;
        String cluster = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STACK: {
                    if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    stack = value;
                    break;
                }
                case EXECUTOR: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, JGroupsTransportResourceDefinition.ExecutorAttribute.TRANSPORT);
                    ROOT_LOGGER.executorIgnored(JGroupsTransportResourceDefinition.ExecutorAttribute.TRANSPORT.getName());
                    break;
                }
                case LOCK_TIMEOUT: {
                    readAttribute(reader, i, operation, JGroupsTransportResourceDefinition.Attribute.LOCK_TIMEOUT);
                    break;
                }
                case SITE: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.topologyAttributeDeprecated(XMLAttribute.SITE.getLocalName());
                    break;
                }
                case RACK: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.topologyAttributeDeprecated(XMLAttribute.RACK.getLocalName());
                    break;
                }
                case MACHINE: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.topologyAttributeDeprecated(XMLAttribute.MACHINE.getLocalName());
                    break;
                }
                case CLUSTER: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_2) && !this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        cluster = value;
                        break;
                    }
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
                case CHANNEL: {
                    if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        readAttribute(reader, i, operation, JGroupsTransportResourceDefinition.Attribute.CHANNEL);
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
            String channel = (cluster != null) ? cluster : ("ee-" + containerAddress.getLastElement().getValue());
            setAttribute(reader, channel, operation, JGroupsTransportResourceDefinition.Attribute.CHANNEL);
            PathAddress channelAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinition.PATH, ChannelResourceDefinition.pathElement(channel));
            ModelNode channelOperation = Util.createAddOperation(channelAddress);
            if (stack != null) {
                setAttribute(reader, stack, channelOperation, ChannelResourceDefinition.Attribute.STACK);
            }
            operations.put(channelAddress, channelOperation);
        }

        ParseUtils.requireNoContent(reader);
    }

    private void parseLocalCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(LocalCacheResourceDefinition.pathElement(name));
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

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(ReplicatedCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseClusteredCacheAttribute(reader, i, address, operations);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseSharedStateCacheElement(reader, address, operations);
        }
    }

    private void parseScatteredCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(ScatteredCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case BIAS_LIFESPAN: {
                    readAttribute(reader, i, operation, ScatteredCacheResourceDefinition.Attribute.BIAS_LIFESPAN);
                    break;
                }
                case INVALIDATION_BATCH_SIZE: {
                    readAttribute(reader, i, operation, ScatteredCacheResourceDefinition.Attribute.INVALIDATION_BATCH_SIZE);
                    break;
                }
                default: {
                    this.parseSegmentedCacheAttribute(reader, i, address, operations);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseSharedStateCacheElement(reader, address, operations);
        }
    }

    private void parseDistributedCache(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(DistributedCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OWNERS: {
                    readAttribute(reader, i, operation, DistributedCacheResourceDefinition.Attribute.OWNERS);
                    break;
                }
                case L1_LIFESPAN: {
                    readAttribute(reader, i, operation, DistributedCacheResourceDefinition.Attribute.L1_LIFESPAN);
                    break;
                }
                case VIRTUAL_NODES: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_4)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    // AS7-5753: convert any non-expression virtual nodes value to a segments value,
                    String virtualNodes = readAttribute(reader, i, SegmentedCacheResourceDefinition.Attribute.SEGMENTS).asString();
                    String segments = SegmentsAndVirtualNodeConverter.virtualNodesToSegments(virtualNodes);
                    setAttribute(reader, segments, operation, SegmentedCacheResourceDefinition.Attribute.SEGMENTS);
                    break;
                }
                case CAPACITY_FACTOR: {
                    if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                        readAttribute(reader, i, operation, DistributedCacheResourceDefinition.Attribute.CAPACITY_FACTOR);
                        break;
                    }
                }
                default: {
                    this.parseSegmentedCacheAttribute(reader, i, address, operations);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                this.parseSharedStateCacheElement(reader, address, operations);
            } else {
                XMLElement element = XMLElement.forName(reader.getLocalName());
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

        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = containerAddress.append(InvalidationCacheResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseClusteredCacheAttribute(reader, i, address, operations);
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseCacheElement(reader, address, operations);
        }
    }

    private void parseCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case NAME: {
                // Already read
                break;
            }
            case START: {
                if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                break;
            }
            case BATCHING: {
                if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                PathAddress transactionAddress = address.append(TransactionResourceDefinition.PATH);
                ModelNode transactionOperation = Util.createAddOperation(transactionAddress);
                transactionOperation.get(TransactionResourceDefinition.Attribute.MODE.getName()).set(new ModelNode(TransactionMode.BATCH.name()));
                operations.put(transactionAddress, transactionOperation);
                break;
            }
            case INDEXING: {
                if (this.schema.since(InfinispanSchema.VERSION_1_4)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                readAttribute(reader, index, operation, CacheResourceDefinition.DeprecatedAttribute.INDEXING);
                break;
            }
            case JNDI_NAME: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                    readAttribute(reader, index, operation, CacheResourceDefinition.DeprecatedAttribute.JNDI_NAME);
                    break;
                }
            }
            case MODULE: {
                if (this.schema.since(InfinispanSchema.VERSION_1_3)) {
                    readAttribute(reader, index, operation, CacheResourceDefinition.Attribute.MODULE);
                    break;
                }
            }
            case STATISTICS_ENABLED: {
                if (this.schema.since(InfinispanSchema.VERSION_1_5)) {
                    readAttribute(reader, index, operation, CacheResourceDefinition.Attribute.STATISTICS_ENABLED);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }

        if (!this.schema.since(InfinispanSchema.VERSION_1_5)) {
            // We need to explicitly enable statistics (to reproduce old behavior), since the new attribute defaults to false.
            operation.get(CacheResourceDefinition.Attribute.STATISTICS_ENABLED.getName()).set(true);
        }
    }

    private void parseSegmentedCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SEGMENTS: {
                if (this.schema.since(InfinispanSchema.VERSION_1_4)) {
                    readAttribute(reader, index, operation, SegmentedCacheResourceDefinition.Attribute.SEGMENTS);
                    break;
                }
            }
            case CONSISTENT_HASH_STRATEGY: {
                if (this.schema.since(InfinispanSchema.VERSION_3_0)) {
                    readAttribute(reader, index, operation, SegmentedCacheResourceDefinition.Attribute.CONSISTENT_HASH_STRATEGY);
                    break;
                }
            }
            default: {
                this.parseClusteredCacheAttribute(reader, index, address, operations);
            }
        }
    }

    private void parseClusteredCacheAttribute(XMLExtendedStreamReader reader, int index, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(address);
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case MODE: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                break;
            }
            case QUEUE_SIZE: {
                readAttribute(reader, index, operation, ClusteredCacheResourceDefinition.DeprecatedAttribute.QUEUE_SIZE);
                break;
            }
            case QUEUE_FLUSH_INTERVAL: {
                readAttribute(reader, index, operation, ClusteredCacheResourceDefinition.DeprecatedAttribute.QUEUE_FLUSH_INTERVAL);
                break;
            }
            case REMOTE_TIMEOUT: {
                readAttribute(reader, index, operation, ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT);
                break;
            }
            case ASYNC_MARSHALLING: {
                if (!this.schema.since(InfinispanSchema.VERSION_1_2) && this.schema.since(InfinispanSchema.VERSION_4_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                break;
            }
            default: {
                this.parseCacheAttribute(reader, index, address, operations);
            }
        }
    }

    private void parseCacheElement(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case EVICTION: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                this.parseEviction(reader, cacheAddress, operations);
                break;
            }
            case EXPIRATION: {
                this.parseExpiration(reader, cacheAddress, operations);
                break;
            }
            case LOCKING: {
                this.parseLocking(reader, cacheAddress, operations);
                break;
            }
            case TRANSACTION: {
                this.parseTransaction(reader, cacheAddress, operations);
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
            case HOTROD_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_6_0)) {
                    this.parseHotRodStore(reader, cacheAddress, operations);
                    break;
                }
            }
            case JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_1_2) && !this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    this.parseJDBCStore(reader, cacheAddress, operations);
                } else {
                    this.parseLegacyJDBCStore(reader, cacheAddress, operations);
                }
                break;
            }
            case STRING_KEYED_JDBC_STORE: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedElement(reader);
                }
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
                if (this.schema.since(InfinispanSchema.VERSION_1_4) && !this.schema.since(InfinispanSchema.VERSION_4_0)) {
                    this.parseIndexing(reader, cacheAddress, operations);
                    break;
                }
            }
            case OBJECT_MEMORY: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    this.parseObjectMemory(reader, cacheAddress, operations);
                    break;
                }
            }
            case BINARY_MEMORY: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    this.parseBinaryMemory(reader, cacheAddress, operations);
                    break;
                }
            }
            case OFF_HEAP_MEMORY: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    this.parseOffHeapMemory(reader, cacheAddress, operations);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    private void parseSharedStateCacheElement(XMLExtendedStreamReader reader, PathAddress address, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        XMLElement element = XMLElement.forName(reader.getLocalName());
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
                if (this.schema.since(InfinispanSchema.VERSION_2_0) && !this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    this.parseBackupFor(reader, address, operations);
                    break;
                }
                throw ParseUtils.unexpectedElement(reader);
            }
            case PARTITION_HANDLING: {
                if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                    this.parsePartitionHandling(reader, address, operations);
                    break;
                }
            }
            default: {
                this.parseCacheElement(reader, address, operations);
            }
        }
    }

    private void parsePartitionHandling(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(PartitionHandlingResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    readAttribute(reader, i, operation, PartitionHandlingResourceDefinition.Attribute.ENABLED);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseStateTransfer(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(StateTransferResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ENABLED: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case TIMEOUT: {
                    readAttribute(reader, i, operation, StateTransferResourceDefinition.Attribute.TIMEOUT);
                    break;
                }
                case FLUSH_TIMEOUT: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case CHUNK_SIZE: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        readAttribute(reader, i, operation, StateTransferResourceDefinition.Attribute.CHUNK_SIZE);
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

        PathAddress address = cacheAddress.append(BackupsResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case BACKUP: {
                    this.parseBackup(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseBackup(XMLExtendedStreamReader reader, PathAddress backupsAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        String site = require(reader, XMLAttribute.SITE);
        PathAddress address = backupsAddress.append(BackupResourceDefinition.pathElement(site));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SITE: {
                    // Already parsed
                    break;
                }
                case STRATEGY: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.STRATEGY);
                    break;
                }
                case BACKUP_FAILURE_POLICY: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.FAILURE_POLICY);
                    break;
                }
                case TIMEOUT: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.TIMEOUT);
                    break;
                }
                case ENABLED: {
                    readAttribute(reader, i, operation, BackupResourceDefinition.Attribute.ENABLED);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case TAKE_OFFLINE: {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case TAKE_OFFLINE_AFTER_FAILURES: {
                                readAttribute(reader, i, operation, BackupResourceDefinition.TakeOfflineAttribute.AFTER_FAILURES);
                                break;
                            }
                            case TAKE_OFFLINE_MIN_WAIT: {
                                readAttribute(reader, i, operation, BackupResourceDefinition.TakeOfflineAttribute.MIN_WAIT);
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseBackupFor(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(BackupForResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case REMOTE_CACHE: {
                    readAttribute(reader, i, operation, BackupForResourceDefinition.Attribute.CACHE);
                    break;
                }
                case REMOTE_SITE: {
                    readAttribute(reader, i, operation, BackupForResourceDefinition.Attribute.SITE);
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ISOLATION: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.ISOLATION);
                    break;
                }
                case STRIPING: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.STRIPING);
                    break;
                }
                case ACQUIRE_TIMEOUT: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.ACQUIRE_TIMEOUT);
                    break;
                }
                case CONCURRENCY_LEVEL: {
                    readAttribute(reader, i, operation, LockingResourceDefinition.Attribute.CONCURRENCY);
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STOP_TIMEOUT: {
                    readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.STOP_TIMEOUT);
                    break;
                }
                case MODE: {
                    readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.MODE);
                    break;
                }
                case LOCKING: {
                    readAttribute(reader, i, operation, TransactionResourceDefinition.Attribute.LOCKING);
                    break;
                }
                case EAGER_LOCKING: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseEviction(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(ObjectMemoryResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case STRATEGY: {
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case MAX_ENTRIES: {
                    readAttribute(reader, i, operation, ObjectMemoryResourceDefinition.DeprecatedAttribute.MAX_ENTRIES);
                    break;
                }
                case INTERVAL: {
                    if (this.schema.since(InfinispanSchema.VERSION_1_1)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_IDLE: {
                    readAttribute(reader, i, operation, ExpirationResourceDefinition.Attribute.MAX_IDLE);
                    break;
                }
                case LIFESPAN: {
                    readAttribute(reader, i, operation, ExpirationResourceDefinition.Attribute.LIFESPAN);
                    break;
                }
                case INTERVAL: {
                    readAttribute(reader, i, operation, ExpirationResourceDefinition.Attribute.INTERVAL);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseIndexing(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        ModelNode operation = operations.get(cacheAddress);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case INDEX: {
                    readAttribute(reader, i, operation, CacheResourceDefinition.DeprecatedAttribute.INDEXING);
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
                    ParseUtils.requireSingleAttribute(reader, XMLAttribute.NAME.getLocalName());
                    readElement(reader, operation, CacheResourceDefinition.DeprecatedAttribute.INDEXING_PROPERTIES);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseObjectMemory(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(ObjectMemoryResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseMemoryAttribute(reader, i, operation);
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseBinaryMemory(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(BinaryMemoryResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            this.parseBinaryMemoryAttribute(reader, i, operation);
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseOffHeapMemory(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(OffHeapMemoryResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CAPACITY: {
                    readAttribute(reader, i, operation, OffHeapMemoryResourceDefinition.Attribute.CAPACITY);
                    break;
                }
                default: {
                    this.parseBinaryMemoryAttribute(reader, i, operation);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseBinaryMemoryAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {

        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case EVICTION_TYPE: {
                readAttribute(reader, index, operation, BinaryMemoryResourceDefinition.Attribute.EVICTION_TYPE);
                break;
            }
            default: {
                this.parseMemoryAttribute(reader, index, operation);
            }
        }
    }

    private void parseMemoryAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {

        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SIZE: {
                readAttribute(reader, index, operation, ObjectMemoryResourceDefinition.Attribute.SIZE);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseCustomStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(CustomStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CLASS: {
                    readAttribute(reader, i, operation, CustomStoreResourceDefinition.Attribute.CLASS);
                    break;
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        if (!operation.hasDefined(CustomStoreResourceDefinition.Attribute.CLASS.getName())) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(XMLAttribute.CLASS));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            this.parseStoreElement(reader, address, operations);
        }
    }

    private void parseFileStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(FileStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case RELATIVE_TO: {
                    readAttribute(reader, i, operation, FileStoreResourceDefinition.Attribute.RELATIVE_TO);
                    break;
                }
                case PATH: {
                    readAttribute(reader, i, operation, FileStoreResourceDefinition.Attribute.RELATIVE_PATH);
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
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.CACHE);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.SOCKET_TIMEOUT);
                    break;
                }
                case TCP_NO_DELAY: {
                    readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.TCP_NO_DELAY);
                    break;
                }
                case REMOTE_SERVERS: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.SOCKET_BINDINGS);
                        break;
                    }
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_SERVER: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
                        switch (attribute) {
                            case OUTBOUND_SOCKET_BINDING: {
                                readAttribute(reader, i, operation, RemoteStoreResourceDefinition.Attribute.SOCKET_BINDINGS);
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }

        if (!operation.hasDefined(RemoteStoreResourceDefinition.Attribute.SOCKET_BINDINGS.getName())) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(XMLAttribute.REMOTE_SERVERS.getLocalName()));
        }
    }

    private void parseHotRodStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = cacheAddress.append(HotRodStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case CACHE_CONFIGURATION: {
                    readAttribute(reader, i, operation, HotRodStoreResourceDefinition.Attribute.CACHE_CONFIGURATION);
                    break;
                }
                case REMOTE_CACHE_CONTAINER: {
                    readAttribute(reader, i, operation, HotRodStoreResourceDefinition.Attribute.REMOTE_CACHE_CONTAINER);
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

    private void parseJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = cacheAddress.append(JDBCStoreResourceDefinition.PATH);
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case TABLE: {
                    this.parseJDBCStoreStringTable(reader, address, operations);
                    break;
                }
                default: {
                    this.parseStoreElement(reader, address, operations);
                }
            }
        }
    }

    private void parseLegacyJDBCStore(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        // We don't know the path yet
        PathAddress address = null;
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation();
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ENTRY_TABLE: {
                    if (address != null) {
                        this.removeStoreOperations(address, operations);
                    }
                    address = cacheAddress.append((address == null) ? StringKeyedJDBCStoreResourceDefinition.PATH : MixedKeyedJDBCStoreResourceDefinition.PATH);
                    Operations.setPathAddress(operation, address);

                    ModelNode binaryTableOperation = operations.get(operationKey.append(BinaryTableResourceDefinition.PATH));
                    if (binaryTableOperation != null) {
                        // Fix address of binary table operation
                        Operations.setPathAddress(binaryTableOperation, address.append(BinaryTableResourceDefinition.PATH));
                    }

                    this.parseJDBCStoreStringTable(reader, address, operations);
                    break;
                }
                case BUCKET_TABLE: {
                    if (address != null) {
                        this.removeStoreOperations(address, operations);
                    }
                    address = cacheAddress.append((address == null) ? BinaryKeyedJDBCStoreResourceDefinition.PATH : MixedKeyedJDBCStoreResourceDefinition.PATH);
                    Operations.setPathAddress(operation, address);

                    ModelNode stringTableOperation = operations.get(operationKey.append(StringTableResourceDefinition.PATH));
                    if (stringTableOperation != null) {
                        // Fix address of string table operation
                        Operations.setPathAddress(stringTableOperation, address.append(StringTableResourceDefinition.PATH));
                    }

                    this.parseJDBCStoreBinaryTable(reader, address, operations);
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
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreBinaryTable(reader, address, operations);
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
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreStringTable(reader, address, operations);
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
        PathAddress operationKey = cacheAddress.append(StoreResourceDefinition.WILDCARD_PATH);
        if (operations.containsKey(operationKey)) {
            throw ParseUtils.unexpectedElement(reader);
        }
        ModelNode operation = Util.createAddOperation(address);
        operations.put(operationKey, operation);

        this.parseJDBCStoreAttributes(reader, operation);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case BINARY_KEYED_TABLE: {
                    this.parseJDBCStoreBinaryTable(reader, address, operations);
                    break;
                }
                case STRING_KEYED_TABLE: {
                    this.parseJDBCStoreStringTable(reader, address, operations);
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
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DATASOURCE: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    readAttribute(reader, i, operation, JDBCStoreResourceDefinition.DeprecatedAttribute.DATASOURCE);
                    break;
                }
                case DIALECT: {
                    if (this.schema.since(InfinispanSchema.VERSION_2_0)) {
                        readAttribute(reader, i, operation, JDBCStoreResourceDefinition.Attribute.DIALECT);
                        break;
                    }
                }
                case DATA_SOURCE: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        readAttribute(reader, i, operation, JDBCStoreResourceDefinition.Attribute.DATA_SOURCE);
                        break;
                    }
                }
                default: {
                    this.parseStoreAttribute(reader, i, operation);
                }
            }
        }

        Attribute requiredAttribute = this.schema.since(InfinispanSchema.VERSION_4_0) ? JDBCStoreResourceDefinition.Attribute.DATA_SOURCE : JDBCStoreResourceDefinition.DeprecatedAttribute.DATASOURCE;
        if (!operation.hasDefined(requiredAttribute.getName())) {
            throw ParseUtils.missingRequired(reader, requiredAttribute.getName());
        }
    }

    private void parseJDBCStoreBinaryTable(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = storeAddress.append(BinaryTableResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(storeAddress.getParent().append(StoreResourceDefinition.WILDCARD_PATH).append(BinaryTableResourceDefinition.PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    readAttribute(reader, i, operation, BinaryTableResourceDefinition.Attribute.PREFIX);
                    break;
                }
                default: {
                    this.parseJDBCStoreTableAttribute(reader, i, operation);
                }
            }
        }

        this.parseJDBCStoreTableElements(reader, operation);
    }

    private void parseJDBCStoreStringTable(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {

        PathAddress address = storeAddress.append(StringTableResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(storeAddress.getParent().append(StoreResourceDefinition.WILDCARD_PATH).append(StringTableResourceDefinition.PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case PREFIX: {
                    readAttribute(reader, i, operation, StringTableResourceDefinition.Attribute.PREFIX);
                    break;
                }
                default: {
                    this.parseJDBCStoreTableAttribute(reader, i, operation);
                }
            }
        }

        this.parseJDBCStoreTableElements(reader, operation);
    }

    private void parseJDBCStoreTableAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {

        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case FETCH_SIZE: {
                readAttribute(reader, index, operation, TableResourceDefinition.Attribute.FETCH_SIZE);
                break;
            }
            case BATCH_SIZE: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    throw ParseUtils.unexpectedAttribute(reader, index);
                }
                readAttribute(reader, index, operation, TableResourceDefinition.DeprecatedAttribute.BATCH_SIZE);
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseJDBCStoreTableElements(XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ID_COLUMN: {
                    this.parseJDBCStoreColumn(reader, ColumnAttribute.ID, operation.get(TableResourceDefinition.ColumnAttribute.ID.getName()).setEmptyObject());
                    break;
                }
                case DATA_COLUMN: {
                    this.parseJDBCStoreColumn(reader, ColumnAttribute.DATA, operation.get(TableResourceDefinition.ColumnAttribute.DATA.getName()).setEmptyObject());
                    break;
                }
                case TIMESTAMP_COLUMN: {
                    this.parseJDBCStoreColumn(reader, ColumnAttribute.TIMESTAMP, operation.get(TableResourceDefinition.ColumnAttribute.TIMESTAMP.getName()).setEmptyObject());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseJDBCStoreColumn(XMLExtendedStreamReader reader, ColumnAttribute columnAttribute, ModelNode column) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    readAttribute(reader, i, column, columnAttribute.getColumnName());
                    break;
                }
                case TYPE: {
                    readAttribute(reader, i, column, columnAttribute.getColumnType());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void removeStoreOperations(PathAddress storeAddress, Map<PathAddress, ModelNode> operations) {
        operations.remove(storeAddress.append(StoreWriteResourceDefinition.WILDCARD_PATH));
    }

    private void parseStoreAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation) throws XMLStreamException {
        XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(index));
        switch (attribute) {
            case SHARED: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.SHARED);
                break;
            }
            case PRELOAD: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.PRELOAD);
                break;
            }
            case PASSIVATION: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.PASSIVATION);
                break;
            }
            case FETCH_STATE: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.FETCH_STATE);
                break;
            }
            case PURGE: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.PURGE);
                break;
            }
            case SINGLETON: {
                readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.SINGLETON);
                break;
            }
            case MAX_BATCH_SIZE: {
                if (this.schema.since(InfinispanSchema.VERSION_5_0)) {
                    readAttribute(reader, index, operation, StoreResourceDefinition.Attribute.MAX_BATCH_SIZE);
                    break;
                }
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private void parseStoreElement(XMLExtendedStreamReader reader, PathAddress storeAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ModelNode operation = operations.get(storeAddress.getParent().append(StoreResourceDefinition.WILDCARD_PATH));

        XMLElement element = XMLElement.forName(reader.getLocalName());
        switch (element) {
            case PROPERTY: {
                ParseUtils.requireSingleAttribute(reader, XMLAttribute.NAME.getLocalName());
                readElement(reader, operation, StoreResourceDefinition.Attribute.PROPERTIES);
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
        operations.put(storeAddress.append(StoreWriteResourceDefinition.WILDCARD_PATH), operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case FLUSH_LOCK_TIMEOUT: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case MODIFICATION_QUEUE_SIZE: {
                    readAttribute(reader, i, operation, StoreWriteBehindResourceDefinition.Attribute.MODIFICATION_QUEUE_SIZE);
                    break;
                }
                case SHUTDOWN_TIMEOUT: {
                    if (this.schema.since(InfinispanSchema.VERSION_4_0)) {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                    ROOT_LOGGER.attributeDeprecated(attribute.getLocalName(), reader.getLocalName());
                    break;
                }
                case THREAD_POOL_SIZE: {
                    readAttribute(reader, i, operation, StoreWriteBehindResourceDefinition.Attribute.THREAD_POOL_SIZE);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private <P extends ThreadPoolDefinition & ResourceDefinition> void parseThreadPool(P pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = parentAddress.append(pool.getPathElement());
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MIN_THREADS: {
                    if (pool.getMinThreads() != null) {
                        readAttribute(reader, i, operation, pool.getMinThreads());
                    }
                    break;
                }
                case MAX_THREADS: {
                    readAttribute(reader, i, operation, pool.getMaxThreads());
                    break;
                }
                case QUEUE_LENGTH: {
                    if (pool.getQueueLength() != null) {
                        readAttribute(reader, i, operation, pool.getQueueLength());
                    }
                    break;
                }
                case KEEPALIVE_TIME: {
                    readAttribute(reader, i, operation, pool.getKeepAliveTime());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    private <P extends ScheduledThreadPoolDefinition & ResourceDefinition> void parseScheduledThreadPool(P pool, XMLExtendedStreamReader reader, PathAddress parentAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = parentAddress.append(pool.getPathElement());
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_THREADS: {
                    readAttribute(reader, i, operation, pool.getMaxThreads());
                    break;
                }
                case KEEPALIVE_TIME: {
                    readAttribute(reader, i, operation, pool.getKeepAliveTime());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        ParseUtils.requireNoContent(reader);
    }

    private void parseRemoteContainer(XMLExtendedStreamReader reader, PathAddress subsystemAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String name = require(reader, XMLAttribute.NAME);
        PathAddress address = subsystemAddress.append(RemoteCacheContainerResourceDefinition.pathElement(name));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case CONNECTION_TIMEOUT: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.CONNECTION_TIMEOUT);
                    break;
                }
                case DEFAULT_REMOTE_CLUSTER: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.DEFAULT_REMOTE_CLUSTER);
                    break;
                }
                case KEY_SIZE_ESTIMATE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.KEY_SIZE_ESTIMATE);
                    break;
                }
                case MAX_RETRIES: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.MAX_RETRIES);
                    break;
                }
                case MODULE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.MODULE);
                    break;
                }
                case PROTOCOL_VERSION: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.PROTOCOL_VERSION);
                    break;
                }
                case SOCKET_TIMEOUT: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.SOCKET_TIMEOUT);
                    break;
                }
                case TCP_NO_DELAY: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.TCP_NO_DELAY);
                    break;
                }
                case TCP_KEEP_ALIVE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.TCP_KEEP_ALIVE);
                    break;
                }
                case VALUE_SIZE_ESTIMATE: {
                    readAttribute(reader, i, operation, RemoteCacheContainerResourceDefinition.Attribute.VALUE_SIZE_ESTIMATE);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case ASYNC_THREAD_POOL: {
                    this.parseThreadPool(ThreadPoolResourceDefinition.CLIENT, reader, address, operations);
                    break;
                }
                case CONNECTION_POOL: {
                    this.parseConnectionPool(reader, address, operations);
                    break;
                }
                case INVALIDATION_NEAR_CACHE: {
                    this.parseInvalidationNearCache(reader, address, operations);
                    break;
                }
                case REMOTE_CLUSTERS: {
                    this.parseRemoteClusters(reader, address, operations);
                    break;
                }
                case SECURITY: {
                    this.parseRemoteCacheContainerSecurity(reader, address, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseInvalidationNearCache(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = cacheAddress.append(InvalidationNearCacheResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case MAX_ENTRIES: {
                    readAttribute(reader, i, operation, InvalidationNearCacheResourceDefinition.Attribute.MAX_ENTRIES);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseConnectionPool(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = cacheAddress.append(ConnectionPoolResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case EXHAUSTED_ACTION: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.EXHAUSTED_ACTION);
                    break;
                }
                case MAX_ACTIVE: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE);
                    break;
                }
                case MAX_WAIT: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MAX_WAIT);
                    break;
                }
                case MIN_EVICTABLE_IDLE_TIME: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MIN_EVICTABLE_IDLE_TIME);
                    break;
                }
                case MIN_IDLE: {
                    readAttribute(reader, i, operation, ConnectionPoolResourceDefinition.Attribute.MIN_IDLE);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseRemoteClusters(XMLExtendedStreamReader reader, PathAddress containerAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            XMLElement element = XMLElement.forName(reader.getLocalName());
            switch (element) {
                case REMOTE_CLUSTER: {
                    this.parseRemoteCluster(reader, containerAddress, operations);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }

    private void parseRemoteCluster(XMLExtendedStreamReader reader, PathAddress clustersAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        String remoteCluster = require(reader, XMLAttribute.NAME);
        PathAddress address = clustersAddress.append(RemoteClusterResourceDefinition.pathElement(remoteCluster));
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    // Already parsed
                    break;
                }
                case SOCKET_BINDINGS: {
                    readAttribute(reader, i, operation, RemoteClusterResourceDefinition.Attribute.SOCKET_BINDINGS);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private void parseRemoteCacheContainerSecurity(XMLExtendedStreamReader reader, PathAddress cacheAddress, Map<PathAddress, ModelNode> operations) throws XMLStreamException {
        PathAddress address = cacheAddress.append(SecurityResourceDefinition.PATH);
        ModelNode operation = Util.createAddOperation(address);
        operations.put(address, operation);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            XMLAttribute attribute = XMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case SSL_CONTEXT: {
                    readAttribute(reader, i, operation, SecurityResourceDefinition.Attribute.SSL_CONTEXT);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
    }

    private static String require(XMLExtendedStreamReader reader, XMLAttribute attribute) throws XMLStreamException {
        String value = reader.getAttributeValue(null, attribute.getLocalName());
        if (value == null) {
            throw ParseUtils.missingRequired(reader, attribute.getLocalName());
        }
        return value;
    }

    private static ModelNode readAttribute(XMLExtendedStreamReader reader, int index, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        return definition.getParser().parse(definition, reader.getAttributeValue(index), reader);
    }

    private static void readAttribute(XMLExtendedStreamReader reader, int index, ModelNode operation, Attribute attribute) throws XMLStreamException {
        setAttribute(reader, reader.getAttributeValue(index), operation, attribute);
    }

    private static void setAttribute(XMLExtendedStreamReader reader, String value, ModelNode operation, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        definition.getParser().parseAndSetParameter(definition, value, operation, reader);
    }

    private static void readElement(XMLExtendedStreamReader reader, ModelNode operation, Attribute attribute) throws XMLStreamException {
        AttributeDefinition definition = attribute.getDefinition();
        AttributeParser parser = definition.getParser();
        if (parser.isParseAsElement()) {
            parser.parseElement(definition, reader, operation);
        } else {
            parser.parseAndSetParameter(definition, reader.getElementText(), operation, reader);
        }
    }
}
