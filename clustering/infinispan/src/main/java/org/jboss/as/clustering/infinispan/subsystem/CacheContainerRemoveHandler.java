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
public class CacheContainerRemoveHandler extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String containerName = address.getLastElement().getValue();

        // remove any existing cache entries
        removeExistingCacheServices(context, model, containerName);

        // remove the cache container services
        CacheContainerAddHandler.removeRuntimeServices(context, operation, model);
    }

    /**
     * Method to re-install any services associated with existing local caches.
     *
     * @param context
     * @param operation
     * @param model
     * @throws OperationFailedException
     */
    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final String containerName = address.getLastElement().getValue();
        // used by service installation
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();

        // re-install the cache container services
        CacheContainerAddHandler.installRuntimeServices(context, operation, model, verificationHandler);

        // re-install any existing cache services
        reinstallExistingCacheServices(context, model, containerName, verificationHandler);
    }


    /**
     * Method to reinstall any services associated with existing caches.
     *
     * @param context
     * @param containerModel
     * @param containerName
     * @throws OperationFailedException
     */
    private static void reinstallExistingCacheServices(OperationContext context, ModelNode containerModel, String containerName, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        for (String cacheType: CacheRemoveHandler.INSTANCE.getCacheTypes()) {
            CacheAddHandler addHandler = CacheRemoveHandler.INSTANCE.getAddHandler(cacheType);
            List<Property> caches = getCachesFromParentModel(cacheType, containerModel);
            if (caches != null) {
                for (Property cache: caches) {
                    String cacheName = cache.getName();
                    ModelNode cacheModel = cache.getValue();
                    ModelNode operation = createCacheAddOperation(cacheType, containerName, cacheName);
                    addHandler.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
                }
            }
        }
    }

    /**
     * Method to remove any services associated with existing caches.
     *
     * @param context
     * @param containerModel
     * @param containerName
     * @throws OperationFailedException
     */
    private static void removeExistingCacheServices(OperationContext context, ModelNode containerModel, String containerName) throws OperationFailedException {

        for (String cacheType: CacheRemoveHandler.INSTANCE.getCacheTypes()) {
            CacheAddHandler addHandler = CacheRemoveHandler.INSTANCE.getAddHandler(cacheType);
            List<Property> caches = getCachesFromParentModel(cacheType, containerModel);
            if (caches != null) {
                for (Property cache : caches) {
                    String cacheName = cache.getName();
                    ModelNode cacheModel = cache.getValue();
                    ModelNode operation = createCacheRemoveOperation(cacheType, containerName, cacheName);
                    addHandler.removeRuntimeServices(context, operation, containerModel, cacheModel);
                }
            }
        }
    }

    private static List<Property> getCachesFromParentModel(String cacheType, ModelNode model) {
        // get the caches of a type
        List<Property> cacheList = null;
        ModelNode caches = model.get(cacheType);
        if (caches.isDefined() && caches.getType() == ModelType.OBJECT) {
            cacheList = caches.asPropertyList();
            return cacheList;
        }
        return null;
    }

    private static ModelNode createCacheRemoveOperation(String cacheType, String containerName, String cacheName) {
        // create the address of the cache
        PathAddress cacheAddr = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode removeOp = new ModelNode();
        removeOp.get(OP).set(REMOVE);
        removeOp.get(OP_ADDR).set(cacheAddr.toModelNode());

        return removeOp;
    }

    private static ModelNode createCacheAddOperation(String cacheType, String containerName, String cacheName) {
        // create the address of the cache
        PathAddress cacheAddr = getCacheAddress(containerName, cacheName, cacheType);
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set(ADD);
        addOp.get(OP_ADDR).set(cacheAddr.toModelNode());

        return addOp;
    }

    private static PathAddress getCacheAddress(String containerName, String cacheName, String cacheType) {
        // create the address of the cache
        PathAddress cacheAddr = PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(ModelKeys.CACHE_CONTAINER, containerName),
                PathElement.pathElement(cacheType, cacheName));
        return cacheAddr;
    }
}
