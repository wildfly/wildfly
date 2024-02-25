/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class LockingRuntimeResourceDefinition extends CacheComponentRuntimeResourceDefinition {

    static final PathElement PATH = pathElement("locking");

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    LockingRuntimeResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);
        new MetricHandler<>(new LockingMetricExecutor(this.executors), LockingMetric.class).register(registration);
        return registration;
    }
}
