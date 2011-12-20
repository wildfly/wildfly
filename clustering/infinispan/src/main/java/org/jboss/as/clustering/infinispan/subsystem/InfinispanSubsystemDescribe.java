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
 */
public class InfinispanSubsystemDescribe implements OperationStepHandler {

    public static final InfinispanSubsystemDescribe INSTANCE = new InfinispanSubsystemDescribe();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode result = context.getResult();

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement());
        ModelNode subModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        // an add operation to recreate the subsystem ModelNode in its current state
        result.add(InfinispanSubsystemAdd.createOperation(rootAddress.toModelNode(), subModel));

        // add operations to create the cache containers
        if (subModel.hasDefined(ModelKeys.CACHE_CONTAINER)) {
            // list of (cacheContainerName, containerModel)
            for (Property container : subModel.get(ModelKeys.CACHE_CONTAINER).asPropertyList()) {
                ModelNode containerAddress = rootAddress.toModelNode();
                containerAddress.add(ModelKeys.CACHE_CONTAINER, container.getName());
                result.add(CacheContainerAdd.createOperation(containerAddress, container.getValue()));

                addCacheContainerConfigCommands(container, containerAddress, result);

                // list of (cacheType, OBJECT)
                for (Property cacheTypeList : container.getValue().asPropertyList()) {
                    // add commands for local caches
                    if (cacheTypeList.getName().equals(ModelKeys.LOCAL_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = containerAddress.clone() ;
                            cacheAddress.add(ModelKeys.LOCAL_CACHE, cache.getName()) ;
                            result.add(LocalCacheAdd.createOperation(cacheAddress, cache.getValue()));

                            addCacheConfigCommands(cache, cacheAddress, result);
                        }
                    // add commands for invalidation caches
                    } else if (cacheTypeList.getName().equals(ModelKeys.INVALIDATION_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = containerAddress.clone() ;
                            cacheAddress.add(ModelKeys.INVALIDATION_CACHE, cache.getName()) ;
                            result.add(InvalidationCacheAdd.createOperation(cacheAddress, cache.getValue()));

                            addCacheConfigCommands(cache, cacheAddress, result);
                        }
                    // add commands for distributed caches
                    } else if (cacheTypeList.getName().equals(ModelKeys.REPLICATED_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = containerAddress.clone() ;
                            cacheAddress.add(ModelKeys.REPLICATED_CACHE, cache.getName()) ;
                            result.add(ReplicatedCacheAdd.createOperation(cacheAddress, cache.getValue()));

                            addCacheConfigCommands(cache, cacheAddress, result);
                        }
                    // add commands for distributed caches
                    } else if (cacheTypeList.getName().equals(ModelKeys.DISTRIBUTED_CACHE)) {
                        for (Property cache : cacheTypeList.getValue().asPropertyList()) {
                            ModelNode cacheAddress = containerAddress.clone() ;
                            cacheAddress.add(ModelKeys.DISTRIBUTED_CACHE, cache.getName()) ;
                            result.add(DistributedCacheAdd.createOperation(cacheAddress, cache.getValue()));

                            addCacheConfigCommands(cache, cacheAddress, result);
                        }
                    }
                }
            }
        }

        context.completeStep();
    }

    /**
     * Creates commands to recreate existing cache container configuration elements
     *
     * @param container  the cache container Property containing the configuration elements
     * @param address the cache container address
     * @param result  the list of operations
     * @throws OperationFailedException
     */
    private void addCacheContainerConfigCommands(Property container, ModelNode address, ModelNode result) throws OperationFailedException {

        // add operation to create the transport for the container
        if (container.getValue().hasDefined(ModelKeys.SINGLETON)) {
            // command to recreate the transport configuration
            if (container.getValue().get(ModelKeys.SINGLETON, ModelKeys.TRANSPORT).isDefined()) {
                ModelNode transport = container.getValue().get(ModelKeys.SINGLETON, ModelKeys.TRANSPORT);
                ModelNode transportAddress = address.clone() ;
                transportAddress.add(ModelKeys.SINGLETON, ModelKeys.TRANSPORT) ;
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
    private void addCacheConfigCommands(Property cache, ModelNode address, ModelNode result) throws OperationFailedException {

        if (cache.getValue().hasDefined(ModelKeys.SINGLETON)) {
            // command to recreate the locking configuration
            if (cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.LOCKING).isDefined()) {
                ModelNode locking = cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.LOCKING);
                ModelNode lockingAddress = address.clone();
                lockingAddress.add(ModelKeys.SINGLETON, ModelKeys.LOCKING);
                result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.LOCKING_ATTRIBUTES, lockingAddress, locking));
            }
            // command to recreate the transaction configuration
            if (cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.TRANSACTION).isDefined()) {
                ModelNode transaction = cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.TRANSACTION);
                ModelNode transactionAddress = address.clone();
                transactionAddress.add(ModelKeys.SINGLETON, ModelKeys.TRANSACTION);
                result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.TRANSACTION_ATTRIBUTES, transactionAddress, transaction));
            }
            // command to recreate the eviction configuration
            if (cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.EVICTION).isDefined()) {
                ModelNode eviction = cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.EVICTION);
                ModelNode evictionAddress = address.clone();
                evictionAddress.add(ModelKeys.SINGLETON, ModelKeys.EVICTION);
                result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.EVICTION_ATTRIBUTES, evictionAddress, eviction));
            }
            // command to recreate the expiration configuration
            if (cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.EXPIRATION).isDefined()) {
                ModelNode expiration = cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.EXPIRATION);
                ModelNode expirationAddress = address.clone();
                expirationAddress.add(ModelKeys.SINGLETON, ModelKeys.EXPIRATION);
                result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.EXPIRATION_ATTRIBUTES, expirationAddress, expiration));
            }
            // command to recreate the state transfer configuration
            if (cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER).isDefined()) {
                ModelNode stateTransfer = cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER);
                ModelNode stateTransferAddress = address.clone();
                stateTransferAddress.add(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER);
                result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.STATE_TRANSFER_ATTRIBUTES, stateTransferAddress, stateTransfer));
            }
            // command to recreate the rehashing configuration
            if (cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.REHASHING).isDefined()) {
                ModelNode rehashing = cache.getValue().get(ModelKeys.SINGLETON, ModelKeys.REHASHING);
                ModelNode rehashingAddress = address.clone();
                rehashingAddress.add(ModelKeys.SINGLETON, ModelKeys.REHASHING);
                result.add(CacheConfigOperationHandlers.createOperation(CommonAttributes.REHASHING_ATTRIBUTES, rehashingAddress, rehashing));
            }
        }
    }
}