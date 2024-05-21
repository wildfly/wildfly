/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

public abstract class StatefulSessionBeanCacheProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<ServiceDependency<StatefulSessionBeanCacheProvider>> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    private final UnaryOperator<ResourceDescriptor> configurator;

    public StatefulSessionBeanCacheProviderResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, EJB3Extension.getResourceDescriptionResolver(path.getKey()));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, this.resolve(context, model)).build();
    }
}
