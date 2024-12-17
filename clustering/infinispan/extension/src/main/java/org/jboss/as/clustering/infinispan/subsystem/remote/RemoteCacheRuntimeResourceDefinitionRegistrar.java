/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinitionRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheRuntimeResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("remote-cache", name);
    }

    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    public RemoteCacheRuntimeResourceDefinitionRegistrar(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(WILDCARD_PATH);
        ResourceDefinition definition = ResourceDefinition.builder(ResourceRegistration.of(WILDCARD_PATH), resolver).asRuntime().build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        new MetricOperationStepHandler<>(new RemoteCacheMetricExecutor(this.executors), RemoteCacheMetric.class).register(registration);
        new RuntimeOperationStepHandler<>(new RemoteCacheOperationExecutor(this.executors), RemoteCacheOperation.class).register(registration);
        return registration;
    }
}
