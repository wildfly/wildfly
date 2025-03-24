/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.List;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for the distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableEjbSubsystemResourceDefinitionRegistrar implements SubsystemResourceDefinitionRegistrar, ResourceServiceConfigurator {

    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("distributable-ejb");
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), DistributableEjbExtension.class);

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(BeanManagementProvider.DEFAULT_SERVICE_DESCRIPTOR).build();
    static final CapabilityReferenceAttributeDefinition<BeanManagementProvider> DEFAULT_BEAN_MANAGEMENT_PROVIDER = new CapabilityReferenceAttributeDefinition.Builder<>("default-bean-management", CapabilityReference.builder(CAPABILITY, BeanManagementProvider.SERVICE_DESCRIPTOR).build())
            .setXmlName(ModelDescriptionConstants.DEFAULT)
            .build();

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .addCapability(CAPABILITY)
                .addAttributes(List.of(DEFAULT_BEAN_MANAGEMENT_PROVIDER))
                .requireSingletonChildResource(ClientMappingsRegistryProviderResourceRegistration.LOCAL)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, RESOLVER).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        // register the child resource infinispan-bean-management
        new InfinispanBeanManagementResourceDefinitionRegistrar().register(registration, context);

        // register the child resources for client-mappings-registry-provider
        new LocalClientMappingsRegistryProviderResourceDefinitionRegistrar().register(registration, context);
        new InfinispanClientMappingsRegistryProviderResourceDefinitionRegistrar().register(registration, context);

        new InfinispanTimerManagementResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, DEFAULT_BEAN_MANAGEMENT_PROVIDER.resolve(context, model)).build();
    }
}
