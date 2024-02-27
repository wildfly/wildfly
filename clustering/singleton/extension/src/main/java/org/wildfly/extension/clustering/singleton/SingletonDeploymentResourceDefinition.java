/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class SingletonDeploymentResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(ServiceName name) {
        return pathElement(name.getParent().getSimpleName());
    }

    static PathElement pathElement(String name) {
        return PathElement.pathElement("deployment", name);
    }

    private final FunctionExecutorRegistry<ServiceName, Singleton> executors;

    public SingletonDeploymentResourceDefinition(FunctionExecutorRegistry<ServiceName, Singleton> executors) {
        super(new Parameters(WILDCARD_PATH, SingletonExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH)).setRuntime());
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        new MetricHandler<>(new SingletonDeploymentMetricExecutor(this.executors), SingletonMetric.class).register(registration);
        return registration;
    }
}
