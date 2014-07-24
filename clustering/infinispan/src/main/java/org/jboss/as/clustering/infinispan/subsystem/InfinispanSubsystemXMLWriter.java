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

import javax.xml.stream.XMLStreamException;

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
    public static final XMLElementWriter<SubsystemMarshallingContext> INSTANCE = new InfinispanSubsystemXMLWriter();

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementWriter#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter, java.lang.Object)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(InfinispanSchema.CURRENT.getNamespaceUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            if (model.hasDefined(CacheContainerResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property entry: model.get(CacheContainerResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {

                    String containerName = entry.getName();
                    ModelNode container = entry.getValue();

                    writer.writeStartElement(Element.CACHE_CONTAINER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), containerName);

                    CacheContainerResourceDefinition.DEFAULT_CACHE.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.EVICTION_EXECUTOR.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.JNDI_NAME.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.LISTENER_EXECUTOR.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.START.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.MODULE.marshallAsAttribute(container, writer);
                    CacheContainerResourceDefinition.STATISTICS_ENABLED.marshallAsAttribute(container, writer);

                    CacheContainerResourceDefinition.ALIASES.marshallAsElement(container, writer);

                    if (container.hasDefined(TransportResourceDefinition.PATH.getKey())) {
                        writer.writeStartElement(Element.TRANSPORT.getLocalName());
                        ModelNode transport = container.get(TransportResourceDefinition.PATH.getKeyValuePair());
                        TransportResourceDefinition.STACK.marshallAsAttribute(transport,false,  writer);
                        TransportResourceDefinition.CLUSTER.marshallAsAttribute(transport,false,  writer);
                        TransportResourceDefinition.EXECUTOR.marshallAsAttribute(transport,false,  writer);
                        TransportResourceDefinition.LOCK_TIMEOUT.marshallAsAttribute(transport,false,  writer);
                        writer.writeEndElement();
                    }

                    // write any existent cache types
                    if (container.hasDefined(LocalCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(LocalCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.LOCAL_CACHE.getLocalName());

                            writeCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(InvalidationCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.INVALIDATION_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(ReplicatedCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.REPLICATED_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(DistributedCacheResourceDefinition.WILDCARD_PATH.getKey())) {
                        for (Property property : container.get(DistributedCacheResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.DISTRIBUTED_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);

                            DistributedCacheResourceDefinition.OWNERS.marshallAsAttribute(cache, writer);
                            DistributedCacheResourceDefinition.SEGMENTS.marshallAsAttribute(cache, writer);
                            DistributedCacheResourceDefinition.L1_LIFESPAN.marshallAsAttribute(cache, writer);
                            DistributedCacheResourceDefinition.CAPACITY_FACTOR.marshallAsAttribute(cache, writer);
                            DistributedCacheResourceDefinition.CONSISTENT_HASH_STRATEGY.marshallAsAttribute(cache, writer);

                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }
                    writer.writeEndElement();
                }
            }
        }
        writer.writeEndElement();
    }

    private static void writeCacheAttributes(XMLExtendedStreamWriter writer, String name, ModelNode cache) throws XMLStreamException {
        writer.writeAttribute(Attribute.NAME.getLocalName(), name);

        CacheResourceDefinition.START.marshallAsAttribute(cache, writer);
        CacheResourceDefinition.JNDI_NAME.marshallAsAttribute(cache, writer);
        CacheResourceDefinition.MODULE.marshallAsAttribute(cache, writer);
        CacheResourceDefinition.STATISTICS_ENABLED.marshallAsAttribute(cache, writer);
    }

    private static void writeClusteredCacheAttributes(XMLExtendedStreamWriter writer, String name, ModelNode cache) throws XMLStreamException {

        writeCacheAttributes(writer, name, cache);

        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING.marshallAsAttribute(cache, writer);
        ClusteredCacheResourceDefinition.MODE.marshallAsAttribute(cache, writer);
        ClusteredCacheResourceDefinition.QUEUE_SIZE.marshallAsAttribute(cache, writer);
        ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL.marshallAsAttribute(cache, writer);
        ClusteredCacheResourceDefinition.REMOTE_TIMEOUT.marshallAsAttribute(cache, writer);
    }

    private static void writeCacheElements(XMLExtendedStreamWriter writer, ModelNode cache) throws XMLStreamException {

        if (cache.hasDefined(LockingResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.LOCKING.getLocalName());
            ModelNode locking = cache.get(LockingResourceDefinition.PATH.getKeyValuePair());
            LockingResourceDefinition.ISOLATION.marshallAsAttribute(locking, writer);
            LockingResourceDefinition.STRIPING.marshallAsAttribute(locking, writer);
            LockingResourceDefinition.ACQUIRE_TIMEOUT.marshallAsAttribute(locking, writer);
            LockingResourceDefinition.CONCURRENCY_LEVEL.marshallAsAttribute(locking, writer);
            writer.writeEndElement();
        }

        if (cache.hasDefined(TransactionResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.TRANSACTION.getLocalName());
            ModelNode transaction = cache.get(TransactionResourceDefinition.PATH.getKeyValuePair());
            TransactionResourceDefinition.STOP_TIMEOUT.marshallAsAttribute(transaction, writer);
            TransactionResourceDefinition.MODE.marshallAsAttribute(transaction, writer);
            TransactionResourceDefinition.LOCKING.marshallAsAttribute(transaction, writer);
            writer.writeEndElement();
        }

        if (cache.hasDefined(EvictionResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.EVICTION.getLocalName());
            ModelNode eviction = cache.get(EvictionResourceDefinition.PATH.getKeyValuePair());
            EvictionResourceDefinition.STRATEGY.marshallAsAttribute(eviction, writer);
            EvictionResourceDefinition.MAX_ENTRIES.marshallAsAttribute(eviction, writer);
            writer.writeEndElement();
        }

        if (cache.hasDefined(ExpirationResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.EXPIRATION.getLocalName());
            ModelNode expiration = cache.get(ExpirationResourceDefinition.PATH.getKeyValuePair());
            ExpirationResourceDefinition.MAX_IDLE.marshallAsAttribute(expiration, writer);
            ExpirationResourceDefinition.LIFESPAN.marshallAsAttribute(expiration, writer);
            ExpirationResourceDefinition.INTERVAL.marshallAsAttribute(expiration, writer);
            writer.writeEndElement();
        }

        if (cache.hasDefined(StateTransferResourceDefinition.PATH.getKey())) {
            ModelNode stateTransfer = cache.get(StateTransferResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.STATE_TRANSFER.getLocalName());
            StateTransferResourceDefinition.ENABLED.marshallAsAttribute(stateTransfer, writer);
            StateTransferResourceDefinition.TIMEOUT.marshallAsAttribute(stateTransfer, writer);
            StateTransferResourceDefinition.CHUNK_SIZE.marshallAsAttribute(stateTransfer, writer);
            writer.writeEndElement();
        }

        if (cache.hasDefined(CustomStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(CustomStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.STORE.getLocalName());
            CustomStoreResourceDefinition.CLASS.marshallAsAttribute(store, writer);
            writeStoreAttributes(writer, store);
            writeStoreElements(writer, store);
            writer.writeEndElement();
        }

        if (cache.hasDefined(FileStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(FileStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.FILE_STORE.getLocalName());
            writeStoreAttributes(writer, store);
            FileStoreResourceDefinition.RELATIVE_TO.marshallAsAttribute(store, writer);
            FileStoreResourceDefinition.RELATIVE_PATH.marshallAsAttribute(store, writer);
            writeStoreElements(writer, store);
            writer.writeEndElement();
        }

        if (cache.hasDefined(StringKeyedJDBCStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(StringKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.STRING_KEYED_JDBC_STORE.getLocalName());
            writeJDBCStoreAttributes(writer, store);
            writeStoreElements(writer, store);
            writeJDBCStoreTable(writer, Element.STRING_KEYED_TABLE, store, ModelKeys.STRING_KEYED_TABLE);
            writer.writeEndElement();
        }

        if (cache.hasDefined(BinaryKeyedJDBCStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(BinaryKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.BINARY_KEYED_JDBC_STORE.getLocalName());
            writeJDBCStoreAttributes(writer, store);
            writeStoreElements(writer, store);
            writeJDBCStoreTable(writer, Element.BINARY_KEYED_TABLE, store, ModelKeys.BINARY_KEYED_TABLE);
            writer.writeEndElement();
        }

        if (cache.hasDefined(MixedKeyedJDBCStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(MixedKeyedJDBCStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.MIXED_KEYED_JDBC_STORE.getLocalName());
            writeJDBCStoreAttributes(writer, store);
            writeStoreElements(writer, store);
            writeJDBCStoreTable(writer, Element.STRING_KEYED_TABLE, store, ModelKeys.STRING_KEYED_TABLE);
            writeJDBCStoreTable(writer, Element.BINARY_KEYED_TABLE, store, ModelKeys.BINARY_KEYED_TABLE);
            writer.writeEndElement();
        }

        if (cache.hasDefined(RemoteStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(RemoteStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.REMOTE_STORE.getLocalName());
            writeStoreAttributes(writer, store);
            RemoteStoreResourceDefinition.CACHE.marshallAsAttribute(store, writer);
            RemoteStoreResourceDefinition.SOCKET_TIMEOUT.marshallAsAttribute(store, writer);
            RemoteStoreResourceDefinition.TCP_NO_DELAY.marshallAsAttribute(store, writer);
            writeStoreElements(writer, store);
            for (ModelNode remoteServer: store.get(ModelKeys.REMOTE_SERVERS).asList()) {
                writer.writeStartElement(Element.REMOTE_SERVER.getLocalName());
                RemoteStoreResourceDefinition.OUTBOUND_SOCKET_BINDING.marshallAsAttribute(remoteServer, writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (cache.get(ModelKeys.INDEXING).isDefined() || cache.get(ModelKeys.INDEXING_PROPERTIES).isDefined()) {
            writer.writeStartElement(Element.INDEXING.getLocalName());
            CacheResourceDefinition.INDEXING.marshallAsAttribute(cache, writer);
            CacheResourceDefinition.INDEXING_PROPERTIES.marshallAsElement(cache, writer);
            writer.writeEndElement();
        }

        if (cache.hasDefined(BackupSiteResourceDefinition.WILDCARD_PATH.getKey())) {
            writer.writeStartElement(Element.BACKUPS.getLocalName());
            for (Property property: cache.get(BackupSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                writer.writeStartElement(Element.BACKUP.getLocalName());
                writer.writeAttribute(Attribute.SITE.getLocalName(), property.getName());
                ModelNode backup = property.getValue();
                BackupSiteResourceDefinition.FAILURE_POLICY.marshallAsAttribute(backup, writer);
                BackupSiteResourceDefinition.STRATEGY.marshallAsAttribute(backup, writer);
                BackupSiteResourceDefinition.REPLICATION_TIMEOUT.marshallAsAttribute(backup, writer);
                BackupSiteResourceDefinition.ENABLED.marshallAsAttribute(backup, writer);
                if (backup.hasDefined(ModelKeys.TAKE_BACKUP_OFFLINE_AFTER_FAILURES) || backup.hasDefined(ModelKeys.TAKE_BACKUP_OFFLINE_MIN_WAIT)) {
                    writer.writeStartElement(Element.TAKE_OFFLINE.getLocalName());
                    BackupSiteResourceDefinition.TAKE_OFFLINE_AFTER_FAILURES.marshallAsAttribute(backup, writer);
                    BackupSiteResourceDefinition.TAKE_OFFLINE_MIN_WAIT.marshallAsAttribute(backup, writer);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (cache.hasDefined(BackupForResourceDefinition.PATH.getKey())) {
            ModelNode backupFor = cache.get(BackupForResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.BACKUP_FOR.getLocalName());
            BackupForResourceDefinition.REMOTE_CACHE.marshallAsAttribute(backupFor, writer);
            BackupForResourceDefinition.REMOTE_SITE.marshallAsAttribute(backupFor, writer);
            writer.writeEndElement();
        }
    }

    private static void writeJDBCStoreTable(XMLExtendedStreamWriter writer, Element element, ModelNode store, String key) throws XMLStreamException {
        if (store.hasDefined(key)) {
            ModelNode table = store.get(key);
            writer.writeStartElement(element.getLocalName());
            JDBCStoreResourceDefinition.PREFIX.marshallAsAttribute(table, writer);
            JDBCStoreResourceDefinition.BATCH_SIZE.marshallAsAttribute(table, writer);
            JDBCStoreResourceDefinition.FETCH_SIZE.marshallAsAttribute(table, writer);
            writeJDBCStoreColumn(writer, Element.ID_COLUMN, table, ModelKeys.ID_COLUMN);
            writeJDBCStoreColumn(writer, Element.DATA_COLUMN, table, ModelKeys.DATA_COLUMN);
            writeJDBCStoreColumn(writer, Element.TIMESTAMP_COLUMN, table, ModelKeys.TIMESTAMP_COLUMN);
            writer.writeEndElement();
        }
    }

    private static void writeJDBCStoreColumn(XMLExtendedStreamWriter writer, Element element, ModelNode table, String key) throws XMLStreamException {
        if (table.hasDefined(key)) {
            ModelNode column = table.get(key);
            writer.writeStartElement(element.getLocalName());
            JDBCStoreResourceDefinition.COLUMN_NAME.marshallAsAttribute(column, writer);
            JDBCStoreResourceDefinition.COLUMN_TYPE.marshallAsAttribute(column, writer);
            writer.writeEndElement();
        }
    }

    private static void writeStoreAttributes(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        StoreResourceDefinition.SHARED.marshallAsAttribute(store, writer);
        StoreResourceDefinition.PRELOAD.marshallAsAttribute(store, writer);
        StoreResourceDefinition.PASSIVATION.marshallAsAttribute(store, writer);
        StoreResourceDefinition.FETCH_STATE.marshallAsAttribute(store, writer);
        StoreResourceDefinition.PURGE.marshallAsAttribute(store, writer);
        StoreResourceDefinition.SINGLETON.marshallAsAttribute(store, writer);
    }

    private static void writeJDBCStoreAttributes(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        writeStoreAttributes(writer, store);

        JDBCStoreResourceDefinition.DATA_SOURCE.marshallAsAttribute(store, writer);
        JDBCStoreResourceDefinition.DIALECT.marshallAsAttribute(store, writer);
    }

    private static void writeStoreElements(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(StoreWriteBehindResourceDefinition.PATH.getKey())) {
            ModelNode writeBehind = store.get(StoreWriteBehindResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.WRITE_BEHIND.getLocalName());
            StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT.marshallAsAttribute(writeBehind, writer);
            StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE.marshallAsAttribute(writeBehind, writer);
            StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT.marshallAsAttribute(writeBehind, writer);
            StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE.marshallAsAttribute(writeBehind, writer);
            writer.writeEndElement();
        }
        if (store.hasDefined(StorePropertyResourceDefinition.WILDCARD_PATH.getKey())) {
            // the format of the property elements
            //  "property" => {
            //       "relative-to" => {"value" => "fred"},
            //   }
            for (Property property: store.get(StorePropertyResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                Property complexValue = property.getValue().asProperty();
                writer.writeCharacters(complexValue.getValue().asString());
                writer.writeEndElement();
            }
        }
    }
}
