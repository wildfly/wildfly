/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.client.hotrod.jmx.RemoteCacheManagerMXBean;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class RemoteClusterOperationExecutor implements OperationExecutor<Map.Entry<String, RemoteCacheManagerMXBean>> {

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    public RemoteClusterOperationExecutor(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, Operation<Map.Entry<String, RemoteCacheManagerMXBean>> operation) throws OperationFailedException {
        ServiceName name = InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(context, UnaryCapabilityNameResolver.PARENT);
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.getExecutor(ServiceDependency.on(name));
        Function<RemoteCacheContainer, Map.Entry<String, RemoteCacheManagerMXBean>> mapper = new Function<>() {
            @Override
            public Map.Entry<String, RemoteCacheManagerMXBean> apply(RemoteCacheContainer container) {
                String cluster = context.getCurrentAddressValue();
                return new AbstractMap.SimpleImmutableEntry<>(cluster, container);
            }
        };
        return (executor != null) ? executor.execute(new OperationFunction<>(context, op, mapper, operation)) : null;
    }
}
