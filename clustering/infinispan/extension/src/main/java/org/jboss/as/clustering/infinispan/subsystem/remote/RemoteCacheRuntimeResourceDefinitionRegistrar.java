/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.jboss.as.clustering.infinispan.subsystem.ComponentRuntimeResourceDefinitionRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a runtime resource definition for a remote cache.
 * @author Paul Ferraro
 */
public class RemoteCacheRuntimeResourceDefinitionRegistrar extends ComponentRuntimeResourceDefinitionRegistrar {

    static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("remote-cache"));

    RemoteCacheRuntimeResourceDefinitionRegistrar(FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        super(REGISTRATION, new ManagementResourceRegistrar() {
            @Override
            public void register(ManagementResourceRegistration registration) {
                new MetricOperationStepHandler<>(new RemoteCacheMetricExecutor(executors), RemoteCacheMetric.class).register(registration);
                new RuntimeOperationStepHandler<>(new RemoteCacheOperationExecutor(executors), RemoteCacheOperation.class).register(registration);
            }
        });
    }
}
