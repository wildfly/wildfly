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

import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DefaultCheckersAndConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
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
     * - expression support was added to most attributes in 1.4, except for CLUSTER, DEFAULT_CACHE and MODE for which it was already enabled in 1.3
     * - attribute STATISTICS was added in 2.0
     * - shared state cache children backup and backup-for are rejected
     * <p/>
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
                        // discard statistics if set to true, reject otherwise
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();

        cacheContainerBuilder.addChildResource(TransportResourceDefinition.TRANSPORT_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, InfinispanRejectedExpressions_1_3.REJECT_TRANSPORT_ATTRIBUTES)
                .end();

        final ResourceTransformationDescriptionBuilder localCacheBuilder = cacheContainerBuilder.addChildResource(LocalCacheResourceDefinition.LOCAL_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME,
                        CacheResourceDefinition.MODULE, CacheResourceDefinition.START)
                        //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                        // discard statistics if set to true, reject otherwise
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        registerCacheResourceChildren(localCacheBuilder, false);

        final ResourceTransformationDescriptionBuilder invalidationCacheBuilder = cacheContainerBuilder.addChildResource(InvalidationCacheResourceDefinition.INVALIDATION_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING, CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME, ClusteredCacheResourceDefinition.MODE,
                        CacheResourceDefinition.MODULE, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL, ClusteredCacheResourceDefinition.QUEUE_SIZE, ClusteredCacheResourceDefinition.REMOTE_TIMEOUT,
                        CacheResourceDefinition.START)
                        //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                        // discard statistics if set to true, reject otherwise
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        registerCacheResourceChildren(invalidationCacheBuilder, false);

        ResourceTransformationDescriptionBuilder replicatedCacheBuilder = cacheContainerBuilder.addChildResource(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH);
        replicatedCacheBuilder.getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING, CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME, ClusteredCacheResourceDefinition.MODE,
                        CacheResourceDefinition.MODULE, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL, ClusteredCacheResourceDefinition.QUEUE_SIZE, ClusteredCacheResourceDefinition.REMOTE_TIMEOUT,
                        CacheResourceDefinition.START)
                        //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                        // discard statistics if set to true, reject otherwise
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        // reject use of x-site related elements
        replicatedCacheBuilder.rejectChildResource(BackupSiteResourceDefinition.BACKUP_PATH);
        replicatedCacheBuilder.rejectChildResource(BackupForResourceDefinition.BACKUP_FOR_PATH);
        registerCacheResourceChildren(replicatedCacheBuilder, true);

        @SuppressWarnings("deprecation")
        ResourceTransformationDescriptionBuilder distributedCacheBuilder = cacheContainerBuilder.addChildResource(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH);
        distributedCacheBuilder.getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResourceDefinition.ASYNC_MARSHALLING, CacheResourceDefinition.BATCHING, CacheResourceDefinition.INDEXING, CacheResourceDefinition.JNDI_NAME, ClusteredCacheResourceDefinition.MODE,
                        CacheResourceDefinition.MODULE, ClusteredCacheResourceDefinition.QUEUE_FLUSH_INTERVAL, ClusteredCacheResourceDefinition.QUEUE_SIZE,
                        ClusteredCacheResourceDefinition.REMOTE_TIMEOUT, CacheResourceDefinition.START,
                        DistributedCacheResourceDefinition.L1_LIFESPAN, DistributedCacheResourceDefinition.OWNERS, DistributedCacheResourceDefinition.VIRTUAL_NODES, DistributedCacheResourceDefinition.SEGMENTS)
                        //discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CacheResourceDefinition.INDEXING_PROPERTIES)
                        //Convert segments to virtual-nodes if it is set
                .setDiscard(DiscardAttributeChecker.UNDEFINED, DistributedCacheResourceDefinition.SEGMENTS)
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
                        // discard statistics if set to true, reject otherwise
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        // reject use of x-site related elements
        distributedCacheBuilder.rejectChildResource(BackupSiteResourceDefinition.BACKUP_PATH);
        distributedCacheBuilder.rejectChildResource(BackupForResourceDefinition.BACKUP_FOR_PATH);
        registerCacheResourceChildren(distributedCacheBuilder, true);

        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);
    }

    private static void registerCacheResourceChildren(final ResourceTransformationDescriptionBuilder parent, final boolean addStateTransfer) {
        parent.addChildResource(LockingResourceDefinition.LOCKING_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, LockingResourceDefinition.LOCKING_ATTRIBUTES)
                .end();
        parent.addChildResource(EvictionResourceDefinition.EVICTION_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, EvictionResourceDefinition.EVICTION_ATTRIBUTES)
                .end();
        parent.addChildResource(ExpirationResourceDefinition.EXPIRATION_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ExpirationResourceDefinition.EXPIRATION_ATTRIBUTES)
                .end();
        parent.addChildResource(TransactionResourceDefinition.TRANSACTION_PATH)
                .getAttributeBuilder()
                        // TRANSACTION_ATTRIBUTES - MODE
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, TransactionResourceDefinition.LOCKING, TransactionResourceDefinition.STOP_TIMEOUT)
                .end();

        // Store transformers
        registerJdbcStoreTransformers(parent);

        //fileStore=FILE_STORE
        ResourceTransformationDescriptionBuilder fileStoreBuilder = parent.addChildResource(FileStoreResourceDefinition.FILE_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        FileStoreResourceDefinition.PATH, StoreResourceDefinition.FETCH_STATE, StoreResourceDefinition.PASSIVATION,
                        StoreResourceDefinition.PRELOAD, StoreResourceDefinition.PURGE, StoreResourceDefinition.SHARED, StoreResourceDefinition.SINGLETON)
                .end();
        registerStoreTransformerChildren(fileStoreBuilder);

        //store=STORE
        ResourceTransformationDescriptionBuilder storeBuilder = parent.addChildResource(CustomStoreResourceDefinition.STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        CustomStoreResourceDefinition.CLASS, StoreResourceDefinition.FETCH_STATE, StoreResourceDefinition.PASSIVATION, StoreResourceDefinition.PRELOAD,
                        StoreResourceDefinition.PURGE, StoreResourceDefinition.SHARED, StoreResourceDefinition.SINGLETON)
                .end();
        registerStoreTransformerChildren(storeBuilder);

        //remote-store=REMOTE_STORE
        ResourceTransformationDescriptionBuilder remoteStoreBuilder = parent.addChildResource(RemoteStoreResourceDefinition.REMOTE_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        RemoteStoreResourceDefinition.CACHE, StoreResourceDefinition.FETCH_STATE, StoreResourceDefinition.PASSIVATION, StoreResourceDefinition.PRELOAD,
                        StoreResourceDefinition.PURGE, StoreResourceDefinition.SHARED, StoreResourceDefinition.SINGLETON, RemoteStoreResourceDefinition.SOCKET_TIMEOUT,
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
        final RejectAttributeChecker nameTypeChecker = new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(new HashMap<String, RejectAttributeChecker>() {
            private static final long serialVersionUID = 1L;

            {
                setMapValues(this, RejectAttributeChecker.SIMPLE_EXPRESSIONS, JDBCStoreResourceDefinition.COLUMN_NAME, JDBCStoreResourceDefinition.COLUMN_TYPE);
            }
        });
        final RejectAttributeChecker jdbcKeyedTableChecker = new RejectAttributeChecker.ObjectFieldsRejectAttributeChecker(new HashMap<String, RejectAttributeChecker>() {
            private static final long serialVersionUID = 1L;

            {
                setMapValues(this, RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        JDBCStoreResourceDefinition.PREFIX, JDBCStoreResourceDefinition.BATCH_SIZE, JDBCStoreResourceDefinition.FETCH_SIZE);
                setMapValues(this, nameTypeChecker, JDBCStoreResourceDefinition.ID_COLUMN, JDBCStoreResourceDefinition.DATA_COLUMN, JDBCStoreResourceDefinition.TIMESTAMP_COLUMN);
            }
        });
        AttributeDefinition[] jdbcStoreSimpleAttributes = {JDBCStoreResourceDefinition.DATA_SOURCE, StoreResourceDefinition.FETCH_STATE, StoreResourceDefinition.PASSIVATION,
                StoreResourceDefinition.PRELOAD, StoreResourceDefinition.PURGE, StoreResourceDefinition.SHARED, StoreResourceDefinition.SINGLETON};

        //binaryKeyedJdbcStore
        ResourceTransformationDescriptionBuilder binaryKeyedJdbcStoreBuilder = parent.addChildResource(BinaryKeyedJDBCStoreResourceDefinition.BINARY_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, JDBCStoreResourceDefinition.BINARY_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(binaryKeyedJdbcStoreBuilder);

        //stringKeyedJdbcStore
        ResourceTransformationDescriptionBuilder stringKeyedJdbcStoreBuilder = parent.addChildResource(StringKeyedJDBCStoreResourceDefinition.STRING_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, JDBCStoreResourceDefinition.STRING_KEYED_TABLE)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, jdbcStoreSimpleAttributes)
                .end();
        registerStoreTransformerChildren(stringKeyedJdbcStoreBuilder);

        //mixedKeyedJdbcStore
        ResourceTransformationDescriptionBuilder mixedKeyedJdbcStoreBuilder = parent.addChildResource(MixedKeyedJDBCStoreResourceDefinition.MIXED_KEYED_JDBC_STORE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(jdbcKeyedTableChecker, JDBCStoreResourceDefinition.STRING_KEYED_TABLE, JDBCStoreResourceDefinition.BINARY_KEYED_TABLE)
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

    static void setMapValues(Map<String, RejectAttributeChecker> map, RejectAttributeChecker checker, AttributeDefinition... defs) {
        for (AttributeDefinition def : defs) {
            map.put(def.getName(), checker);
        }
    }

    /**
     * Register the transformers for transforming from current to 1.4.0 management api version, including:
     * - use of the VIRTUAL_NODES attribute was again allowed in 1.4.1, with a value conversion applied
     * - attribute STATISTICS was added in 2.0
     * - shared state cache children backup and backup-for are rejected
     *
     * @param subsystem the subsystems registration
     */
    @SuppressWarnings("deprecation")
    private static void registerTransformers140(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1, 4, 0);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder cacheContainerBuilder = subsystemBuilder.addChildResource(CacheContainerResourceDefinition.CONTAINER_PATH).getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();

        ResourceTransformationDescriptionBuilder distributedCacheBuilder = cacheContainerBuilder.addChildResource(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH);
        distributedCacheBuilder.getAttributeBuilder()
                //Convert virtual-nodes to segments if it is set
                //.setDiscard(DiscardAttributeChecker.UNDEFINED, DistributedCacheResourceDefinition.VIRTUAL_NODES)
                // this is required to address WFLY-2598
                .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker(false, true) {
                    @Override
                    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        if (attributeName.equals(DistributedCacheResourceDefinition.VIRTUAL_NODES.getName())) {
                            if (attributeValue.isDefined()) {
                                if (attributeValue.equals(new ModelNode().set(1))) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                }, DistributedCacheResourceDefinition.VIRTUAL_NODES)
                .addRejectCheck(VirtualNodesCheckerAndConverter.INSTANCE, DistributedCacheResourceDefinition.VIRTUAL_NODES)
                .setValueConverter(VirtualNodesCheckerAndConverter.INSTANCE, DistributedCacheResourceDefinition.VIRTUAL_NODES)
                .addRename(DistributedCacheResourceDefinition.VIRTUAL_NODES, DistributedCacheResourceDefinition.SEGMENTS.getName())
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        distributedCacheBuilder.rejectChildResource(BackupSiteResourceDefinition.BACKUP_PATH);
        distributedCacheBuilder.rejectChildResource(BackupForResourceDefinition.BACKUP_FOR_PATH);

        ResourceTransformationDescriptionBuilder replicatedCacheBuilder = cacheContainerBuilder.addChildResource(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH);
        replicatedCacheBuilder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        replicatedCacheBuilder.rejectChildResource(BackupSiteResourceDefinition.BACKUP_PATH);
        replicatedCacheBuilder.rejectChildResource(BackupForResourceDefinition.BACKUP_FOR_PATH);

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


        TransformationDescription sub = subsystemBuilder.build();

        // TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);
        TransformationDescription.Tools.register(sub, subsystem, version);
    }

    /**
     * Register the transformers for transforming from current to 1.4.0 management api version, including:
     * - attribute STATISTICS was added in 2.0
     * - shared state cache children backup and backup-for are rejected
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
        ResourceTransformationDescriptionBuilder distributedCacheBuilder = containerBuilder.addChildResource(DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_PATH);
        distributedCacheBuilder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        distributedCacheBuilder.rejectChildResource(BackupSiteResourceDefinition.BACKUP_PATH);
        distributedCacheBuilder.rejectChildResource(BackupForResourceDefinition.BACKUP_FOR_PATH);
        ResourceTransformationDescriptionBuilder replicatedCacheBuilder = containerBuilder.addChildResource(ReplicatedCacheResourceDefinition.REPLICATED_CACHE_PATH);
        replicatedCacheBuilder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheResourceDefinition.STATISTICS)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), CacheResourceDefinition.STATISTICS)
                .end();
        replicatedCacheBuilder.rejectChildResource(BackupSiteResourceDefinition.BACKUP_PATH);
        replicatedCacheBuilder.rejectChildResource(BackupForResourceDefinition.BACKUP_FOR_PATH);
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

    /*
     * Required to prevent conversion of values which are expressions.
     */
    static class VirtualNodesCheckerAndConverter extends DefaultCheckersAndConverter {

        static final VirtualNodesCheckerAndConverter INSTANCE = new VirtualNodesCheckerAndConverter();

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return InfinispanMessages.MESSAGES.segmentsDoesNotSupportExpressions();
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (checkForExpression(attributeValue)) {
                return true;
            }
            return false;
        }

        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                attributeValue.set(SegmentsAndVirtualNodeConverter.virtualNodesToSegments(attributeValue));
            }
        }

        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            // not used for discard - there is a separate transformer for this
            return false;
        }
    }

}
