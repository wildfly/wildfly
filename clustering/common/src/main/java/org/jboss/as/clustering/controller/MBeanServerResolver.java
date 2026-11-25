/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import javax.management.MBeanServer;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resolves an optional dependency on the server {@link MBeanServer}.
 * @author Paul Ferraro
 */
public class MBeanServerResolver implements ResourceModelResolver<ServiceDependency<MBeanServer>> {
    static final NullaryServiceDescriptor<MBeanServer> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.management.jmx", MBeanServer.class);

    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition attribute;

    public MBeanServerResolver(RuntimeCapability<Void> capability) {
        this(capability, null);
    }

    public MBeanServerResolver(RuntimeCapability<Void> capability, AttributeDefinition attribute) {
        this.capability = capability;
        this.attribute = attribute;
    }

    @Override
    public ServiceDependency<MBeanServer> resolve(OperationContext context, Resource resource) {
        return this.resolve(context);
    }

    @Override
    public ServiceDependency<MBeanServer> resolve(OperationContext context, ModelNode model) {
        return this.resolve(context);
    }

    private ServiceDependency<MBeanServer> resolve(OperationContext context) {
        return context.hasOptionalCapability(SERVICE_DESCRIPTOR, this.capability, this.attribute) ? ServiceDependency.on(SERVICE_DESCRIPTOR) : ServiceDependency.of(null);
    }
}
