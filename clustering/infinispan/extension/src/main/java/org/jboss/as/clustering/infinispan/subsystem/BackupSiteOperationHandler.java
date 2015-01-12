/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.infinispan.Cache;
import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;

/**
 * Operation handler for backup site operations.
 * @author Paul Ferraro
 */
public class BackupSiteOperationHandler extends AbstractRuntimeOnlyHandler {

    private final Operation<BackupSiteOperationContext> operation;

    public BackupSiteOperationHandler(Operation<BackupSiteOperationContext> operation) {
        this.operation = operation;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        PathAddress address = Operations.getPathAddress(operation);
        String cacheContainerName = address.getElement(address.size() - 3).getValue();
        String cacheName = address.getElement(address.size() - 2).getValue();
        final String site = address.getElement(address.size() - 1).getValue();

        ServiceName cacheServiceName = CacheServiceName.CACHE.getServiceName(cacheContainerName, cacheName);
        ServiceController<?> controller = context.getServiceRegistry(true).getService(cacheServiceName);
        final Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();

        try {
            BackupSiteOperationContext operationContext = new BackupSiteOperationContext() {
                @Override
                public String getSite() {
                    return site;
                }

                @Override
                public XSiteAdminOperations getOperations() {
                    return cache.getAdvancedCache().getComponentRegistry().getComponent(XSiteAdminOperations.class);
                }
            };
            ModelNode result = this.operation.getValue(operationContext);
            if (result != null) {
                context.getResult().set(result);
            }
            context.stepCompleted();
        } catch(Exception e) {
            throw InfinispanLogger.ROOT_LOGGER.failedToInvokeOperation(e.getCause(), this.operation.getDefinition().getName());
        }
    }
}
