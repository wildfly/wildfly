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
 * Registers a runtime resource definition for a singleton deployment.
 * @author Paul Ferraro
 */
public class SingletonDeploymentResourceDefinitionRegistrar extends SingletonRuntimeResourceDefinitionRegistrar {

    private final FunctionExecutorRegistry<ServiceName, Singleton> executors;

    public SingletonDeploymentResourceDefinitionRegistrar(FunctionExecutorRegistry<ServiceName, Singleton> executors) {
        super(SingletonRuntimeResourceRegistration.DEPLOYMENT);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);
        new MetricOperationStepHandler<>(new SingletonDeploymentMetricExecutor(this.executors), SingletonMetric.class).register(registration);
        return registration;
    }
}
