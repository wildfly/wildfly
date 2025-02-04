/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import java.util.List;

import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-ejb resource.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableEjbResourceDefinition extends SubsystemResourceDefinition implements ResourceServiceConfigurator {

    static final PathElement PATH = pathElement(DistributableEjbExtension.SUBSYSTEM_NAME);

    static final RuntimeCapability<Void> DEFAULT_BEAN_MANAGEMENT_PROVIDER = RuntimeCapability.Builder.of(BeanManagementProvider.DEFAULT_SERVICE_DESCRIPTOR).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_BEAN_MANAGEMENT("default-bean-management", ModelType.STRING, CapabilityReferenceRecorder.builder(DEFAULT_BEAN_MANAGEMENT_PROVIDER, BeanManagementProvider.SERVICE_DESCRIPTOR).build()),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder<?> reference) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setCapabilityReference(reference)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    DistributableEjbResourceDefinition() {
        super(PATH, DistributableEjbExtension.SUBSYSTEM_RESOLVER);
    }

    @Override
    public void register(SubsystemRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubsystemModel(this);

        new DefaultSubsystemDescribeHandler().register(registration);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(DEFAULT_BEAN_MANAGEMENT_PROVIDER))
                .addRequiredSingletonChildren(LocalClientMappingsRegistryProviderResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = ResourceServiceHandler.of(ResourceOperationRuntimeHandler.configureService(this));
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        // register the child resource infinispan-bean-management
        new InfinispanBeanManagementResourceDefinition().register(registration);

        // register the child resources for client-mappings-registry-provider
        new LocalClientMappingsRegistryProviderResourceDefinition().register(registration);
        new InfinispanClientMappingsRegistryProviderResourceDefinition().register(registration);

        // register the child resources for module-availability-registrar-provider
        new LocalModuleAvailabilityRegistrarProviderResourceDefinition().register(registration);
        new InfinispanModuleAvailabilityRegistrarProviderResourceDefinition().register(registration);

        new InfinispanTimerManagementResourceDefinition().register(registration);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = Attribute.DEFAULT_BEAN_MANAGEMENT.resolveModelAttribute(context, model).asString();
        return CapabilityServiceInstaller.builder(DEFAULT_BEAN_MANAGEMENT_PROVIDER, ServiceDependency.on(BeanManagementProvider.SERVICE_DESCRIPTOR, name)).build();
    }
}
