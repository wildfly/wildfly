/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class PartitionHandlingRuntimeResourceDefinition extends CacheComponentRuntimeResourceDefinition {

    static final PathElement PATH = pathElement("partition-handling");

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    PartitionHandlingRuntimeResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);
        new MetricHandler<>(new PartitionHandlingMetricExecutor(this.executors), PartitionHandlingMetric.class).register(registration);
        new OperationHandler<>(new PartitionHandlingOperationExecutor(this.executors), PartitionHandlingOperation.class).register(registration);
        return registration;
    }
}
