/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.Cache;
import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.clustering.controller.Operation;
import org.jboss.as.clustering.controller.OperationExecutor;
import org.jboss.as.clustering.controller.OperationFunction;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Operation handler for backup site operations.
 * @author Paul Ferraro
 */
public class BackupOperationExecutor implements OperationExecutor<Map.Entry<String, XSiteAdminOperations>> {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    public BackupOperationExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        this.executors = executors;
    }

    @Override
    public ModelNode execute(OperationContext context, ModelNode operation, Operation<Map.Entry<String, XSiteAdminOperations>> executable) throws OperationFailedException {
        ServiceName name = InfinispanCacheRequirement.CACHE.getServiceName(context, BinaryCapabilityNameResolver.GRANDPARENT_PARENT);
        Function<Cache<?, ?>, Map.Entry<String, XSiteAdminOperations>> mapper = new Function<>() {
            @SuppressWarnings("deprecation")
            @Override
            public Map.Entry<String, XSiteAdminOperations> apply(Cache<?, ?> cache) {
                String site = context.getCurrentAddressValue();
                return new AbstractMap.SimpleImmutableEntry<>(site, cache.getAdvancedCache().getComponentRegistry().getLocalComponent(XSiteAdminOperations.class));
            }
        };
        FunctionExecutor<Cache<?, ?>> executor = this.executors.getExecutor(ServiceDependency.on(name));
        return (executor != null) ? executor.execute(new OperationFunction<>(context, operation, mapper, executable)) : null;
    }
}
