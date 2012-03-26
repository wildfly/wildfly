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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Returns a ModelNode LIST of operations which can re-create the subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 * @author Tristan Tarrant
 */
public class InfinispanSubsystemDescribe implements OperationStepHandler {

    public static final InfinispanSubsystemDescribe INSTANCE = new InfinispanSubsystemDescribe();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode result = context.getResult();

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement());
        ModelNode subsystemModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        // an add operation to recreate the subsystem ModelNode in its current state
        result.add(InfinispanSubsystemAdd.createOperation(rootAddress.toModelNode(), subsystemModel));

        // add operations to recreate the cache container ModelNodes in their current state
        addCacheContainerCommands(subsystemModel, rootAddress.toModelNode(), result);

        context.completeStep();
    }

    /**
     * Creates commands to recreate existing cache container elements
     *
     * @param subsystem  the cache container Property containing the configuration elements
     * @param subsystemAddress the cache container address
     * @param result  the list of operations
     * @throws OperationFailedException
     */
    public static void addCacheContainerCommands(ModelNode subsystem, ModelNode subsystemAddress, ModelNode result) throws OperationFailedException {

        // add operations to create the cache containers
        if (subsystem.hasDefined(ModelKeys.CACHE_CONTAINER)) {

            // list of (cacheContainerName, containerModel)
            for (Property container : subsystem.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
                ModelNode containerAddress = subsystemAddress.clone();
                containerAddress.add(ModelKeys.CACHE_CONTAINER, container.getName());
                result.add(CacheContainerAdd.createOperation(containerAddress, container.getValue()));

                addCacheContainerConfigCommands(container, containerAddress, result);

                addCacheCommands(container, containerAddress, result);
            }
        }
    }

    /**
     * Creates commands to recreate existing cache elements
     *
     * @param container  the cache container Property containing the configuration elements
     * @param containerAddress the cache container address
     * @param result  the list of operations
     * @throws OperationFailedException
     */

    public static void addCacheCommands(Property container, ModelNode containerAddress, ModelNode result) throws OperationFailedException {

        // list of (cacheType, OBJECT)
        for (Property cacheTypeList : container.getValue().asPropertyList()) {
            // add commands for local caches
            if (cacheTypeList.getName().equals(ModelKeys.LOCAL_CACHE)) {
                for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                    ModelNode cacheAddress = containerAddress.clone();
                    cacheAddress.add(ModelKeys.LOCAL_CACHE, cache.getName());
                    result.add(LocalCacheAdd.createOperation(cacheAddress, cache.getValue()));

                    addCacheConfigCommands(cache, cacheAddress, result);
                }
                // add commands for invalidation caches
            } else if (cacheTypeList.getName().equals(ModelKeys.INVALIDATION_CACHE)) {
                for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                    ModelNode cacheAddress = containerAddress.clone();
                    cacheAddress.add(ModelKeys.INVALIDATION_CACHE, cache.getName());
                    result.add(InvalidationCacheAdd.createOperation(cacheAddress, cache.getValue()));

                    addCacheConfigCommands(cache, cacheAddress, result);
                }
                // add commands for distributed caches
            } else if (cacheTypeList.getName().equals(ModelKeys.REPLICATED_CACHE)) {
                for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                    ModelNode cacheAddress = containerAddress.clone();
                    cacheAddress.add(ModelKeys.REPLICATED_CACHE, cache.getName());
                    result.add(ReplicatedCacheAdd.createOperation(cacheAddress, cache.getValue()));

                    addCacheConfigCommands(cache, cacheAddress, result);
                    addSharedStateCacheConfigCommands(cache, cacheAddress, result);
                }
                // add commands for distributed caches
            } else if (cacheTypeList.getName().equals(ModelKeys.DISTRIBUTED_CACHE)) {
                for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                    ModelNode cacheAddress = containerAddress.clone();
                    cacheAddress.add(ModelKeys.DISTRIBUTED_CACHE, cache.getName());
                    result.add(DistributedCacheAdd.createOperation(cacheAddress, cache.getValue()));

                    addCacheConfigCommands(cache, cacheAddress, result);
                    addSharedStateCacheConfigCommands(cache, cacheAddress, result);
                }
            }
        }
    }

    /**
     * Creates commands to recreate existing cache container configuration elements
     *
     * @param container  the cache container Property containing the configuration elements
     * @param address the cache container address
     * @param result  the list of operations
     * @throws OperationFailedException
     */
    private static void addCacheContainerConfigCommands(Property container, ModelNode address, ModelNode result) throws OperationFailedException {

        // add operation to create the transport for the container
        if (container.getValue().hasDefined(ModelKeys.TRANSPORT)) {
            // command to recreate the transport configuration
            if (container.getValue().get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME).isDefined()) {
                ModelNode transport = container.getValue().get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME);
                ModelNode transportAddress = address.clone() ;
                transportAddress.add(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME) ;
                result.add(TransportAdd.createOperation(transportAddress, transport));
            }
        }
    }

    /**
     * Creates commands to recreate existing cache configuration elements
     *
     * @param cache  the cache Property containing the configuration elements
     * @param address the cache address
     * @param result  the list of operations
     * @throws OperationFailedException
     */
    private static void addCacheConfigCommands(Property cache, ModelNode address, ModelNode result) throws OperationFailedException {

        // command to recreate the locking configuration
        if (cache.getValue().get(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME).isDefined()) {
            ModelNode locking = cache.getValue().get(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);
            ModelNode lockingAddress = address.clone();
            lockingAddress.add(ModelKeys.LOCKING, ModelKeys.LOCKING_NAME);
            result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.LOCKING_ATTRIBUTES, lockingAddress, locking));
        }
        // command to recreate the transaction configuration
        if (cache.getValue().get(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME).isDefined()) {
            ModelNode transaction = cache.getValue().get(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);
            ModelNode transactionAddress = address.clone();
            transactionAddress.add(ModelKeys.TRANSACTION, ModelKeys.TRANSACTION_NAME);
            result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.TRANSACTION_ATTRIBUTES, transactionAddress, transaction));
        }
        // command to recreate the eviction configuration
        if (cache.getValue().get(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME).isDefined()) {
            ModelNode eviction = cache.getValue().get(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);
            ModelNode evictionAddress = address.clone();
            evictionAddress.add(ModelKeys.EVICTION, ModelKeys.EVICTION_NAME);
            result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.EVICTION_ATTRIBUTES, evictionAddress, eviction));
        }
        // command to recreate the expiration configuration
        if (cache.getValue().get(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME).isDefined()) {
            ModelNode expiration = cache.getValue().get(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);
            ModelNode expirationAddress = address.clone();
            expirationAddress.add(ModelKeys.EXPIRATION, ModelKeys.EXPIRATION_NAME);
            result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.EXPIRATION_ATTRIBUTES, expirationAddress, expiration));
        }

        // command to recreate the cache store configuration
        // we need to take account of properties here
        if (cache.getValue().get(ModelKeys.STORE, ModelKeys.STORE_NAME).isDefined()) {
            ModelNode store = cache.getValue().get(ModelKeys.STORE, ModelKeys.STORE_NAME);
            ModelNode storeAddress = address.clone();
            storeAddress.add(ModelKeys.STORE, ModelKeys.STORE_NAME);
            result.add(CacheConfigOperationHandlers.createStoreOperation(CommonAttributes.COMMON_STORE_ATTRIBUTES, storeAddress, store,
                    CommonAttributes.STORE_ATTRIBUTES));
            addStoreWriteBehindConfigCommands(store, storeAddress, result);
            addCacheStorePropertyCommands(store, storeAddress, result);
        }
        else if (cache.getValue().get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME).isDefined()) {
            ModelNode store = cache.getValue().get(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);
            ModelNode storeAddress = address.clone();
            storeAddress.add(ModelKeys.FILE_STORE, ModelKeys.FILE_STORE_NAME);
            result.add(CacheConfigOperationHandlers.createStoreOperation(CommonAttributes.COMMON_STORE_ATTRIBUTES, storeAddress, store,
                    CommonAttributes.FILE_STORE_ATTRIBUTES));
            addStoreWriteBehindConfigCommands(store, storeAddress, result);
            addCacheStorePropertyCommands(store, storeAddress, result);
        }
        else if (cache.getValue().get(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME).isDefined()) {
            ModelNode store = cache.getValue().get(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME);
            ModelNode storeAddress = address.clone();
            storeAddress.add(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME);
            result.add(CacheConfigOperationHandlers.createStringKeyedStoreOperation(storeAddress, store));
            addStoreWriteBehindConfigCommands(store, storeAddress, result);
            addCacheStorePropertyCommands(store, storeAddress, result);
        }
        else if (cache.getValue().get(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME).isDefined()) {
            ModelNode store = cache.getValue().get(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME);
            ModelNode storeAddress = address.clone();
            storeAddress.add(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME);
            result.add(CacheConfigOperationHandlers.createBinaryKeyedStoreOperation(storeAddress, store));
            addStoreWriteBehindConfigCommands(store, storeAddress, result);
            addCacheStorePropertyCommands(store, storeAddress, result);
        }
        else if (cache.getValue().get(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME).isDefined()) {
            ModelNode store = cache.getValue().get(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME);
            ModelNode storeAddress = address.clone();
            storeAddress.add(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME);
            result.add(CacheConfigOperationHandlers.createMixedKeyedStoreOperation(storeAddress, store));
            addStoreWriteBehindConfigCommands(store, storeAddress, result);
            addCacheStorePropertyCommands(store, storeAddress, result);
        }
        else if (cache.getValue().get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME).isDefined()) {
            ModelNode store = cache.getValue().get(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);
            ModelNode storeAddress = address.clone();
            storeAddress.add(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);
            result.add(CacheConfigOperationHandlers.createStoreOperation(CommonAttributes.COMMON_STORE_ATTRIBUTES, storeAddress, store,
                    CommonAttributes.REMOTE_STORE_ATTRIBUTES));
            addStoreWriteBehindConfigCommands(store, storeAddress, result);
            addCacheStorePropertyCommands(store, storeAddress, result);
        }
    }

    /**
     * Creates commands to recreate existing cache configuration elements
     *
     * @param cache  the cache Property containing the configuration elements
     * @param address the cache address
     * @param result  the list of operations
     * @throws OperationFailedException
     */
    private static void addSharedStateCacheConfigCommands(Property cache, ModelNode address, ModelNode result) throws OperationFailedException {

        // command to recreate the state transfer configuration
        if (cache.getValue().get(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME).isDefined()) {
            ModelNode stateTransfer = cache.getValue().get(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);
            ModelNode stateTransferAddress = address.clone();
            stateTransferAddress.add(ModelKeys.STATE_TRANSFER, ModelKeys.STATE_TRANSFER_NAME);
            ModelNode createOperation = CacheConfigOperationHandlers.createOperation(CommonAttributes.STATE_TRANSFER_ATTRIBUTES, stateTransferAddress, stateTransfer);
            result.add(createOperation);
        }
    }

    private static void addStoreWriteBehindConfigCommands(ModelNode store, ModelNode address, ModelNode result) throws OperationFailedException {
        // command to recreate the write-behind configuration
        if (store.get(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME).isDefined()) {
            ModelNode writeBehind = store.get(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME);
            ModelNode writeBehindAddress = address.clone();
            writeBehindAddress.add(ModelKeys.WRITE_BEHIND, ModelKeys.WRITE_BEHIND_NAME);
            ModelNode createOperation = CacheConfigOperationHandlers.createOperation(CommonAttributes.WRITE_BEHIND_ATTRIBUTES, writeBehindAddress, writeBehind);
            result.add(createOperation);
        }
    }

    private static void addCacheStorePropertyCommands(ModelNode store, ModelNode address, ModelNode result) throws OperationFailedException {

        if (store.hasDefined(ModelKeys.PROPERTY)) {
             for (Property property : store.get(ModelKeys.PROPERTY).asPropertyList()) {
                 ModelNode propertyAddress = address.clone().add(ModelKeys.PROPERTY, property.getName());
                 AttributeDefinition[] ATTRIBUTE = {CommonAttributes.VALUE} ;
                 result.add(CacheConfigOperationHandlers.createOperation(ATTRIBUTE, propertyAddress, property.getValue()));
             }
        }
    }
}