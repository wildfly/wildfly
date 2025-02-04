/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Base class definition of the /subsystem=distributable-ejb/module-availability-registrar=* resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public abstract class ModuleAvailabilityRegistrarProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("module-availability-registrar", value);
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ModuleAvailabilityRegistrarProvider.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    private final UnaryOperator<ResourceDescriptor> configurator;

    ModuleAvailabilityRegistrarProviderResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, DistributableEjbExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
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
}
