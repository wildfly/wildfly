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
import org.infinispan.interceptors.impl.TxInterceptor;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * A handler for cache transaction metrics.
 *
 * @author Paul Ferraro
 */
public class TransactionMetricExecutor implements MetricExecutor<TxInterceptor<?, ?>> {

    @Override
    public ModelNode execute(OperationContext context, Metric<TxInterceptor<?, ?>> metric) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        Cache<?, ?> cache = new PassiveServiceSupplier<Cache<?, ?>>(context.getServiceRegistry(true), InfinispanCacheRequirement.CACHE.getServiceName(context, containerName, cacheName)).get();
        if (cache != null) {
            TxInterceptor<?, ?> interceptor = CacheMetric.findInterceptor(cache, TxInterceptor.class);
            if (interceptor != null) {
                return metric.execute(interceptor);
            }
        }
        return null;
    }
}
