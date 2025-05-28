/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.service.capture.FunctionExecutorRegistry;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;

/**
 * Registers a runtime resource definition for a singleton service.
 * @author Paul Ferraro
 */
public class SingletonServiceResourceDefinitionRegistrar extends SingletonRuntimeResourceDefinitionRegistrar {

    private final FunctionExecutorRegistry<ServiceName, Singleton> executors;

    public SingletonServiceResourceDefinitionRegistrar(FunctionExecutorRegistry<ServiceName, Singleton> executors) {
        super(SingletonRuntimeResourceRegistration.SERVICE);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);
        new MetricOperationStepHandler<>(new SingletonServiceMetricExecutor(this.executors), SingletonMetric.class).register(registration);
        return registration;
    }
}
