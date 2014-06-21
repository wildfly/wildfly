/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.infinispan.Cache;
import org.infinispan.interceptors.TxInterceptor;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * A handler for cache transaction metrics.
 *
 * @author Paul Ferraro
 */
public class TransactionMetricsHandler extends AbstractRuntimeOnlyHandler {

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        // Address is of the form: /subsystem=infinispan/cache-container=*/*-cache=*/transaction=TRANSACTION
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String containerName = address.getElement(address.size() - 3).getValue();
        String cacheName = address.getElement(address.size() - 2).getValue();
        String name = operation.require(ModelDescriptionConstants.NAME).asString();

        TransactionMetric metric = TransactionMetric.forName(name);

        if (metric == null) {
            context.getFailureDescription().set(InfinispanLogger.ROOT_LOGGER.unknownMetric(name));
        } else {
            Cache<?, ?> cache = ServiceContainerHelper.findValue(context.getServiceRegistry(false), CacheService.getServiceName(containerName, cacheName));
            if (cache != null) {
                TxInterceptor interceptor = CacheMetric.findInterceptor(cache, TxInterceptor.class);
                context.getResult().set((interceptor != null) ? metric.getValue(interceptor) : new ModelNode(0));
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }
}
