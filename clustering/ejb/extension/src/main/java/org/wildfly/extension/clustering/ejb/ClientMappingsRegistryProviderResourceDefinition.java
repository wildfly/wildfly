package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.ejb.EjbRequirement;
import org.wildfly.clustering.service.Requirement;

import java.util.function.UnaryOperator;

/**
 * Base class definition of the /subsystem=distributable-ejb/client-mappings-registry resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class ClientMappingsRegistryProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("client-mappings-registry", value);
    }

    enum Capability implements CapabilityProvider, UnaryOperator<RuntimeCapability.Builder<Void>> {
        CLIENT_MAPPINGS_REGISTRY_PROVIDER(EjbRequirement.CLIENT_MAPPINGS_REGISTRY_PROVIDER),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(Requirement requirement) {
            this.capability = new RequirementCapability(requirement, this);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }

        @Override
        public RuntimeCapability.Builder<Void> apply(RuntimeCapability.Builder<Void> builder) {
            return builder.setAllowMultipleRegistrations(true);
        }
    }

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceConfiguratorFactory serviceConfiguratorFactory;

    ClientMappingsRegistryProviderResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory) {
        super(path, DistributableEjbExtension.SUBSYSTEM_RESOLVER.createChildResolver(pathElement(PathElement.WILDCARD_VALUE), path));
        this.configurator = configurator;
        this.serviceConfiguratorFactory = serviceConfiguratorFactory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this.serviceConfiguratorFactory);
        new SimpleResourceRegistration(descriptor, handler).register(registration);
        return registration;
    }

}
