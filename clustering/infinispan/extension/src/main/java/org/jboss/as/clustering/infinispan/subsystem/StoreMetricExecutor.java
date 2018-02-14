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

import org.infinispan.Cache;
import org.infinispan.interceptors.ActivationInterceptor;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;

/**
 * A handler for cache store metrics.
 *
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class StoreMetricExecutor implements MetricExecutor<ActivationInterceptor> {

    @Override
    public ModelNode execute(OperationContext context, Metric<ActivationInterceptor> metric) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        Cache<?, ?> cache = ServiceContainerHelper.findValue(context.getServiceRegistry(false), InfinispanCacheRequirement.CACHE.getServiceName(context, containerName, cacheName));
        if (cache != null) {
            ActivationInterceptor interceptor = CacheMetric.findInterceptor(cache, ActivationInterceptor.class);
            if (interceptor != null) {
                return metric.execute(interceptor);
            }
        }
        return null;
    }
}
