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
import java.util.ArrayList;
import java.util.List;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
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
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            writer.writeAttribute(Attribute.DEFAULT_CACHE_CONTAINER.getLocalName(), model.require(ModelKeys.DEFAULT_CACHE_CONTAINER).asString());
            for (Property entry: model.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {

                String containerName = entry.getName();
                ModelNode container = entry.getValue();

                writer.writeStartElement(Element.CACHE_CONTAINER.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), containerName);
                // AS7-3488 make default-cache a non required attribute
                // this.writeRequired(writer, Attribute.DEFAULT_CACHE, container, ModelKeys.DEFAULT_CACHE);
                this.writeAliases(writer, Attribute.ALIASES, container, ModelKeys.ALIASES);
                this.writeOptional(writer, Attribute.DEFAULT_CACHE, container, ModelKeys.DEFAULT_CACHE);
                this.writeOptional(writer, Attribute.EVICTION_EXECUTOR, container, ModelKeys.EVICTION_EXECUTOR);
                this.writeOptional(writer, Attribute.JNDI_NAME, container, ModelKeys.JNDI_NAME);
                this.writeOptional(writer, Attribute.LISTENER_EXECUTOR, container, ModelKeys.LISTENER_EXECUTOR);
                this.writeOptional(writer, Attribute.REPLICATION_QUEUE_EXECUTOR, container, ModelKeys.REPLICATION_QUEUE_EXECUTOR);
                this.writeOptional(writer, Attribute.START, container, ModelKeys.START);

                if (container.hasDefined(ModelKeys.TRANSPORT)) {
                    writer.writeStartElement(Element.TRANSPORT.getLocalName());
                    ModelNode transport = container.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
                    this.writeOptional(writer, Attribute.STACK, transport, ModelKeys.STACK);
                    this.writeOptional(writer, Attribute.CLUSTER, transport, ModelKeys.CLUSTER);
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
                    CacheMode mode = CacheMode.valueOf(cache.get(ModelKeys.MODE).asString());
                    if (mode.isClustered()) {
                        if (mode.isDistributed()) {
                            writer.writeStartElement(Element.DISTRIBUTED_CACHE.getLocalName());
                            // write identifier before other attributes
                            this.writeRequired(writer, Attribute.NAME, cache, ModelKeys.NAME);
                            this.writeOptional(writer, Attribute.OWNERS, cache, ModelKeys.OWNERS);
                            this.writeOptional(writer, Attribute.VIRTUAL_NODES, cache, ModelKeys.VIRTUAL_NODES);
                            this.writeOptional(writer, Attribute.L1_LIFESPAN, cache, ModelKeys.L1_LIFESPAN);
                        } else if (mode.isInvalidation()) {
                            writer.writeStartElement(Element.INVALIDATION_CACHE.getLocalName());
                            // write identifier before other attributes
                            this.writeRequired(writer, Attribute.NAME, cache, ModelKeys.NAME);
                        } else {
                            writer.writeStartElement(Element.REPLICATED_CACHE.getLocalName());
                            // write identifier before other attributes
                            this.writeRequired(writer, Attribute.NAME, cache, ModelKeys.NAME);
                        }
                        this.writeOptional(writer, Attribute.ASYNC_MARSHALLING, cache, ModelKeys.ASYNC_MARSHALLING);
                        writer.writeAttribute(Attribute.MODE.getLocalName(), Mode.forCacheMode(mode).name());
                        this.writeOptional(writer, Attribute.QUEUE_SIZE, cache, ModelKeys.QUEUE_SIZE);
                        this.writeOptional(writer, Attribute.QUEUE_FLUSH_INTERVAL, cache, ModelKeys.QUEUE_FLUSH_INTERVAL);
                        this.writeOptional(writer, Attribute.REMOTE_TIMEOUT, cache, ModelKeys.REMOTE_TIMEOUT);
                    } else {
                        writer.writeStartElement(Element.LOCAL_CACHE.getLocalName());
                        // write identifier before other attributes
                        this.writeRequired(writer, Attribute.NAME, cache, ModelKeys.NAME);
                    }
                    this.writeOptional(writer, Attribute.START, cache, ModelKeys.START);
                    this.writeOptional(writer, Attribute.BATCHING, cache, ModelKeys.BATCHING);
                    this.writeOptional(writer, Attribute.INDEXING, cache, ModelKeys.INDEXING);
                    this.writeOptional(writer, Attribute.JNDI_NAME, cache, ModelKeys.JNDI_NAME);

                    if (cache.get(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME).isDefined()) {
                        writer.writeStartElement(Element.LOCKING.getLocalName());
                        ModelNode locking = cache.get(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);
                        this.writeOptional(writer, Attribute.ISOLATION, locking, ModelKeys.ISOLATION);
                        this.writeOptional(writer, Attribute.STRIPING, locking, ModelKeys.STRIPING);
                        this.writeOptional(writer, Attribute.ACQUIRE_TIMEOUT, locking, ModelKeys.ACQUIRE_TIMEOUT);
                        this.writeOptional(writer, Attribute.CONCURRENCY_LEVEL, locking, ModelKeys.CONCURRENCY_LEVEL);
                        writer.writeEndElement();
                    }

                    if (cache.get(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME).isDefined()) {
                        writer.writeStartElement(Element.TRANSACTION.getLocalName());
                        ModelNode transaction = cache.get(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);
                        this.writeOptional(writer, Attribute.STOP_TIMEOUT, transaction, ModelKeys.STOP_TIMEOUT);
                        this.writeOptional(writer, Attribute.MODE, transaction, ModelKeys.MODE);
                        this.writeOptional(writer, Attribute.LOCKING, transaction, ModelKeys.LOCKING);
                        writer.writeEndElement();
                    }

                    if (cache.get(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME).isDefined()) {
                        writer.writeStartElement(Element.EVICTION.getLocalName());
                        ModelNode eviction = cache.get(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);
                        this.writeOptional(writer, Attribute.STRATEGY, eviction, ModelKeys.STRATEGY);
                        this.writeOptional(writer, Attribute.MAX_ENTRIES, eviction, ModelKeys.MAX_ENTRIES);
                        writer.writeEndElement();
                    }

                    if (cache.get(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME).isDefined()) {
                        writer.writeStartElement(Element.EXPIRATION.getLocalName());
                        ModelNode expiration = cache.get(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);
                        this.writeOptional(writer, Attribute.MAX_IDLE, expiration, ModelKeys.MAX_IDLE);
                        this.writeOptional(writer, Attribute.LIFESPAN, expiration, ModelKeys.LIFESPAN);
                        this.writeOptional(writer, Attribute.INTERVAL, expiration, ModelKeys.INTERVAL);
                        writer.writeEndElement();
                    }

                    if (cache.get(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME).isDefined()) {
                        ModelNode stateTransfer = cache.get(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);
                        writer.writeStartElement(Element.STATE_TRANSFER.getLocalName());
                        this.writeOptional(writer, Attribute.ENABLED, stateTransfer, ModelKeys.ENABLED);
                        this.writeOptional(writer, Attribute.TIMEOUT, stateTransfer, ModelKeys.TIMEOUT);
                        this.writeOptional(writer, Attribute.CHUNK_SIZE, stateTransfer, ModelKeys.CHUNK_SIZE);
                        writer.writeEndElement();
                    }

                    if (cache.get(ModelKeys.STORE, ModelKeys.STORE_NAME).isDefined()) {
                        ModelNode store = cache.get(ModelKeys.STORE, ModelKeys.STORE_NAME);
                        writer.writeStartElement(Element.STORE.getLocalName());
                        this.writeRequired(writer, Attribute.CLASS, store, ModelKeys.CLASS);
                        this.writeStoreAttributes(writer, store);
                        this.writeStoreWriteBehind(writer, store);
                        this.writeStoreProperties(writer, store);
                        writer.writeEndElement();
                    }
                    else if (cache.get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME).isDefined()) {
                        ModelNode store = cache.get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);
                        writer.writeStartElement(Element.FILE_STORE.getLocalName());
                        this.writeOptional(writer, Attribute.RELATIVE_TO, store, ModelKeys.RELATIVE_TO);
                        this.writeOptional(writer, Attribute.PATH, store, ModelKeys.PATH);
                        this.writeStoreAttributes(writer, store);
                        this.writeStoreWriteBehind(writer, store);
                        this.writeStoreProperties(writer, store);
                        writer.writeEndElement();
                    }
                    else if (cache.get(ModelKeys.JDBC_STORE, ModelKeys.JDBC_STORE_NAME).isDefined()) {
                        ModelNode store = cache.get(ModelKeys.JDBC_STORE, ModelKeys.JDBC_STORE_NAME);
                        writer.writeStartElement(Element.JDBC_STORE.getLocalName());
                        this.writeRequired(writer, Attribute.DATASOURCE, store, ModelKeys.DATASOURCE);
                        this.writeStoreAttributes(writer, store);
                        this.writeStoreWriteBehind(writer, store);
                        this.writeStoreProperties(writer, store);
                        this.writeJDBCStoreTable(writer, Element.BUCKET_TABLE, store, ModelKeys.BUCKET_TABLE);
                        this.writeJDBCStoreTable(writer, Element.ENTRY_TABLE, store, ModelKeys.ENTRY_TABLE);
                        writer.writeEndElement();
                    }
                    else if (cache.get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME).isDefined()) {
                        ModelNode store = cache.get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);
                        writer.writeStartElement(Element.REMOTE_STORE.getLocalName());
                        this.writeOptional(writer, Attribute.CACHE, store, ModelKeys.CACHE);
                        this.writeOptional(writer, Attribute.SOCKET_TIMEOUT, store, ModelKeys.SOCKET_TIMEOUT);
                        this.writeOptional(writer, Attribute.TCP_NO_DELAY, store, ModelKeys.TCP_NO_DELAY);
                        this.writeStoreAttributes(writer, store);
                        this.writeStoreWriteBehind(writer, store);
                        this.writeStoreProperties(writer, store);
                        for (ModelNode remoteServer: store.get(ModelKeys.REMOTE_SERVERS).asList()) {
                            writer.writeStartElement(Element.REMOTE_SERVER.getLocalName());
                            writer.writeAttribute(Attribute.OUTBOUND_SOCKET_BINDING.getLocalName(), remoteServer.get(ModelKeys.OUTBOUND_SOCKET_BINDING).asString());
                            writer.writeEndElement();
                        }
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

    private void writeAliases(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode container, String key) throws XMLStreamException {
        if (container.hasDefined(key)) {
            StringBuffer result = new StringBuffer() ;
            ModelNode aliases = container.get(key);
            if (aliases.isDefined() && aliases.getType() == ModelType.LIST) {
                List<ModelNode> aliasesList = aliases.asList();
                for (int i = 0; i < aliasesList.size(); i++) {
                    result.append(aliasesList.get(i).asString());
                    if (i < aliasesList.size()-1) {
                        result.append(" ");
                    }
                }
                writer.writeAttribute(attribute.getLocalName(), result.toString());
            }
        }
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

    private void writeStoreWriteBehind(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(ModelKeys.WRITE_BEHIND)) {
            ModelNode writeBehind = store.get(ModelKeys.WRITE_BEHIND);
            writer.writeStartElement(Element.WRITE_BEHIND.getLocalName());
            this.writeOptional(writer, Attribute.FLUSH_LOCK_TIMEOUT, writeBehind, ModelKeys.FLUSH_LOCK_TIMEOUT);
            this.writeOptional(writer, Attribute.MODIFICATION_QUEUE_SIZE, writeBehind, ModelKeys.MODIFICATION_QUEUE_SIZE);
            this.writeOptional(writer, Attribute.SHUTDOWN_TIMEOUT, writeBehind, ModelKeys.SHUTDOWN_TIMEOUT);
            this.writeOptional(writer, Attribute.THREAD_POOL_SIZE, writeBehind, ModelKeys.THREAD_POOL_SIZE);
            writer.writeEndElement();
        }
    }

    private void writeStoreProperties(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(ModelKeys.PROPERTY)) {
            // the format of the property elements
            //  "property" => {
            //       "relative-to" => {"value" => "fred"},
            //   }
            for (Property property: store.get(ModelKeys.PROPERTY).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                Property complexValue = property.getValue().asProperty();
                writer.writeCharacters(complexValue.getValue().asString());
                writer.writeEndElement();
            }
        }
    }

    /*
    private void writeStoreProperties(XMLExtendedStreamWriter writer, ModelNode store) throws XMLStreamException {
        if (store.hasDefined(ModelKeys.PROPERTIES)) {
            for (Property property: store.get(ModelKeys.PROPERTIES).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                writer.writeCharacters(property.getValue().asString());
                writer.writeEndElement();
            }
        }
    }
    */

    private void writeOptional(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        if (model.hasDefined(key)) {
            writer.writeAttribute(attribute.getLocalName(), model.get(key).asString());
        }
    }

    private void writeRequired(XMLExtendedStreamWriter writer, Attribute attribute, ModelNode model, String key) throws XMLStreamException {
        writer.writeAttribute(attribute.getLocalName(), model.require(key).asString());
    }
}
