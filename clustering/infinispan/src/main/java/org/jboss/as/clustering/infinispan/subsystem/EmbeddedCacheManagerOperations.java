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

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Guillaume Grossetie
 */
public abstract class EmbeddedCacheManagerOperations implements OperationStepHandler {

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.readResource(PathAddress.EMPTY_ADDRESS);
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        context.addStep(new OperationStepHandler() {
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final String containerName = address.getLastElement().getValue();
                final ServiceName containerServiceName = EmbeddedCacheManagerService.getServiceName(containerName);
                final ServiceController<?> containerService = context.getServiceRegistry(false).getRequiredService(containerServiceName);
                final EmbeddedCacheManager embeddedCacheManager = EmbeddedCacheManager.class.cast(containerService.getValue());
                if (embeddedCacheManager == null) {
                    throw new OperationFailedException(new ModelNode().set(InfinispanMessages.MESSAGES.cacheContainerNotAvailableForOperation(containerName)));
                }
                ModelNode operationResult = invokeCommandOn(embeddedCacheManager);
                if (operationResult != null) {
                    context.getResult().set(operationResult);
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
        context.completeStep();
    }

    protected abstract ModelNode invokeCommandOn(EmbeddedCacheManager embeddedCacheManager);

    public static class ClearCachesInCacheManager extends EmbeddedCacheManagerOperations {
        public static ClearCachesInCacheManager INSTANCE = new ClearCachesInCacheManager();

        @Override
        protected ModelNode invokeCommandOn(EmbeddedCacheManager embeddedCacheManager) {
            for (String cacheName : embeddedCacheManager.getCacheNames()) {
                embeddedCacheManager.getCache(cacheName).clear();
            }
            return null;
        }
    }
}
