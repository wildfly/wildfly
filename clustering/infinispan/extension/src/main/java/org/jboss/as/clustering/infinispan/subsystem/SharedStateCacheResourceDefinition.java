/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;
import java.util.function.UnaryOperator;

import org.infinispan.Cache;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.PathElement;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Base class for cache resources which require common cache attributes, clustered cache attributes
 * and shared cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SharedStateCacheResourceDefinition extends ClusteredCacheResourceDefinition {

    static final Set<PathElement> REQUIRED_CHILDREN = Set.of(PartitionHandlingResourceDefinition.PATH, StateTransferResourceDefinition.PATH, BackupsResourceDefinition.PATH);

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addRequiredChildren(REQUIRED_CHILDREN);
        }
    }

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    SharedStateCacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ClusteredCacheServiceHandler handler, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(path, new ResourceDescriptorConfigurator(configurator), handler, executors);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PartitionHandlingResourceDefinition().register(registration);
        new StateTransferResourceDefinition().register(registration);
        new BackupsResourceDefinition(this.executors).register(registration);

        return registration;
    }
}
