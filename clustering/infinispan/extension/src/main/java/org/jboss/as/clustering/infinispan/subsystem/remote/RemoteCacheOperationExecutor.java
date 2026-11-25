/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.service.capture.FunctionExecutorRegistry;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;
import org.wildfly.subsystem.resource.executor.RuntimeOperationExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationFunction;

/**
 * Executor for remote cache runtime operations.
 * @author Paul Ferraro
 */
public class RemoteCacheOperationExecutor implements RuntimeOperationExecutor<RemoteCacheClientStatisticsMXBean> {

    private final FunctionExecutorRegistry<PathAddress, RemoteCacheContainer> executors;

    public RemoteCacheOperationExecutor(FunctionExecutorRegistry<PathAddress, RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, RuntimeOperation<RemoteCacheClientStatisticsMXBean> operation) throws OperationFailedException {
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.getExecutor(context.getCurrentAddress().getParent());
        return (executor != null) ? executor.execute(new RuntimeOperationFunction<>(context, op, new RemoteCacheClientStatisticsFactory(context.getCurrentAddressValue()), operation)) : null;
    }
}
