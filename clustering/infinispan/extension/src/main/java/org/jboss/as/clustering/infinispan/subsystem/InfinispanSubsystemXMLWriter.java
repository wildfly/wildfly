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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.InvalidationNearCacheResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * XML writer for current Infinispan subsystem schema version.
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Tristan Tarrant
 */
public class InfinispanSubsystemXMLWriter implements XMLElementWriter<SubsystemMarshallingContext> {

    @SuppressWarnings("deprecation")
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(InfinispanSchema.CURRENT.getNamespaceUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            if (model.hasDefined(CacheContainerResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property entry: model.get(CacheContainerResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {

                    String containerName = entry.getName();
                    ModelNode container = entry.getValue();

                    writer.writeStartElement(XMLElement.CACHE_CONTAINER.getLocalName());
                    writer.writeAttribute(XMLAttribute.NAME.getLocalName(), containerName);

                    writeAttributes(writer, container, EnumSet.allOf(CacheContainerResourceDefinition.Attribute.class));
                    writeAttributes(writer, container, EnumSet.allOf(CacheContainerResourceDefinition.ExecutorAttribute.class));

                    if (container.hasDefined(JGroupsTransportResourceDefinition.PATH.getKeyValuePair())) {
                        writer.writeStartElement(XMLElement.TRANSPORT.getLocalName());
                        ModelNode transport = container.get(JGroupsTransportResourceDefinition.PATH.getKeyValuePair());
                        writeAttributes(writer, transport, EnumSet.allOf(JGroupsTransportResourceDefinition.Attribute.class));
                        writeAttributes(writer, transport, EnumSet.allOf(JGroupsTransportResourceDefinition.ExecutorAttribute.class));
                        writer.writeEndElement();
                    }

                    // write any configured thread pools
                    if (container.hasDefined(ThreadPoolResourceDefinition.WILDCARD_PATH.getKey())) {
                        writeThreadPoolElements(XMLElement.ASYNC_OPERATIONS_THREAD_POOL, ThreadPoolResourceDefinition.ASYNC_OPERATIONS, writer, container);
                        writeThreadPoolElements(XMLElement.LISTENER_THREAD_POOL, ThreadPoolResourceDefinition.LISTENER, writer, container);
                        writeThreadPoolElements(XMLElement.REMOTE_COMMAND_THREAD_POOL, ThreadPoolResourceDefinition.REMOTE_COMMAND, writer, container);
                        writeThreadPoolElements(XMLElement.STATE_TRANSFER_THREAD_POOL, ThreadPoolResourceDefinition.STATE_TRANSFER, writer, container);
                        writeThreadPoolElements(XMLElement.TRANSPORT_THREAD_POOL, ThreadPoolResourceDefinition.TRANSPORT, writer, container);
                        writeScheduledThreadPoolElements(XMLElement.EXPIRATION_THREAD_POOL, ScheduledThreadPoolResourceDefinition.EXPIRATION, writer, container);
                        writeScheduledThreadPoolElements(XMLElement.PERSISTENCE_THREAD_POOL, ThreadPoolResourceDefinition.PERSISTENCE, writer, container);
                    }

                    // write any existent cache types
                    if (container.hasDefined(LocalCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(LocalCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(XMLElement.LOCAL_CACHE.getLocalName());

                            writeCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(XMLElement.INVALIDATION_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(XMLElement.REPLICATED_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(DistributedCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(DistributedCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(XMLElement.DISTRIBUTED_CACHE.getLocalName());

                            writeSegmentedCacheAttributes(writer, property.getName(), cache);
                            writeAttributes(writer, cache, EnumSet.allOf(DistributedCacheResourceDefinition.Attribute.class));

                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(ScatteredCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(ScatteredCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(XMLElement.SCATTERED_CACHE.getLocalName());

                            writeSegmentedCacheAttributes(writer, property.getName(), cache);
                            writeAttributes(writer, cache, EnumSet.allOf(ScatteredCacheResourceDefinition.Attribute.class));

                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }
                    writer.writeEndElement();
                }
            }

            if (model.hasDefined(RemoteCacheContainerResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property entry : model.get(RemoteCacheContainerResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {

                    String remoteContainerName = entry.getName();
                    ModelNode remoteContainer = entry.getValue();

                    writer.writeStartElement(XMLElement.REMOTE_CACHE_CONTAINER.getLocalName());
                    writer.writeAttribute(XMLAttribute.NAME.getLocalName(), remoteContainerName);

                    writeAttributes(writer, remoteContainer, EnumSet.allOf(RemoteCacheContainerResourceDefinition.Attribute.class));

                    writeThreadPoolElements(XMLElement.ASYNC_THREAD_POOL, ThreadPoolResourceDefinition.CLIENT, writer, remoteContainer);

                    ModelNode connectionPool = remoteContainer.get(ConnectionPoolResourceDefinition.PATH.getKeyValuePair());
                    EnumSet<ConnectionPoolResourceDefinition.Attribute> attributes = EnumSet.allOf(ConnectionPoolResourceDefinition.Attribute.class);
                    if (hasDefined(connectionPool, attributes)) {
                        writer.writeStartElement(XMLElement.CONNECTION_POOL.getLocalName());
                        writeAttributes(writer, connectionPool, attributes);
                        writer.writeEndElement();
                    }

                    if (remoteContainer.hasDefined(InvalidationNearCacheResourceDefinition.PATH.getKeyValuePair())) {
                        writer.writeStartElement(XMLElement.INVALIDATION_NEAR_CACHE.getLocalName());
                        ModelNode nearCache = remoteContainer.get(InvalidationNearCacheResourceDefinition.PATH.getKeyValuePair());
                        writeAttributes(writer, nearCache, EnumSet.allOf(InvalidationNearCacheResourceDefinition.Attribute.class));
                        writer.writeEndElement();
                    }

                    writer.writeStartElement(XMLElement.REMOTE_CLUSTERS.getLocalName());

                    for (Property clusterEntry : remoteContainer.get(RemoteClusterResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                        writer.writeStartElement(XMLElement.REMOTE_CLUSTER.getLocalName());

                        String remoteClusterName = clusterEntry.getName();
                        ModelNode remoteCluster = clusterEntry.getValue();

                        writer.writeAttribute(XMLAttribute.NAME.getLocalName(), remoteClusterName);
                        writeAttributes(writer, remoteCluster, RemoteClusterResourceDefinition.Attribute.class);
                        writer.writeEndElement();
                    }

                    writer.writeEndElement(); // </remote-clusters>

                    ModelNode securityModel = remoteContainer.get(SecurityResourceDefinition.PATH.getKeyValuePair());
                    EnumSet<SecurityResourceDefinition.Attribute> securityAttributes = EnumSet.allOf(SecurityResourceDefinition.Attribute.class);
                    if (hasDefined(securityModel, securityAttributes)) {
                        writer.writeStartElement(XMLElement.SECURITY.getLocalName());
                        writeAttributes(writer, securityModel, securityAttributes);
                        writer.writeEndElement();
                    }

                    writer.writeEndElement(); // </remote-cache-container>
                }
            }
        }
        writer.writeEndElement();
    }

    private static void writeCacheAttributes(XMLExtendedStreamWriter writer, String name, ModelNode cache) throws XMLStreamException {
        writer.writeAttribute(XMLAttribute.NAME.getLocalName(), name);
        writeAttributes(writer, cache, EnumSet.allOf(CacheResourceDefinition.Attribute.class));
    }

    private static void writeClusteredCacheAttributes(XMLExtendedStreamWriter writer, String name, ModelNode cache) throws XMLStreamException {
        writeCacheAttributes(writer, name, cache);
        writeAttributes(writer, cache, ClusteredCacheResourceDefinition.Attribute.class);
    }

    private static void writeSegmentedCacheAttributes(XMLExtendedStreamWriter writer, String name, ModelNode cache) throws XMLStreamException {
        writeClusteredCacheAttributes(writer, name, cache);
        writeAttributes(writer, cache, SegmentedCacheResourceDefinition.Attribute.class);
    }

    @SuppressWarnings("deprecation")
    private static void writeCacheElements(XMLExtendedStreamWriter writer, ModelNode cache) throws XMLStreamException {

        if (cache.hasDefined(LockingResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode locking = cache.get(LockingResourceDefinition.PATH.getKeyValuePair());
            Set<LockingResourceDefinition.Attribute> attributes = EnumSet.allOf(LockingResourceDefinition.Attribute.class);
            if (hasDefined(locking, attributes)) {
                writer.writeStartElement(XMLElement.LOCKING.getLocalName());
                writeAttributes(writer, locking, attributes);
                writer.writeEndElement();
            }
        }

        if (cache.hasDefined(TransactionResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode transaction = cache.get(TransactionResourceDefinition.PATH.getKeyValuePair());
            Set<TransactionResourceDefinition.Attribute> attributes = EnumSet.allOf(TransactionResourceDefinition.Attribute.class);
            if (hasDefined(transaction, attributes)) {
                writer.writeStartElement(XMLElement.TRANSACTION.getLocalName());
                writeAttributes(writer, transaction, attributes);
                writer.writeEndElement();
            }
        }

        if (cache.hasDefined(ObjectMemoryResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode memory = cache.get(ObjectMemoryResourceDefinition.PATH.getKeyValuePair());
            Set<ObjectMemoryResourceDefinition.Attribute> attributes = EnumSet.allOf(MemoryResourceDefinition.Attribute.class);
            if (hasDefined(memory, attributes)) {
                writer.writeStartElement(XMLElement.OBJECT_MEMORY.getLocalName());
                writeAttributes(writer, memory, attributes);
                writer.writeEndElement();
            }
        } else if (cache.hasDefined(BinaryMemoryResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode memory = cache.get(BinaryMemoryResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.BINARY_MEMORY.getLocalName());
            writeAttributes(writer, memory, MemoryResourceDefinition.Attribute.class);
            writeAttributes(writer, memory, BinaryMemoryResourceDefinition.Attribute.class);
            writer.writeEndElement();
        } else if (cache.hasDefined(OffHeapMemoryResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode memory = cache.get(OffHeapMemoryResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.OFF_HEAP_MEMORY.getLocalName());
            writeAttributes(writer, memory, MemoryResourceDefinition.Attribute.class);
            writeAttributes(writer, memory, BinaryMemoryResourceDefinition.Attribute.class);
            writeAttributes(writer, memory, OffHeapMemoryResourceDefinition.Attribute.class);
            writer.writeEndElement();
        }

        if (cache.hasDefined(ExpirationResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode expiration = cache.get(ExpirationResourceDefinition.PATH.getKeyValuePair());
            Set<ExpirationResourceDefinition.Attribute> attributes = EnumSet.allOf(ExpirationResourceDefinition.Attribute.class);
            if (hasDefined(expiration, attributes)) {
                writer.writeStartElement(XMLElement.EXPIRATION.getLocalName());
                writeAttributes(writer, expiration, attributes);
                writer.writeEndElement();
            }
        }

        Set<StoreResourceDefinition.Attribute> storeAttributes = EnumSet.complementOf(EnumSet.of(StoreResourceDefinition.Attribute.PROPERTIES));

        if (cache.hasDefined(CustomStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode store = cache.get(CustomStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.STORE.getLocalName());
            writeAttributes(writer, store, CustomStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, JDBCStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, storeAttributes);
            writeStoreElements(writer, store);
            writer.writeEndElement();
        }

        if (cache.hasDefined(FileStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode store = cache.get(FileStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.FILE_STORE.getLocalName());
            writeAttributes(writer, store, FileStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, storeAttributes);
            writeStoreElements(writer, store);
            writer.writeEndElement();
        }

        if (cache.hasDefined(BinaryKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode store = cache.get(BinaryKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.BINARY_KEYED_JDBC_STORE.getLocalName());
            writeAttributes(writer, store, JDBCStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, storeAttributes);
            writeStoreElements(writer, store);
            writeJDBCStoreTable(writer, XMLElement.BINARY_KEYED_TABLE, store, BinaryTableResourceDefinition.PATH, BinaryTableResourceDefinition.Attribute.PREFIX);
            writer.writeEndElement();
        }

        if (cache.hasDefined(StringKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode store = cache.get(StringKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.JDBC_STORE.getLocalName());
            writeAttributes(writer, store, JDBCStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, storeAttributes);
            writeStoreElements(writer, store);
            writeJDBCStoreTable(writer, XMLElement.TABLE, store, StringTableResourceDefinition.PATH, StringTableResourceDefinition.Attribute.PREFIX);
            writer.writeEndElement();
        }

        if (cache.hasDefined(MixedKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode store = cache.get(MixedKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.MIXED_KEYED_JDBC_STORE.getLocalName());
            writeAttributes(writer, store, JDBCStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, storeAttributes);
            writeStoreElements(writer, store);
            writeJDBCStoreTable(writer, XMLElement.BINARY_KEYED_TABLE, store, BinaryTableResourceDefinition.PATH, BinaryTableResourceDefinition.Attribute.PREFIX);
            writeJDBCStoreTable(writer, XMLElement.STRING_KEYED_TABLE, store, StringTableResourceDefinition.PATH, StringTableResourceDefinition.Attribute.PREFIX);
            writer.writeEndElement();
        }

        if (cache.hasDefined(RemoteStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode store = cache.get(RemoteStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.REMOTE_STORE.getLocalName());
            writeAttributes(writer, store, RemoteStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, store, storeAttributes);
            writeStoreElements(writer, store);
            writer.writeEndElement();
        }

        if (cache.hasDefined(HotRodStoreResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode hotRodStore = cache.get(HotRodStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(XMLElement.HOTROD_STORE.getLocalName());
            writeAttributes(writer, hotRodStore, HotRodStoreResourceDefinition.Attribute.class);
            writeAttributes(writer, hotRodStore, storeAttributes);
            writeStoreElements(writer, hotRodStore);
            writer.writeEndElement();
        }

        if (cache.hasDefined(PartitionHandlingResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode partitionHandling = cache.get(PartitionHandlingResourceDefinition.PATH.getKeyValuePair());
            EnumSet<PartitionHandlingResourceDefinition.Attribute> attributes = EnumSet.allOf(PartitionHandlingResourceDefinition.Attribute.class);
            if (hasDefined(partitionHandling, attributes)) {
                writer.writeStartElement(XMLElement.PARTITION_HANDLING.getLocalName());
                writeAttributes(writer, partitionHandling, attributes);
                writer.writeEndElement();
            }
        }

        if (cache.hasDefined(StateTransferResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode stateTransfer = cache.get(StateTransferResourceDefinition.PATH.getKeyValuePair());
            EnumSet<StateTransferResourceDefinition.Attribute> attributes = EnumSet.allOf(StateTransferResourceDefinition.Attribute.class);
            if (hasDefined(stateTransfer, attributes)) {
                writer.writeStartElement(XMLElement.STATE_TRANSFER.getLocalName());
                writeAttributes(writer, stateTransfer, attributes);
                writer.writeEndElement();
            }
        }

        if (cache.hasDefined(BackupsResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode backups = cache.get(BackupsResourceDefinition.PATH.getKeyValuePair());
            if (backups.hasDefined(BackupResourceDefinition.WILDCARD_PATH.getKey())) {
                writer.writeStartElement(XMLElement.BACKUPS.getLocalName());
                for (Property property: backups.get(BackupResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    writer.writeStartElement(XMLElement.BACKUP.getLocalName());
                    writer.writeAttribute(XMLAttribute.SITE.getLocalName(), property.getName());
                    ModelNode backup = property.getValue();
                    writeAttributes(writer, backup, EnumSet.allOf(BackupResourceDefinition.Attribute.class));
                    EnumSet<BackupResourceDefinition.TakeOfflineAttribute> takeOfflineAttributes = EnumSet.allOf(BackupResourceDefinition.TakeOfflineAttribute.class);
                    if (hasDefined(backup, takeOfflineAttributes)) {
                        writer.writeStartElement(XMLElement.TAKE_OFFLINE.getLocalName());
                        writeAttributes(writer, backup, takeOfflineAttributes);
                        writer.writeEndElement();
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
    }

    private static void writeJDBCStoreTable(XMLExtendedStreamWriter writer, XMLElement element, ModelNode store, PathElement path, Attribute prefixAttribute) throws XMLStreamException {
        if (store.hasDefined(path.getKeyValuePair())) {
            ModelNode table = store.get(path.getKeyValuePair());
            writer.writeStartElement(element.getLocalName());
            writeAttributes(writer, table, TableResourceDefinition.Attribute.class);
            writeAttribute(writer, table, prefixAttribute);
            for (TableResourceDefinition.ColumnAttribute attribute : TableResourceDefinition.ColumnAttribute.values()) {
                if (table.hasDefined(attribute.getName())) {
                    ModelNode column = table.get(attribute.getName());
                    writer.writeStartElement(attribute.getDefinition().getXmlName());
                    writeAttribute(writer, column, attribute.getColumnName());
                    writeAttribute(writer, column, attribute.getColumnType());
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
    }

    private static void writeStoreElements(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(StoreWriteBehindResourceDefinition.PATH.getKeyValuePair())) {
            ModelNode writeBehind = store.get(StoreWriteBehindResourceDefinition.PATH.getKeyValuePair());
            Set<StoreWriteBehindResourceDefinition.Attribute> attributes = EnumSet.allOf(StoreWriteBehindResourceDefinition.Attribute.class);
            if (hasDefined(writeBehind, attributes)) {
                writer.writeStartElement(XMLElement.WRITE_BEHIND.getLocalName());
                writeAttributes(writer, writeBehind, attributes);
                writer.writeEndElement();
            }
        }
        writeElement(writer, store, StoreResourceDefinition.Attribute.PROPERTIES);
    }

    private static boolean hasDefined(ModelNode model, Iterable<? extends Attribute> attributes) {
        for (Attribute attribute : attributes) {
            if (model.hasDefined(attribute.getName())) return true;
        }
        return false;
    }

    private static <A extends Enum<A> & Attribute> void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Class<A> attributeClass) throws XMLStreamException {
        writeAttributes(writer, model, EnumSet.allOf(attributeClass));
    }

    private static void writeAttributes(XMLExtendedStreamWriter writer, ModelNode model, Iterable<? extends Attribute> attributes) throws XMLStreamException {
        for (Attribute attribute : attributes) {
            writeAttribute(writer, model, attribute);
        }
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getMarshaller().marshallAsAttribute(attribute.getDefinition(), model, true, writer);
    }

    private static void writeElement(XMLExtendedStreamWriter writer, ModelNode model, Attribute attribute) throws XMLStreamException {
        attribute.getDefinition().getMarshaller().marshallAsElement(attribute.getDefinition(), model, true, writer);
    }

    private static <P extends ThreadPoolDefinition & ResourceDefinition> void writeThreadPoolElements(XMLElement element, P pool, XMLExtendedStreamWriter writer, ModelNode container) throws XMLStreamException {
        if (container.get(pool.getPathElement().getKey()).hasDefined(pool.getPathElement().getValue())) {
            ModelNode threadPool = container.get(pool.getPathElement().getKeyValuePair());
            Iterable<Attribute> attributes = Arrays.asList(pool.getMinThreads(), pool.getMaxThreads(), pool.getQueueLength(), pool.getKeepAliveTime());
            if (hasDefined(threadPool, attributes)) {
                writer.writeStartElement(element.getLocalName());
                writeAttributes(writer, threadPool, attributes);
                writer.writeEndElement();
            }
        }
    }

    private static <P extends ScheduledThreadPoolDefinition & ResourceDefinition> void writeScheduledThreadPoolElements(XMLElement element, P pool, XMLExtendedStreamWriter writer, ModelNode container) throws XMLStreamException {
        if (container.get(pool.getPathElement().getKey()).hasDefined(pool.getPathElement().getValue())) {
            ModelNode threadPool = container.get(pool.getPathElement().getKeyValuePair());
            Iterable<Attribute> attributes = Arrays.asList(pool.getMaxThreads(), pool.getKeepAliveTime());
            if (hasDefined(threadPool, attributes)) {
                writer.writeStartElement(element.getLocalName());
                writeAttributes(writer, threadPool, attributes);
                writer.writeEndElement();
            }
        }
    }
}
