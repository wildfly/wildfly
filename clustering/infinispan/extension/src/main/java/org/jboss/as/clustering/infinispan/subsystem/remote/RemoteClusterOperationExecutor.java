/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
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
 * Executor for runtime operations of a remote Infinispan cluster.
 * @author Paul Ferraro
 */
public class RemoteClusterOperationExecutor implements RuntimeOperationExecutor<Map.Entry<String, RemoteCacheManagerMXBean>> {

    private final FunctionExecutorRegistry<PathAddress, RemoteCacheContainer> executors;

    public RemoteClusterOperationExecutor(FunctionExecutorRegistry<PathAddress, RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, RuntimeOperation<Map.Entry<String, RemoteCacheManagerMXBean>> operation) throws OperationFailedException {
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.getExecutor(context.getCurrentAddress().getParent());
        Function<RemoteCacheContainer, Map.Entry<String, RemoteCacheManagerMXBean>> mapper = new Function<>() {
            @Override
            public Map.Entry<String, RemoteCacheManagerMXBean> apply(RemoteCacheContainer container) {
                String cluster = context.getCurrentAddressValue();
                return new AbstractMap.SimpleImmutableEntry<>(cluster, container);
            }
        };
        return (executor != null) ? executor.execute(new RuntimeOperationFunction<>(context, op, mapper, operation)) : null;
    }
}
