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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheRemoveHandler extends AbstractRemoveStepHandler {

    static final CacheRemoveHandler INSTANCE = new CacheRemoveHandler();

    private final Map<String, CacheAddHandler> handlers = new HashMap<>();

    CacheRemoveHandler() {
        this.handlers.put(ModelKeys.LOCAL_CACHE, LocalCacheAddHandler.INSTANCE);
        this.handlers.put(ModelKeys.INVALIDATION_CACHE, InvalidationCacheAddHandler.INSTANCE);
        this.handlers.put(ModelKeys.REPLICATED_CACHE, ReplicatedCacheAddHandler.INSTANCE);
        this.handlers.put(ModelKeys.DISTRIBUTED_CACHE, DistributedCacheAddHandler.INSTANCE);
    }

    Set<String> getCacheTypes() {
        return this.handlers.keySet();
    }

    CacheAddHandler getAddHandler(String cacheType) {
        CacheAddHandler handler = this.handlers.get(cacheType);
        if (handler == null) {
            throw new IllegalArgumentException(cacheType);
        }
        return handler;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // we also need the containerModel to re-install cache services
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1);
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        String cacheType = getCacheType(operation);
        CacheAddHandler addHandler = getAddHandler(cacheType);
        addHandler.removeRuntimeServices(context, operation, containerModel, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode cacheModel) throws OperationFailedException {

        // we also need the containerModel to re-install cache services
        final PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathAddress containerAddress = cacheAddress.subAddress(0, cacheAddress.size()-1);
        ModelNode containerModel = context.readResourceFromRoot(containerAddress).getModel();

        // re-add the services if the remove failed
        String cacheType = getCacheType(operation);
        ServiceVerificationHandler verificationHandler = null;
        CacheAddHandler addHandler = getAddHandler(cacheType);
        addHandler.installRuntimeServices(context, operation, containerModel, cacheModel, verificationHandler);
    }

    private static String getCacheType(ModelNode operation) {
        PathAddress cacheAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        return cacheAddress.getLastElement().getKey();
    }
}
