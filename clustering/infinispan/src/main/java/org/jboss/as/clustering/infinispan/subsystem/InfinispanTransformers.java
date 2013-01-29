/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class InfinispanTransformers {
    /**
     * Register the transformers for transforming from 1.4.0 to 1.3.0 management api versions, in which:
     * - attributes INDEXING_PROPERTIES, SEGMENTS were added in 1.4
     * - attribute VIRTUAL_NODES was deprecated in 1.4
     * - expression support was added to most attributes in 1.4, except for CLUSTER, DEFAULT_CACHE and MODE
     * for which it was already enabled in 1.3
     *
     * Chaining of transformers is used in cases where two transformers are required for the same operation.
     *
     * @param subsystem the subsystems registration
     */
    static void registerTransformers(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1, 3);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        final ResourceTransformationDescriptionBuilder cacheContainerBuilder = subsystemBuilder.addChildResource(CacheContainerResource.CONTAINER_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, InfinispanRejectedExpressions_1_3.REJECT_CONTAINER_ATTRIBUTES)
                .end();

        cacheContainerBuilder.addChildResource(TransportResource.TRANSPORT_PATH)
            .getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, InfinispanRejectedExpressions_1_3.REJECT_TRANSPORT_ATTRIBUTES)
            .end();

        final ResourceTransformationDescriptionBuilder distributedCacheBuilder = cacheContainerBuilder.addChildResource(DistributedCacheResource.DISTRIBUTED_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.ASYNC_MARSHALLING, ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME, ClusteredCacheResource.MODE,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.QUEUE_FLUSH_INTERVAL,  ClusteredCacheResource.QUEUE_SIZE,
                        ClusteredCacheResource.REMOTE_TIMEOUT, ClusteredCacheResource.START,
                        DistributedCacheResource.L1_LIFESPAN, DistributedCacheResource.OWNERS, DistributedCacheResource.VIRTUAL_NODES, DistributedCacheResource.SEGMENTS)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResource.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResource.INDEXING_PROPERTIES)
                //Convert segments to virtual-nodes if it is set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, DistributedCacheResource.SEGMENTS)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                TransformationContext context) {
                            if (attributeValue.isDefined()) {
                                attributeValue.set(SegmentsAndVirtualNodeConverter.segmentsToVirtualNodes(attributeValue.asString()));
                            }
                        }
                    }, DistributedCacheResource.SEGMENTS)
                .addRename(DistributedCacheResource.SEGMENTS, DistributedCacheResource.VIRTUAL_NODES.getName())
                .end();
        registerCacheResourceChildren(distributedCacheBuilder, true);

        final ResourceTransformationDescriptionBuilder invalidationCacheBuilder = cacheContainerBuilder.addChildResource(InvalidationCacheResource.INVALIDATION_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.ASYNC_MARSHALLING, ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME, ClusteredCacheResource.MODE,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.QUEUE_FLUSH_INTERVAL, ClusteredCacheResource.QUEUE_SIZE, ClusteredCacheResource.REMOTE_TIMEOUT,
                        ClusteredCacheResource.START)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResource.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResource.INDEXING_PROPERTIES)
                .end();
        registerCacheResourceChildren(invalidationCacheBuilder, false);

        final ResourceTransformationDescriptionBuilder localCacheBuilder = cacheContainerBuilder.addChildResource(LocalCacheResource.LOCAL_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.START)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResource.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResource.INDEXING_PROPERTIES)
                .end();
        registerCacheResourceChildren(localCacheBuilder, false);

        final ResourceTransformationDescriptionBuilder replicatedCacheBuilder = cacheContainerBuilder.addChildResource(ReplicatedCacheResource.REPLICATED_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.ASYNC_MARSHALLING, ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME, ClusteredCacheResource.MODE,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.QUEUE_FLUSH_INTERVAL, ClusteredCacheResource.QUEUE_SIZE, ClusteredCacheResource.REMOTE_TIMEOUT,
                        ClusteredCacheResource.START)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResource.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResource.INDEXING_PROPERTIES)
                .end();
        registerCacheResourceChildren(replicatedCacheBuilder, true);

        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);

    }

    private static void registerCacheResourceChildren(final ResourceTransformationDescriptionBuilder parent, final boolean addStateTransfer) {
        parent.addChildResource(LockingResource.LOCKING_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    LockingResource.ACQUIRE_TIMEOUT, LockingResource.CONCURRENCY_LEVEL, LockingResource.ISOLATION, LockingResource.STRIPING)
            .end();
        parent.addChildResource(EvictionResource.EVICTION_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    EvictionResource.MAX_ENTRIES, EvictionResource.EVICTION_STRATEGY)
            .end();
        parent.addChildResource(ExpirationResource.EXPIRATION_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    ExpirationResource.EXPIRATION_ATTRIBUTES)
            .end();
        parent.addChildResource(TransactionResource.TRANSACTION_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    TransactionResource.LOCKING, TransactionResource.STOP_TIMEOUT)
            .end();

        // Store transformers ///////
        registerJdbcStoreTransformers(parent);
        //fileStore=FILE_STORE
        ResourceTransformationDescriptionBuilder fileStoreBuilder = parent.addChildResource(FileStoreResource.FILE_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        FileStoreResource.PATH, StoreResource.FETCH_STATE, StoreResource.PASSIVATION,
                        StoreResource.PRELOAD, StoreResource.PURGE, StoreResource.SHARED, StoreResource.SINGLETON)
                .end();
        registerStoreTransformerChildren(fileStoreBuilder);
        //store=STORE
        ResourceTransformationDescriptionBuilder storeBuilder = parent.addChildResource(StoreResource.STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        StoreResource.CLASS, StoreResource.FETCH_STATE, StoreResource.PASSIVATION, StoreResource.PRELOAD,
                        StoreResource.PURGE, StoreResource.SHARED, StoreResource.SINGLETON)
                .end();
        registerStoreTransformerChildren(storeBuilder);
        //remote-store=REMOTE_STORE
        ResourceTransformationDescriptionBuilder remoteStoreBuilder = parent.addChildResource(RemoteStoreResource.REMOTE_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        RemoteStoreResource.CACHE, StoreResource.FETCH_STATE, StoreResource.PASSIVATION, StoreResource.PRELOAD,
                        StoreResource.PURGE, StoreResource.SHARED, StoreResource.SINGLETON, RemoteStoreResource.SOCKET_TIMEOUT,
                        RemoteStoreResource.TCP_NO_DELAY)
                .end();
        registerStoreTransformerChildren(remoteStoreBuilder);

        if (addStateTransfer) {
            parent.addChildResource(StateTransferResource.STATE_TRANSFER_PATH)
                    .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, StateTransferResource.STATE_TRANSFER_ATTRIBUTES)
                    .end();
        }
    }

    private static void registerJdbcStoreTransformers(ResourceTransformationDescriptionBuilder parent) {
        //Common jdbc store stuff
        final RejectAttributeChecker nameTypeChecker = new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(new HashMap<String, RejectAttributeChecker>(){
            private static final long serialVersionUID = 1L;
            {
                setMapValues(this, RejectAttributeChecker.SIMPLE_EXPRESSIONS, BaseJDBCStoreResource.COLUMN_NAME, BaseJDBCStoreResource.COLUMN_TYPE);
            }});
        final RejectAttributeChecker jdbcKeyedTableChecker = new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(new HashMap<String, RejectAttributeChecker>() {
            private static final long serialVersionUID = 1L;
            {
                setMapValues(this, RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        BaseJDBCStoreResource.PREFIX, BaseJDBCStoreResource.BATCH_SIZE, BaseJDBCStoreResource.FETCH_SIZE);
                setMapValues(this, nameTypeChecker, BaseJDBCStoreResource.ID_COLUMN, BaseJDBCStoreResource.DATA_COLUMN, BaseJDBCStoreResource.TIMESTAMP_COLUMN);
            }});
        AttributeDefinition[] jdbcStoreSimpleAttributes = {BaseJDBCStoreResource.DATA_SOURCE, StoreResource.FETCH_STATE, StoreResource.PASSIVATION,
                StoreResource.PRELOAD, StoreResource.PURGE, StoreResource.SHARED, StoreResource.SINGLETON};

        //binaryKeyedJdbcStore
        ResourceTransformationDescriptionBuilder binaryKeyedJdbcStoreBuilder = parent.addChildResource(BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, BaseJDBCStoreResource.BINARY_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(binaryKeyedJdbcStoreBuilder);

        //stringKeyedJdbcStore
        ResourceTransformationDescriptionBuilder stringKeyedJdbcStoreBuilder = parent.addChildResource(StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, BaseJDBCStoreResource.STRING_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(stringKeyedJdbcStoreBuilder);

        //mixedKeyedJdbcStore
        ResourceTransformationDescriptionBuilder mixedKeyedJdbcStoreBuilder = parent.addChildResource(MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, BaseJDBCStoreResource.STRING_KEYED_TABLE, BaseJDBCStoreResource.BINARY_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(mixedKeyedJdbcStoreBuilder);

    }

    private static void registerStoreTransformerChildren(ResourceTransformationDescriptionBuilder parent) {
        parent.addChildResource(StorePropertyResource.STORE_PROPERTY_PATH)
            .getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, StorePropertyResource.VALUE)
            .end();

        parent.addChildResource(StoreWriteBehindResource.STORE_WRITE_BEHIND_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    StoreWriteBehindResource.FLUSH_LOCK_TIMEOUT, StoreWriteBehindResource.MODIFICATION_QUEUE_SIZE, StoreWriteBehindResource.SHUTDOWN_TIMEOUT, StoreWriteBehindResource.THREAD_POOL_SIZE)
            .end();
    }

    private static void setMapValues(Map<String, RejectAttributeChecker> map, RejectAttributeChecker checker, AttributeDefinition...defs) {
        for (AttributeDefinition def : defs) {
            map.put(def.getName(), checker);
        }
    }}
