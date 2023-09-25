/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheMetricExecutor implements MetricExecutor<RemoteCacheClientStatisticsMXBean> {

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    public RemoteCacheMetricExecutor(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<RemoteCacheClientStatisticsMXBean> metric) throws OperationFailedException {
        ServiceName name = InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(context, UnaryCapabilityNameResolver.PARENT);
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.get(name);
        return (executor != null) ? executor.execute(new MetricFunction<>(new RemoteCacheClientStatisticsFactory(context.getCurrentAddressValue()), metric)) : null;
    }
}
