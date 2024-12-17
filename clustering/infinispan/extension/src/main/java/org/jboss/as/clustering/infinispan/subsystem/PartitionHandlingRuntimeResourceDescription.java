/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public enum PartitionHandlingRuntimeResourceDescription implements CacheComponentRuntimeResourceDescription {
    INSTANCE;

    @Override
    public PathElement getPathElement() {
        return PartitionHandlingResourceDescription.INSTANCE.getPathElement();
    }

    @Override
    public ManagementResourceRegistrar apply(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        return new ManagementResourceRegistrar() {
            @Override
            public void register(ManagementResourceRegistration registration) {
                new MetricOperationStepHandler<>(new PartitionHandlingMetricExecutor(executors), PartitionHandlingMetric.class).register(registration);
                new RuntimeOperationStepHandler<>(new PartitionHandlingOperationExecutor(executors), PartitionHandlingOperation.class).register(registration);
            }
        };
    }
}
