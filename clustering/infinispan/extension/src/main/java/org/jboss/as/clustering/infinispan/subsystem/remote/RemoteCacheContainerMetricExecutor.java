/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Function;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.common.function.Functions;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.service.capture.FunctionExecutorRegistry;
import org.wildfly.subsystem.resource.executor.Metric;
import org.wildfly.subsystem.resource.executor.MetricExecutor;
import org.wildfly.subsystem.resource.executor.MetricFunction;

/**
 * Executor for remote cache container metrics.
 * @author Paul Ferraro
 */
public class RemoteCacheContainerMetricExecutor implements MetricExecutor<RemoteCacheManagerMXBean> {

    private final FunctionExecutorRegistry<PathAddress, RemoteCacheContainer> executors;

    public RemoteCacheContainerMetricExecutor(FunctionExecutorRegistry<PathAddress, RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, Metric<RemoteCacheManagerMXBean> metric) throws OperationFailedException {
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.getExecutor(context.getCurrentAddress());
        return (executor != null) ? executor.execute(new MetricFunction<>(Functions.cast(Function.identity()), metric)) : null;
    }
}
