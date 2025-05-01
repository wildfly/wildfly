/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;
import org.wildfly.subsystem.resource.executor.RuntimeOperationExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationFunction;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Executor for remote cache runtime operations.
 * @author Paul Ferraro
 */
public class RemoteCacheOperationExecutor implements RuntimeOperationExecutor<RemoteCacheClientStatisticsMXBean> {

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    public RemoteCacheOperationExecutor(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, RuntimeOperation<RemoteCacheClientStatisticsMXBean> operation) throws OperationFailedException {
        String containerName = context.getCurrentAddress().getParent().getLastElement().getValue();
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.getExecutor(ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, containerName));
        return (executor != null) ? executor.execute(new RuntimeOperationFunction<>(context, op, new RemoteCacheClientStatisticsFactory(context.getCurrentAddressValue()), operation)) : null;
    }
}
