/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Function;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.clustering.controller.Metric;
import org.jboss.as.clustering.controller.MetricExecutor;
import org.jboss.as.clustering.controller.MetricFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.common.function.Functions;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheContainerMetricExecutor implements MetricExecutor<RemoteCacheManagerMXBean> {

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    public RemoteCacheContainerMetricExecutor(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<RemoteCacheManagerMXBean> metric) throws OperationFailedException {
        ServiceName name = InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(context, UnaryCapabilityNameResolver.DEFAULT);
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.getExecutor(ServiceDependency.on(name));
        return (executor != null) ? executor.execute(new MetricFunction<>(Functions.cast(Function.identity()), metric)) : null;
    }
}
