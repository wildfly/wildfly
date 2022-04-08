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

import java.util.function.Function;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanRequirement;

/**
 * Executor for cache-container metrics.
 *
 * @author Paul Ferraro
 */
public class CacheContainerMetricExecutor implements MetricExecutor<EmbeddedCacheManager> {

    private final FunctionExecutorRegistry<EmbeddedCacheManager> executors;

    public CacheContainerMetricExecutor(FunctionExecutorRegistry<EmbeddedCacheManager> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<EmbeddedCacheManager> metric) throws OperationFailedException {
        ServiceName name = InfinispanRequirement.CONTAINER.getServiceName(context, UnaryCapabilityNameResolver.DEFAULT);
        FunctionExecutor<EmbeddedCacheManager> executor = this.executors.get(name);
        return (executor != null) ? executor.execute(new MetricFunction<>(Function.identity(), metric)) : null;
    }
}
