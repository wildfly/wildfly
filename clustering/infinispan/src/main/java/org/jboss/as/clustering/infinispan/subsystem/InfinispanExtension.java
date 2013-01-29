/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Defines the Infinispan subsystem and its addressable resources.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "infinispan";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    public static final String RESOURCE_NAME = InfinispanExtension.class.getPackage().getName() + "." +"LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 4;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
           StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
           for (String kp : keyPrefix) {
               prefix.append('.').append(kp);
           }
            return new InfinispanResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, InfinispanExtension.class.getClassLoader());
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        // IMPORTANT: Management API version != xsd version! Not all Management API changes result in XSD changes
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        // Create the path resolver handler
        final ResolvePathHandler resolvePathHandler;
        if (context.getProcessType().isServer()) {
            resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                    .setPathAttribute(FileStoreResource.PATH)
                    .setRelativeToAttribute(FileStoreResource.RELATIVE_TO)
                    .build();
        } else {
            resolvePathHandler = null;
        }

        subsystem.registerSubsystemModel(new InfinispanSubsystemRootResource(resolvePathHandler));
        subsystem.registerXMLElementWriter(new InfinispanSubsystemXMLWriter());
        if (context.isRegisterTransformers()) {
            registerTransformers(subsystem);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }

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
    private void registerTransformers(final SubsystemRegistration subsystem) {
        final ModelVersion version = ModelVersion.create(1, 3);

        final ResourceTransformationDescriptionBuilder subsystemBuilder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        final ResourceTransformationDescriptionBuilder cacheContainerBuilder = subsystemBuilder.addChildResource(CacheContainerResource.CONTAINER_PATH)
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CacheContainerResource.JNDI_NAME, CacheContainerResource.CACHE_CONTAINER_MODULE, CacheContainerResource.START)
                .end();

        ResourceTransformationDescriptionBuilder distributedCacheBuilder = cacheContainerBuilder.addChildResource(DistributedCacheResource.DISTRIBUTED_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.ASYNC_MARSHALLING, ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME, ClusteredCacheResource.MODE,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.QUEUE_FLUSH_INTERVAL,  ClusteredCacheResource.QUEUE_SIZE,
                        ClusteredCacheResource.REMOTE_TIMEOUT, ClusteredCacheResource.START,
                        DistributedCacheResource.L1_LIFESPAN, DistributedCacheResource.OWNERS, DistributedCacheResource.VIRTUAL_NODES, DistributedCacheResource.SEGMENTS)
                //TODO discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.ALWAYS, CacheResource.INDEXING_PROPERTIES)
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

        ResourceTransformationDescriptionBuilder invalidationCacheBuilder = cacheContainerBuilder.addChildResource(InvalidationCacheResource.INVALIDATION_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.ASYNC_MARSHALLING, ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME, ClusteredCacheResource.MODE,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.QUEUE_FLUSH_INTERVAL, ClusteredCacheResource.QUEUE_SIZE, ClusteredCacheResource.REMOTE_TIMEOUT,
                        ClusteredCacheResource.START)
                //TODO discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.ALWAYS, CacheResource.INDEXING_PROPERTIES)
                .end();
        registerCacheResourceChildren(invalidationCacheBuilder, false);

        ResourceTransformationDescriptionBuilder localCacheBuilder = cacheContainerBuilder.addChildResource(LocalCacheResource.LOCAL_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.START)
                //TODO discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.ALWAYS, CacheResource.INDEXING_PROPERTIES)
                .end();
        registerCacheResourceChildren(localCacheBuilder, false);

        ResourceTransformationDescriptionBuilder replicatedCacheBuilder = cacheContainerBuilder.addChildResource(ReplicatedCacheResource.REPLICATED_CACHE_PATH)
                .getAttributeBuilder()
                .addRejectCheck(
                        RejectAttributeChecker.SIMPLE_EXPRESSIONS,
                        ClusteredCacheResource.ASYNC_MARSHALLING, ClusteredCacheResource.BATCHING, ClusteredCacheResource.INDEXING, ClusteredCacheResource.JNDI_NAME, ClusteredCacheResource.MODE,
                        ClusteredCacheResource.CACHE_MODULE, ClusteredCacheResource.QUEUE_FLUSH_INTERVAL, ClusteredCacheResource.QUEUE_SIZE, ClusteredCacheResource.REMOTE_TIMEOUT,
                        ClusteredCacheResource.START)
                //TODO discard indexing-properties if undefined, and reject it if not set
                .setDiscard(DiscardAttributeChecker.ALWAYS, CacheResource.INDEXING_PROPERTIES)
                .end();
        registerCacheResourceChildren(replicatedCacheBuilder, true);

        TransformationDescription.Tools.register(subsystemBuilder.build(), subsystem, version);
    }

    private void registerCacheResourceChildren(final ResourceTransformationDescriptionBuilder parent, final boolean addStateTransfer) {
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
                    ExpirationResource.INTERVAL, EvictionResource.EVICTION_STRATEGY, EvictionResource.MAX_ENTRIES)
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
                        BaseJDBCStoreResource.DATA_SOURCE, StoreResource.FETCH_STATE, StoreResource.PASSIVATION,
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


        //TODO
        /*

            "transaction"
         */
        if (addStateTransfer) {
            parent.addChildResource(StateTransferResource.STATE_TRANSFER_PATH)
                    .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, StateTransferResource.STATE_TRANSFER_ATTRIBUTES)
                    .end();
        }
    }

    private void registerJdbcStoreTransformers(ResourceTransformationDescriptionBuilder parent) {
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

    private void registerStoreTransformerChildren(ResourceTransformationDescriptionBuilder parent) {
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
    }

//    private static void deprecated(final SubsystemRegistration subsystem) {
//        // reject expression values for resource - recursive, so have to define all attributes
//        // reject expression values for operations - not recursive, do don't have to define all attributes
//
//        InfinispanResourceAndOperationTransformer_1_3 resourceAndOperationTransformer = new InfinispanResourceAndOperationTransformer_1_3() ;
//        final RejectExpressionValuesTransformer totalReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_TOTAL);
//        // shold we chain a resource transformer here to ignore expressions on attributes?
//        final ChainedResourceTransformer chainedResourceAndOperationTransformer = new ChainedResourceTransformer(resourceAndOperationTransformer, totalReject.getChainedTransformer());
//
//        TransformersSubRegistration registration = subsystem.registerModelTransformers(ModelVersion.create(1, 3), chainedResourceAndOperationTransformer);
//
//        // cache-container=*
//        // this transformer will check and reject values for cache-container attributes which should not accept expressions in 1.3
//        final RejectExpressionValuesTransformer cacheContainerReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_CONTAINER_ATTRIBUTES);
//        TransformersSubRegistration containerRegistration =
//                registerTransformer(registration, CacheContainerResource.CONTAINER_PATH, cacheContainerReject, cacheContainerReject, cacheContainerReject.getWriteAttributeTransformer(), null);
//
//        // cache-container=*/transport=TRANSPORT
//        // this transformer will check and reject values for attributes which should not accept expressions in 1.3
//        final RejectExpressionValuesTransformer transportReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_TRANSPORT_ATTRIBUTES);
//        registerTransformer(containerRegistration, TransportResource.TRANSPORT_PATH, transportReject, transportReject, transportReject.getWriteAttributeTransformer(), null);
//
//        // cache-container=*/cache=*
//        // this chained transformer will do two things:
//        // - discard attributes INDEXING_PROPERTIES, SEGMENTS and VIRTUAL_NODES from add and write operations in 1.3
//        // - check and reject values for cache attributes which should not accept expressions in 1.3
//        final InfinispanDiscardAttributesTransformer removeSelectedCacheAttributes = new InfinispanDiscardAttributesTransformer(ModelKeys.INDEXING, ModelKeys.SEGMENTS, ModelKeys.VIRTUAL_NODES);
//        final RejectExpressionValuesTransformer cacheReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_CACHE_ATTRIBUTES);
//        final ChainedResourceTransformer chainedResource = new ChainedResourceTransformer(resourceAndOperationTransformer, cacheReject.getChainedTransformer());
//        final ChainedOperationTransformer chainedAdd = new ChainedOperationTransformer(resourceAndOperationTransformer, cacheReject);
//        final ChainedOperationTransformer chainedWrite = new ChainedOperationTransformer(resourceAndOperationTransformer.getWriteAttributeTransformer(), cacheReject.getWriteAttributeTransformer());
//
//        PathElement[] cachePaths = {
//                LocalCacheResource.LOCAL_CACHE_PATH,
//                InvalidationCacheResource.INVALIDATION_CACHE_PATH,
//                ReplicatedCacheResource.REPLICATED_CACHE_PATH,
//                DistributedCacheResource.DISTRIBUTED_CACHE_PATH
//        };
//        for (int i=0; i < cachePaths.length; i++) {
//            TransformersSubRegistration cacheRegistration =
//                    registerTransformer(containerRegistration, cachePaths[i], chainedResource, chainedAdd, chainedWrite, removeSelectedCacheAttributes.getUndefineAttributeTransformer());
//            registerCacheChildrenTransformers(cacheRegistration) ;
//        }
//    }
//
//    private static TransformersSubRegistration registerTransformer(TransformersSubRegistration parent, PathElement path, ResourceTransformer resourceTransformer, OperationTransformer addTransformer,
//                        OperationTransformer writeAttributeTransformer, OperationTransformer undefineAttributeTransformer) {
//        TransformersSubRegistration childReg = parent.registerSubResource(path, resourceTransformer);
//        childReg.registerOperationTransformer(ADD, addTransformer);
//        childReg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, writeAttributeTransformer);
//        if (undefineAttributeTransformer != null) {
//            childReg.registerOperationTransformer(UNDEFINE_ATTRIBUTE_OPERATION, undefineAttributeTransformer);
//        }
//        return childReg;
//    }
//
//    private static void registerCacheChildrenTransformers(TransformersSubRegistration cacheReg) {
//
//        // this transformer will check and reject values for cache child attributes which should not accept expressions in 1.3
//        final RejectExpressionValuesTransformer childReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_CHILD_ATTRIBUTES) ;
//
//        PathElement[] childPaths = {
//                LockingResource.LOCKING_PATH,
//                TransactionResource.TRANSACTION_PATH,
//                ExpirationResource.EXPIRATION_PATH,
//                EvictionResource.EVICTION_PATH,
//                StateTransferResource.STATE_TRANSFER_PATH
//        } ;
//
//        for (int i=0; i < childPaths.length; i++) {
//            // reject expressions on operations in children
//            cacheReg.registerSubResource(childPaths[i], (OperationTransformer) childReject);
//        }
//
//        // this transformer will check and reject values for store attributes which should not accept expressions in 1.3
//        final RejectExpressionValuesTransformer storeReject = new RejectExpressionValuesTransformer(InfinispanRejectedExpressions_1_3.REJECT_STORE_ATTRIBUTES);
//        PathElement[] storePaths = {
//                StoreResource.STORE_PATH,
//                FileStoreResource.FILE_STORE_PATH,
//                StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_PATH,
//                BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_PATH,
//                MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_PATH,
//                RemoteStoreResource.REMOTE_STORE_PATH
//        } ;
//
//        for (int i=0; i < storePaths.length; i++) {
//            // reject expressions on operations on stores and store properties
//            TransformersSubRegistration storeReg = cacheReg.registerSubResource(storePaths[i], (OperationTransformer) storeReject);
//            storeReg.registerSubResource(StoreWriteBehindResource.STORE_WRITE_BEHIND_PATH, (OperationTransformer) storeReject);
//            storeReg.registerSubResource(StorePropertyResource.STORE_PROPERTY_PATH, (OperationTransformer) storeReject);
//        }
//    }
//
//    private static class InfinispanDiscardAttributesTransformer extends DiscardAttributesTransformer {
//        private InfinispanDiscardAttributesTransformer(String... attributes) {
//            super(attributes);
//        }
//    }
}
