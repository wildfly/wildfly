/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.jmx.RemoteCacheClientStatisticsMXBean;
import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
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

/**
 * @author Paul Ferraro
 */
public class RemoteCacheOperationExecutor implements OperationExecutor<RemoteCacheClientStatisticsMXBean> {

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    public RemoteCacheOperationExecutor(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode op, Operation<RemoteCacheClientStatisticsMXBean> operation) throws OperationFailedException {
        ServiceName name = InfinispanClientRequirement.REMOTE_CONTAINER.getServiceName(context, UnaryCapabilityNameResolver.PARENT);
        FunctionExecutor<RemoteCacheContainer> executor = this.executors.get(name);
        return (executor != null) ? executor.execute(new OperationFunction<>(context, op, new RemoteCacheClientStatisticsFactory(context.getCurrentAddressValue()), operation)) : null;
    }
}
