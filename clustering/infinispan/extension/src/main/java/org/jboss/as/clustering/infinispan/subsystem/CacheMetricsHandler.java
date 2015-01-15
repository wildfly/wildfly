/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceName;

/**
 * Handler for cache metrics.
 *
 * @author Tristan Tarrant
 * @author Richard Achmatowicz
 * @author Paul Ferraro
 */
public class CacheMetricsHandler extends AbstractRuntimeOnlyHandler {

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) {
        // Address is of the form: /subsystem=infinispan/cache-container=*/*-cache=*
        PathAddress address = context.getCurrentAddress();
        String containerName = address.getElement(address.size() - 2).getValue();
        String cacheName = address.getElement(address.size() - 1).getValue();
        String name = Operations.getAttributeName(operation);

        CacheMetric metric = CacheMetric.forName(name);

        if (metric == null) {
            context.getFailureDescription().set(InfinispanLogger.ROOT_LOGGER.unknownMetric(name));
        } else {
            Cache<?, ?> cache = ServiceContainerHelper.findValue(context.getServiceRegistry(false), CacheServiceName.CACHE.getServiceName(containerName, cacheName));
            if (cache != null) {
                context.getResult().set(metric.getValue(cache));
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }
}
