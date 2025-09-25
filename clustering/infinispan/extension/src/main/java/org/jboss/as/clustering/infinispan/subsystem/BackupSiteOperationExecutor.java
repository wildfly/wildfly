/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;
import org.wildfly.subsystem.resource.executor.RuntimeOperationExecutor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationFunction;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Operation handler for backup site operations.
 * @author Paul Ferraro
 */
public class BackupSiteOperationExecutor implements RuntimeOperationExecutor<Map.Entry<String, XSiteAdminOperations>> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupSiteOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode operation, RuntimeOperation<Map.Entry<String, XSiteAdminOperations>> executable) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress().getParent().getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();
        Function<Cache<?, ?>, Map.Entry<String, XSiteAdminOperations>> mapper = new Function<>() {
            @Override
            public Map.Entry<String, XSiteAdminOperations> apply(Cache<?, ?> cache) {
                String site = context.getCurrentAddressValue();
                return new AbstractMap.SimpleImmutableEntry<>(site, ComponentRegistry.componentOf(cache.getAdvancedCache(), XSiteAdminOperations.class));
            }
        };
        FunctionExecutor<Cache<?, ?>> executor = this.executors.getExecutor(ServiceDependency.on(InfinispanServiceDescriptor.CACHE, containerName, cacheName));
        return (executor != null) ? executor.execute(new RuntimeOperationFunction<>(context, operation, mapper, executable)) : null;
    }
}
