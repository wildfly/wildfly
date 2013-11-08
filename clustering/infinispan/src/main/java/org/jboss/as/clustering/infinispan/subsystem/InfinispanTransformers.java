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
     * Register the transformers for transforming from current to earlier management api versions.
     *
     * @param subsystem the subsystem registration
     */
    static void registerTransformers(final SubsystemRegistration subsystem) {
        registerTransformers130(subsystem);
        registerTransformers140(subsystem);
        registerTransformers141(subsystem);
    }

    /**
     * Register the transformers for transforming from current to 1.3.0 management api version, in which:
     * - attributes INDEXING_PROPERTIES, SEGMENTS were added in 1.4
     * - attribute VIRTUAL_NODES was deprecated in 1.4
     * - expression support was added to most attributes in 1.4, except for CLUSTER, DEFAULT_CACHE and MODE
     * for which it was already enabled in 1.3
     * - attribute STATISTICS was added in 2.0
     *
     * Chaining of transformers is used in cases where two transformers are required for the same operation.
     *
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers130(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1, 3);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        final ResourceTransformationDescriptionBuilder cacheContainerBuilder = subsystemBuilder.addChildResource(CacheContainerResourceDefinition.CONTAINER_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, InfinispanRejectedExpressions_1_3.REJECT_CONTAINER_ATTRIBUTES)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();

        cacheContainerBuilder.addChildResource(TransportResourceDefinition.TRANSPORT_PATH)
            .getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, InfinispanRejectedExpressions_1_3.REJECT_TRANSPORT_ATTRIBUTES)
            .end();

        final ResourceTransformationDescriptionBuilder distributedCacheBuilder = cacheContainerBuilder.addChildResource(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING, CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME, ClusteredCacheResourceDefinition.MODE,
                        CacheResourceDefinition.CACHE_MODULE, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL,  ClusteredCacheResourceDefinition.QUEUE_SIZE,
                        ClusteredCacheResourceDefinition.REMOTE_TIMEOUT, CacheResourceDefinition.START,
                        DistributedCacheResourceDefinition.L1_LIFESPAN, DistributedCacheResourceDefinition.OWNERS, DistributedCacheResourceDefinition.VIRTUAL_NODES, DistributedCacheResourceDefinition.SEGMENTS)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                //Convert segments to virtual-nodes if it is set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, DistributedCacheResourceDefinition.SEGMENTS)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                TransformationContext context) {
                            if (attributeValue.isDefined()) {
                                attributeValue.set(SegmentsAndVirtualNodeConverter.segmentsToVirtualNodes(attributeValue.asString()));
                            }
                        }
                    }, DistributedCacheResourceDefinition.SEGMENTS)
                .addRename(DistributedCacheResourceDefinition.SEGMENTS, DistributedCacheResourceDefinition.VIRTUAL_NODES.getName())
                .end();
        registerCacheResourceChildren(distributedCacheBuilder, true);

        final ResourceTransformationDescriptionBuilder invalidationCacheBuilder = cacheContainerBuilder.addChildResource(InvalidationCacheResourceDefinition.INVALIDATION_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING, CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME, ClusteredCacheResourceDefinition.MODE,
                        CacheResourceDefinition.CACHE_MODULE, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL, ClusteredCacheResourceDefinition.QUEUE_SIZE, ClusteredCacheResourceDefinition.REMOTE_TIMEOUT,
                        CacheResourceDefinition.START)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        registerCacheResourceChildren(invalidationCacheBuilder, false);

        final ResourceTransformationDescriptionBuilder localCacheBuilder = cacheContainerBuilder.addChildResource(LocalCacheResourceDefinition.LOCAL_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME,
                        CacheResourceDefinition.CACHE_MODULE, CacheResourceDefinition.START)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        registerCacheResourceChildren(localCacheBuilder, false);

        final ResourceTransformationDescriptionBuilder replicatedCacheBuilder = cacheContainerBuilder.addChildResource(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING, CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME, ClusteredCacheResourceDefinition.MODE,
                        CacheResourceDefinition.CACHE_MODULE, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL, ClusteredCacheResourceDefinition.QUEUE_SIZE, ClusteredCacheResourceDefinition.REMOTE_TIMEOUT,
                        CacheResourceDefinition.START)
                //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
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
        parent.addChildResource(EvictionResourceDefinition.EVICTION_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    EvictionResourceDefinition.MAX_ENTRIES, EvictionResourceDefinition.EVICTION_STRATEGY)
            .end();
        parent.addChildResource(ExpirationResourceDefinition.EXPIRATION_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    ExpirationResourceDefinition.EXPIRATION_ATTRIBUTES)
            .end();
        parent.addChildResource(TransactionResourceDefinition.TRANSACTION_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    TransactionResourceDefinition.LOCKING, TransactionResourceDefinition.STOP_TIMEOUT)
            .end();

        // Store transformers ///////
        registerJdbcStoreTransformers(parent);
        //fileStore=FILE_STORE
        ResourceTransformationDescriptionBuilder fileStoreBuilder = parent.addChildResource(FileStoreResourceDefinition.FILE_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        FileStoreResourceDefinition.PATH, BaseStoreResourceDefinition.FETCH_STATE, BaseStoreResourceDefinition.PASSIVATION,
                        BaseStoreResourceDefinition.PRELOAD, BaseStoreResourceDefinition.PURGE, BaseStoreResourceDefinition.SHARED, BaseStoreResourceDefinition.SINGLETON)
                .end();
        registerStoreTransformerChildren(fileStoreBuilder);
        //store=STORE
        ResourceTransformationDescriptionBuilder storeBuilder = parent.addChildResource(StoreResourceDefinition.STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        StoreResourceDefinition.CLASS, BaseStoreResourceDefinition.FETCH_STATE, BaseStoreResourceDefinition.PASSIVATION, BaseStoreResourceDefinition.PRELOAD,
                        BaseStoreResourceDefinition.PURGE, BaseStoreResourceDefinition.SHARED, BaseStoreResourceDefinition.SINGLETON)
                .end();
        registerStoreTransformerChildren(storeBuilder);
        //remote-store=REMOTE_STORE
        ResourceTransformationDescriptionBuilder remoteStoreBuilder = parent.addChildResource(RemoteStoreResourceDefinition.REMOTE_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        RemoteStoreResourceDefinition.CACHE, BaseStoreResourceDefinition.FETCH_STATE, BaseStoreResourceDefinition.PASSIVATION, BaseStoreResourceDefinition.PRELOAD,
                        BaseStoreResourceDefinition.PURGE, BaseStoreResourceDefinition.SHARED, BaseStoreResourceDefinition.SINGLETON, RemoteStoreResourceDefinition.SOCKET_TIMEOUT,
                        RemoteStoreResourceDefinition.TCP_NO_DELAY)
                .end();
        registerStoreTransformerChildren(remoteStoreBuilder);

        if (addStateTransfer) {
            parent.addChildResource(StateTransferResourceDefinition.STATE_TRANSFER_PATH)
                    .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, StateTransferResourceDefinition.STATE_TRANSFER_ATTRIBUTES)
                    .end();
        }
    }

    private static void registerJdbcStoreTransformers(ResourceTransformationDescriptionBuilder parent) {
        //Common jdbc store stuff
        final RejectAttributeChecker nameTypeChecker = new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(new HashMap<String, RejectAttributeChecker>(){
            private static final long serialVersionUID = 1L;
            {
                setMapValues(this, RejectAttributeChecker.SIMPLE_EXPRESSIONS, BaseJDBCStoreResourceDefinition.COLUMN_NAME, BaseJDBCStoreResourceDefinition.COLUMN_TYPE);
            }});
        final RejectAttributeChecker jdbcKeyedTableChecker = new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(new HashMap<String, RejectAttributeChecker>() {
            private static final long serialVersionUID = 1L;
            {
                setMapValues(this, RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        BaseJDBCStoreResourceDefinition.PREFIX, BaseJDBCStoreResourceDefinition.BATCH_SIZE, BaseJDBCStoreResourceDefinition.FETCH_SIZE);
                setMapValues(this, nameTypeChecker, BaseJDBCStoreResourceDefinition.ID_COLUMN, BaseJDBCStoreResourceDefinition.DATA_COLUMN, BaseJDBCStoreResourceDefinition.TIMESTAMP_COLUMN);
            }});
        AttributeDefinition[] jdbcStoreSimpleAttributes = { BaseJDBCStoreResourceDefinition.DATA_SOURCE, BaseStoreResourceDefinition.FETCH_STATE, BaseStoreResourceDefinition.PASSIVATION,
                BaseStoreResourceDefinition.PRELOAD, BaseStoreResourceDefinition.PURGE, BaseStoreResourceDefinition.SHARED, BaseStoreResourceDefinition.SINGLETON };

        //binaryKeyedJdbcStore
        ResourceTransformationDescriptionBuilder binaryKeyedJdbcStoreBuilder = parent.addChildResource(BinaryKeyedJDBCStoreResourceDefinition.BINARY_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, BaseJDBCStoreResourceDefinition.BINARY_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(binaryKeyedJdbcStoreBuilder);

        //stringKeyedJdbcStore
        ResourceTransformationDescriptionBuilder stringKeyedJdbcStoreBuilder = parent.addChildResource(StringKeyedJDBCStoreResourceDefinition.STRING_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, BaseJDBCStoreResourceDefinition.STRING_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(stringKeyedJdbcStoreBuilder);

        //mixedKeyedJdbcStore
        ResourceTransformationDescriptionBuilder mixedKeyedJdbcStoreBuilder = parent.addChildResource(MixedKeyedJDBCStoreResourceDefinition.MIXED_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, BaseJDBCStoreResourceDefinition.STRING_KEYED_TABLE, BaseJDBCStoreResourceDefinition.BINARY_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(mixedKeyedJdbcStoreBuilder);

    }

    private static void registerStoreTransformerChildren(ResourceTransformationDescriptionBuilder parent) {
        parent.addChildResource(StorePropertyResourceDefinition.STORE_PROPERTY_PATH)
            .getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, StorePropertyResourceDefinition.VALUE)
            .end();

        parent.addChildResource(StoreWriteBehindResourceDefinition.STORE_WRITE_BEHIND_PATH)
            .getAttributeBuilder()
            .addRejectCheck(
                    RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                    StoreWriteBehindResourceDefinition.FLUSH_LOCK_TIMEOUT, StoreWriteBehindResourceDefinition.MODIFICATION_QUEUE_SIZE, StoreWriteBehindResourceDefinition.SHUTDOWN_TIMEOUT, StoreWriteBehindResourceDefinition.THREAD_POOL_SIZE)
            .end();
    }

    static void setMapValues(Map<String, RejectAttributeChecker> map, RejectAttributeChecker checker, AttributeDefinition...defs) {
        for (AttributeDefinition def : defs) {
            map.put(def.getName(), checker);
        }
    }

    /**
     * Register the transformers for transforming from current to 1.4.0 management api version, including:
     * - use of the VIRTUAL_NODES attribute was again allowed in 1.4.1, with a value conversion applied
     * - attribute STATISTICS was added in 2.0
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers140(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1, 4, 0);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder cacheContainerBuilder = subsystemBuilder.addChildResource(CacheContainerResourceDefinition.CONTAINER_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        cacheContainerBuilder.addChildResource(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH).getAttributeBuilder()
                //Convert virtual-nodes to segments if it is set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, DistributedCacheResourceDefinition.VIRTUAL_NODES)
                .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                    @Override
                    protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                    TransformationContext context) {
                        if (attributeValue.isDefined()) {
                            attributeValue.set(SegmentsAndVirtualNodeConverter.virtualNodesToSegments(attributeValue));
                        }
                    }
                }, DistributedCacheResourceDefinition.VIRTUAL_NODES)
                .addRename(DistributedCacheResourceDefinition.VIRTUAL_NODES, DistributedCacheResourceDefinition.SEGMENTS.getName())
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        cacheContainerBuilder.addChildResource(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        cacheContainerBuilder.addChildResource(InvalidationCacheResourceDefinition.INVALIDATION_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        cacheContainerBuilder.addChildResource(LocalCacheResourceDefinition.LOCAL_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();

        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);
    }

    /**
     * Register the transformers for transforming from current to 1.4.0 management api version, including:
     * - attribute STATISTICS was added in 2.0
     * @param subsystem the subsystems registration
     */
    private static void registerTransformers141(final SubsystemRegistration subsystem) {

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder containerBuilder = builder.addChildResource(CacheContainerResourceDefinition.CONTAINER_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        containerBuilder.addChildResource(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        containerBuilder.addChildResource(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        containerBuilder.addChildResource(InvalidationCacheResourceDefinition.INVALIDATION_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        containerBuilder.addChildResource(LocalCacheResourceDefinition.LOCAL_CACHE_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();

        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 4, 1));
    }
}
