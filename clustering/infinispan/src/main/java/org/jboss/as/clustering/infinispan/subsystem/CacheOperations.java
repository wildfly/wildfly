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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.infinispan.Cache;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

/**
 * @author Guillaume Grossetie
 */
public abstract class CacheOperations implements OperationStepHandler {

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.readResource(PathAddress.EMPTY_ADDRESS);
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final PathAddress containerAddress = address.subAddress(0, address.size() - 1);
                String cacheName = address.getLastElement().getValue();
                String containerName = containerAddress.getLastElement().getValue();
                final ServiceName cacheServiceName = CacheService.getServiceName(containerName, cacheName);
                final ServiceController<?> cacheService = context.getServiceRegistry(false).getService(cacheServiceName);
                try {
                    if (!ServiceController.State.UP.equals(cacheService.getState())) {
                        cacheService.setMode(ServiceController.Mode.ACTIVE);
                    }
                    final Cache cache = ServiceContainerHelper.getValue(cacheService, Cache.class);
                    ModelNode operationResult = invokeCommandOn(cache);
                    if (operationResult != null) {
                        context.getResult().set(operationResult);
                    }
                } catch (StartException e) {
                    throw new OperationFailedException(e.getLocalizedMessage(), e);
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    protected abstract ModelNode invokeCommandOn(Cache cache);

    public static class ClearCache extends CacheOperations {
        public static ClearCache INSTANCE = new ClearCache();

        @Override
        protected ModelNode invokeCommandOn(Cache cache) {
            cache.clear();
            return null;
        }
    }
}
