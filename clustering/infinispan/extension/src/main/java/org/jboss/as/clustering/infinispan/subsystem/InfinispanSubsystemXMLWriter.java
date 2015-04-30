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

import org.jboss.as.controller.AttributeDefinition;
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

                    writeAttribute(writer, container, CacheContainerResourceDefinition.DEFAULT_CACHE);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.EVICTION_EXECUTOR);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.JNDI_NAME);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.LISTENER_EXECUTOR);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.MODULE);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.STATISTICS_ENABLED);
                    writeAttribute(writer, container, CacheContainerResourceDefinition.ALIASES);

                    if (container.hasDefined(TransportResourceDefinition.PATH.getKey())) {
                        writer.writeStartElement(Element.TRANSPORT.getLocalName());
                        ModelNode transport = container.get(TransportResourceDefinition.PATH.getKeyValuePair());
                        writeAttribute(writer, transport, TransportResourceDefinition.CHANNEL);
                        writeAttribute(writer, transport, TransportResourceDefinition.EXECUTOR);
                        writeAttribute(writer, transport, TransportResourceDefinition.LOCK_TIMEOUT);
                        writer.writeEndElement();
                    }

                    // write any existent cache types
                    if (container.hasDefined(CacheType.LOCAL.pathElement().getKey())) {
                        for (Property property : container.get(CacheType.LOCAL.pathElement().getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.LOCAL_CACHE.getLocalName());

                            writeCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(CacheType.INVALIDATION.pathElement().getKey())) {
                        for (Property property : container.get(CacheType.INVALIDATION.pathElement().getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.INVALIDATION_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(CacheType.REPLICATED.pathElement().getKey())) {
                        for (Property property : container.get(CacheType.REPLICATED.pathElement().getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.REPLICATED_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);
                            writeCacheElements(writer, cache);

                            writer.writeEndElement();
                        }
                    }

                    if (container.hasDefined(CacheType.DISTRIBUTED.pathElement().getKey())) {
                        for (Property property : container.get(CacheType.DISTRIBUTED.pathElement().getKey()).asPropertyList()) {
                            ModelNode cache = property.getValue();

                            writer.writeStartElement(Element.DISTRIBUTED_CACHE.getLocalName());

                            writeClusteredCacheAttributes(writer, property.getName(), cache);

                            writeAttribute(writer, cache, DistributedCacheResourceDefinition.OWNERS);
                            writeAttribute(writer, cache, DistributedCacheResourceDefinition.SEGMENTS);
                            writeAttribute(writer, cache, DistributedCacheResourceDefinition.L1_LIFESPAN);
                            writeAttribute(writer, cache, DistributedCacheResourceDefinition.CAPACITY_FACTOR);
                            writeAttribute(writer, cache, DistributedCacheResourceDefinition.CONSISTENT_HASH_STRATEGY);

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

        writeAttribute(writer, cache, CacheResourceDefinition.JNDI_NAME);
        writeAttribute(writer, cache, CacheResourceDefinition.MODULE);
        writeAttribute(writer, cache, CacheResourceDefinition.STATISTICS_ENABLED);
    }

    private static void writeClusteredCacheAttributes(XMLExtendedStreamWriter writer, String name, ModelNode cache) throws XMLStreamException {

        writeCacheAttributes(writer, name, cache);

        writeAttribute(writer, cache, ClusteredCacheResourceDefinition.ASYNC_MARSHALLING);
        writeAttribute(writer, cache, ClusteredCacheResourceDefinition.MODE);
        writeAttribute(writer, cache, ClusteredCacheResourceDefinition.QUEUE_SIZE);
        writeAttribute(writer, cache, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL);
        writeAttribute(writer, cache, ClusteredCacheResourceDefinition.REMOTE_TIMEOUT);
    }

    private static void writeCacheElements(XMLExtendedStreamWriter writer, ModelNode cache) throws XMLStreamException {

        if (cache.hasDefined(LockingResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.LOCKING.getLocalName());
            ModelNode locking = cache.get(LockingResourceDefinition.PATH.getKeyValuePair());
            writeAttribute(writer, locking, LockingResourceDefinition.ISOLATION);
            writeAttribute(writer, locking, LockingResourceDefinition.STRIPING);
            writeAttribute(writer, locking, LockingResourceDefinition.ACQUIRE_TIMEOUT);
            writeAttribute(writer, locking, LockingResourceDefinition.CONCURRENCY_LEVEL);
            writer.writeEndElement();
        }

        if (cache.hasDefined(TransactionResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.TRANSACTION.getLocalName());
            ModelNode transaction = cache.get(TransactionResourceDefinition.PATH.getKeyValuePair());
            writeAttribute(writer, transaction, TransactionResourceDefinition.STOP_TIMEOUT);
            writeAttribute(writer, transaction, TransactionResourceDefinition.MODE);
            writeAttribute(writer, transaction, TransactionResourceDefinition.LOCKING);
            writer.writeEndElement();
        }

        if (cache.hasDefined(EvictionResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.EVICTION.getLocalName());
            ModelNode eviction = cache.get(EvictionResourceDefinition.PATH.getKeyValuePair());
            writeAttribute(writer, eviction, EvictionResourceDefinition.STRATEGY);
            writeAttribute(writer, eviction, EvictionResourceDefinition.MAX_ENTRIES);
            writer.writeEndElement();
        }

        if (cache.hasDefined(ExpirationResourceDefinition.PATH.getKey())) {
            writer.writeStartElement(Element.EXPIRATION.getLocalName());
            ModelNode expiration = cache.get(ExpirationResourceDefinition.PATH.getKeyValuePair());
            writeAttribute(writer, expiration, ExpirationResourceDefinition.MAX_IDLE);
            writeAttribute(writer, expiration, ExpirationResourceDefinition.LIFESPAN);
            writeAttribute(writer, expiration, ExpirationResourceDefinition.INTERVAL);
            writer.writeEndElement();
        }

        if (cache.hasDefined(CustomStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(CustomStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.STORE.getLocalName());
            writeAttribute(writer, store, CustomStoreResourceDefinition.CLASS);
            writeStoreAttributes(writer, store);
            writeStoreElements(writer, store);
            writer.writeEndElement();
        }

        if (cache.hasDefined(FileStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(FileStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.FILE_STORE.getLocalName());
            writeStoreAttributes(writer, store);
            writeAttribute(writer, store, FileStoreResourceDefinition.RELATIVE_TO);
            writeAttribute(writer, store, FileStoreResourceDefinition.RELATIVE_PATH);
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
            writeJDBCStoreTable(writer, Element.BINARY_KEYED_TABLE, store, ModelKeys.BINARY_KEYED_TABLE);
            writeJDBCStoreTable(writer, Element.STRING_KEYED_TABLE, store, ModelKeys.STRING_KEYED_TABLE);
            writer.writeEndElement();
        }

        if (cache.hasDefined(RemoteStoreResourceDefinition.PATH.getKey())) {
            ModelNode store = cache.get(RemoteStoreResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.REMOTE_STORE.getLocalName());
            writeStoreAttributes(writer, store);
            writeAttribute(writer, store, RemoteStoreResourceDefinition.CACHE);
            writeAttribute(writer, store, RemoteStoreResourceDefinition.SOCKET_TIMEOUT);
            writeAttribute(writer, store, RemoteStoreResourceDefinition.TCP_NO_DELAY);
            writeStoreElements(writer, store);
            for (ModelNode remoteServer: store.get(ModelKeys.REMOTE_SERVERS).asList()) {
                writer.writeStartElement(Element.REMOTE_SERVER.getLocalName());
                writeAttribute(writer, remoteServer, RemoteStoreResourceDefinition.OUTBOUND_SOCKET_BINDING);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (cache.get(ModelKeys.INDEXING).isDefined() || cache.get(ModelKeys.INDEXING_PROPERTIES).isDefined()) {
            writer.writeStartElement(Element.INDEXING.getLocalName());
            writeAttribute(writer, cache, CacheResourceDefinition.INDEXING);
            writeElement(writer, cache, CacheResourceDefinition.INDEXING_PROPERTIES);
            writer.writeEndElement();
        }

        if (cache.hasDefined(StateTransferResourceDefinition.PATH.getKey())) {
            ModelNode stateTransfer = cache.get(StateTransferResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.STATE_TRANSFER.getLocalName());
            writeAttribute(writer, stateTransfer, StateTransferResourceDefinition.ENABLED);
            writeAttribute(writer, stateTransfer, StateTransferResourceDefinition.TIMEOUT);
            writeAttribute(writer, stateTransfer, StateTransferResourceDefinition.CHUNK_SIZE);
            writer.writeEndElement();
        }

        if (cache.hasDefined(BackupSiteResourceDefinition.WILDCARD_PATH.getKey())) {
            writer.writeStartElement(Element.BACKUPS.getLocalName());
            for (Property property: cache.get(BackupSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                writer.writeStartElement(Element.BACKUP.getLocalName());
                writer.writeAttribute(Attribute.SITE.getLocalName(), property.getName());
                ModelNode backup = property.getValue();
                writeAttribute(writer, backup, BackupSiteResourceDefinition.FAILURE_POLICY);
                writeAttribute(writer, backup, BackupSiteResourceDefinition.STRATEGY);
                writeAttribute(writer, backup, BackupSiteResourceDefinition.REPLICATION_TIMEOUT);
                writeAttribute(writer, backup, BackupSiteResourceDefinition.ENABLED);
                if (backup.hasDefined(ModelKeys.TAKE_BACKUP_OFFLINE_AFTER_FAILURES) || backup.hasDefined(ModelKeys.TAKE_BACKUP_OFFLINE_MIN_WAIT)) {
                    writer.writeStartElement(Element.TAKE_OFFLINE.getLocalName());
                    writeAttribute(writer, backup, BackupSiteResourceDefinition.TAKE_OFFLINE_AFTER_FAILURES);
                    writeAttribute(writer, backup, BackupSiteResourceDefinition.TAKE_OFFLINE_MIN_WAIT);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if (cache.hasDefined(BackupForResourceDefinition.PATH.getKey())) {
            ModelNode backupFor = cache.get(BackupForResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.BACKUP_FOR.getLocalName());
            writeAttribute(writer, backupFor, BackupForResourceDefinition.REMOTE_CACHE);
            writeAttribute(writer, backupFor, BackupForResourceDefinition.REMOTE_SITE);
            writer.writeEndElement();
        }
    }

    private static void writeJDBCStoreTable(XMLExtendedStreamWriter writer, Element element, ModelNode store, String key) throws XMLStreamException {
        if (store.hasDefined(key)) {
            ModelNode table = store.get(key);
            writer.writeStartElement(element.getLocalName());
            writeAttribute(writer, table, JDBCStoreResourceDefinition.PREFIX);
            writeAttribute(writer, table, JDBCStoreResourceDefinition.BATCH_SIZE);
            writeAttribute(writer, table, JDBCStoreResourceDefinition.FETCH_SIZE);
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
            writeAttribute(writer, column, JDBCStoreResourceDefinition.COLUMN_NAME);
            writeAttribute(writer, column, JDBCStoreResourceDefinition.COLUMN_TYPE);
            writer.writeEndElement();
        }
    }

    private static void writeStoreAttributes(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        writeAttribute(writer, store, StoreResourceDefinition.SHARED);
        writeAttribute(writer, store, StoreResourceDefinition.PRELOAD);
        writeAttribute(writer, store, StoreResourceDefinition.PASSIVATION);
        writeAttribute(writer, store, StoreResourceDefinition.FETCH_STATE);
        writeAttribute(writer, store, StoreResourceDefinition.PURGE);
        writeAttribute(writer, store, StoreResourceDefinition.SINGLETON);
    }

    private static void writeJDBCStoreAttributes(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        writeStoreAttributes(writer, store);

        writeAttribute(writer, store, JDBCStoreResourceDefinition.DATA_SOURCE);
        writeAttribute(writer, store, JDBCStoreResourceDefinition.DIALECT);
    }

    private static void writeStoreElements(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(StoreWriteBehindResourceDefinition.PATH.getKey())) {
            ModelNode writeBehind = store.get(StoreWriteBehindResourceDefinition.PATH.getKeyValuePair());
            writer.writeStartElement(Element.WRITE_BEHIND.getLocalName());
            writeAttribute(writer, writeBehind, StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT);
            writeAttribute(writer, writeBehind, StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE);
            writeAttribute(writer, writeBehind, StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT);
            writeAttribute(writer, writeBehind, StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE);
            writer.writeEndElement();
        }
        writeElement(writer, store, StoreResourceDefinition.PROPERTIES);
    }

    private static void writeAttribute(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getAttributeMarshaller().marshallAsAttribute(attribute, model, true, writer);
    }

    private static void writeElement(XMLExtendedStreamWriter writer, ModelNode model, AttributeDefinition attribute) throws XMLStreamException {
        attribute.getAttributeMarshaller().marshallAsElement(attribute, model, true, writer);
    }
}
