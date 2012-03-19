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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.List;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Remove a cache container, taking care to remove any child cache resources as well.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat, Inc.
 */
public class CacheContainerRemove extends AbstractRemoveStepHandler {

    public static final CacheContainerRemove INSTANCE = new CacheContainerRemove();

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String containerName = address.getLastElement().getValue();

        // remove any existing cache entries
        removeExistingCacheServices(context, model, containerName);

        // remove the cache container services
        CacheContainerAdd.INSTANCE.removeRuntimeServices(context, operation, model);
    }

    /**
     * Method to re-install any services associated with existing local caches.
     *
     * @param context
     * @param operation
     * @param model
     * @throws OperationFailedException
     */
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String containerName = address.getLastElement().getValue();
        // used by service installation
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler() ;

        // re-install the cache container services
        CacheContainerAdd.INSTANCE.installRuntimeServices(context, operation, model, verificationHandler, null);

        // re-install any existing cache services
        reinstallExistingCacheServices(context, model, containerName, verificationHandler);
    }


    /**
     * Method to reinstall any services associated with existing caches.
     *
     * @param context
     * @param model
     * @param containerName
     * @throws OperationFailedException
     */
    private void reinstallExistingCacheServices(OperationContext context, ModelNode model, String containerName, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        // re-install the services of any local caches
        List<Property> localCacheList = getCachesFromParentModel(ModelKeys.LOCAL_CACHE, model);
        // don't know why extended for loop doesn't detect null list ...
        if (localCacheList != null)
            for (Property localCache : localCacheList) {
                String localCacheName = localCache.getName();
                ModelNode localCacheModel = localCache.getValue();
                ModelNode localCacheAddOp = createCacheAddOperation(ModelKeys.LOCAL_CACHE, containerName, localCacheName);
                LocalCacheAdd.INSTANCE.installRuntimeServices(context, localCacheAddOp, localCacheModel, verificationHandler, null);
            }

        // re-install the services of any invalidation caches
        List<Property> invalidationCacheList = getCachesFromParentModel(ModelKeys.INVALIDATION_CACHE, model);
        if (invalidationCacheList != null)
            for (Property invCache : invalidationCacheList) {
                String invCacheName = invCache.getName();
                ModelNode invCacheModel = invCache.getValue();
                ModelNode invCacheAddOp = createCacheAddOperation(ModelKeys.INVALIDATION_CACHE, containerName, invCacheName);
                InvalidationCacheAdd.INSTANCE.installRuntimeServices(context, invCacheAddOp, invCacheModel, verificationHandler, null);
            }
        // re-install the services of any replicated caches
        List<Property> replCacheList = getCachesFromParentModel(ModelKeys.REPLICATED_CACHE, model);
        if (replCacheList != null)
            for (Property replCache : replCacheList) {
                String replCacheName = replCache.getName();
                ModelNode replCacheModel = replCache.getValue();
                ModelNode replCacheAddOp = createCacheAddOperation(ModelKeys.REPLICATED_CACHE, containerName, replCacheName);
                ReplicatedCacheAdd.INSTANCE.installRuntimeServices(context, replCacheAddOp, replCacheModel, verificationHandler, null);
            }
        // re-install the services of any distributed caches
        List<Property> distCacheList = getCachesFromParentModel(ModelKeys.DISTRIBUTED_CACHE, model);
        if (distCacheList != null)
            for (Property distCache : distCacheList) {
                String distCacheName = distCache.getName();
                ModelNode distCacheModel = distCache.getValue();
                ModelNode distCacheAddOp = createCacheAddOperation(ModelKeys.DISTRIBUTED_CACHE, containerName, distCacheName);
                DistributedCacheAdd.INSTANCE.installRuntimeServices(context, distCacheAddOp, distCacheModel, verificationHandler, null);
            }
    }

    /**
     * Method to remove any services associated with existing caches.
     *
     * @param context
     * @param model
     * @param containerName
     * @throws OperationFailedException
     */
    private void removeExistingCacheServices(OperationContext context, ModelNode model, String containerName) throws OperationFailedException {

        // remove the services of any local caches
        List<Property> localCacheList = getCachesFromParentModel(ModelKeys.LOCAL_CACHE, model);
        // don't know why extended for loop doesn't detect null list ...
        if (localCacheList != null)
            for (Property localCache : localCacheList) {
                String localCacheName = localCache.getName();
                ModelNode localCacheModel = localCache.getValue();
                ModelNode localCacheRemoveOp = createCacheRemoveOperation(ModelKeys.LOCAL_CACHE, containerName, localCacheName);
                LocalCacheAdd.INSTANCE.removeRuntimeServices(context, localCacheRemoveOp, localCacheModel);
            }

        // remove the services of any invalidation caches
        List<Property> invalidationCacheList = getCachesFromParentModel(ModelKeys.INVALIDATION_CACHE, model);
        if (invalidationCacheList != null)
            for (Property invCache : invalidationCacheList) {
                String invCacheName = invCache.getName();
                ModelNode invCacheModel = invCache.getValue();
                ModelNode invCacheRemoveOp = createCacheRemoveOperation(ModelKeys.INVALIDATION_CACHE, containerName, invCacheName);
                InvalidationCacheAdd.INSTANCE.removeRuntimeServices(context, invCacheRemoveOp, invCacheModel);
            }
        // remove the services of any replicated caches
        List<Property> replCacheList = getCachesFromParentModel(ModelKeys.REPLICATED_CACHE, model);
        if (replCacheList != null)
            for (Property replCache : replCacheList) {
                String replCacheName = replCache.getName();
                ModelNode replCacheModel = replCache.getValue();
                ModelNode replCacheRemoveOp = createCacheRemoveOperation(ModelKeys.REPLICATED_CACHE, containerName, replCacheName);
                ReplicatedCacheAdd.INSTANCE.removeRuntimeServices(context, replCacheRemoveOp, replCacheModel);
            }
        // remove the services of any distributed caches
        List<Property> distCacheList = getCachesFromParentModel(ModelKeys.DISTRIBUTED_CACHE, model);
        if (distCacheList != null)
            for (Property distCache : distCacheList) {
                String distCacheName = distCache.getName();
                ModelNode distCacheModel = distCache.getValue();
                ModelNode distCacheRemoveOp = createCacheRemoveOperation(ModelKeys.DISTRIBUTED_CACHE, containerName, distCacheName);
                DistributedCacheAdd.INSTANCE.removeRuntimeServices(context, distCacheRemoveOp, distCacheModel);
            }
    }

    private List<Property> getCachesFromParentModel(String cacheType, ModelNode model) {
        // get the caches of a type
        List<Property> cacheList = null;
        ModelNode caches = model.get(cacheType);
        if (caches.isDefined() && caches.getType() == ModelType.OBJECT) {
            cacheList = caches.asPropertyList();
            return cacheList;
        }
        return null;
    }

    private ModelNode createCacheRemoveOperation(String cacheType, String containerName, String cacheName) {
        // create the address of the cache
        PathAddress cacheAddr = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode removeOp = new ModelNode();
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(cacheAddr.toModelNode());

        return removeOp;
    }

    private ModelNode createCacheAddOperation(String cacheType, String containerName, String cacheName) {
        // create the address of the cache
        PathAddress cacheAddr = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(cacheAddr.toModelNode());

        return addOp;
    }

    private PathAddress getCacheAddress(String containerName, String cacheName, String cacheType) {
        // create the address of the cache
        PathAddress cacheAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement("cache-container", containerName),
                PathElement.pathElement(cacheType, cacheName));
        return cacheAddr;
    }

    private void printCacheList(String name, List<Property> list) {
        System.out.println("Printing list: " + name);
        if (list != null) {
            for (Property element : list) {
                System.out.println("element: name = " + element.getName() +
                        ", value = " + element.getValue());
            }
        } else {
            System.out.println("<empty>");
        }
    }
}
