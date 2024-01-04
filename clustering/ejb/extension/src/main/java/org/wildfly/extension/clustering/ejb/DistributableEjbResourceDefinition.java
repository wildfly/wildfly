/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.bean.BeanProviderRequirement;
import org.wildfly.clustering.ejb.bean.DefaultBeanProviderRequirement;
import org.wildfly.clustering.service.Requirement;

/**
 * Definition of the /subsystem=distributable-ejb resource.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableEjbResourceDefinition extends SubsystemResourceDefinition {

    static final PathElement PATH = pathElement(DistributableEjbExtension.SUBSYSTEM_NAME);

    enum Capability implements CapabilityProvider {
        DEFAULT_BEAN_MANAGEMENT_PROVIDER(DefaultBeanProviderRequirement.BEAN_MANAGEMENT_PROVIDER),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(Requirement requirement) {
            this.capability = new RequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DEFAULT_BEAN_MANAGEMENT("default-bean-management", ModelType.STRING, new CapabilityReference(Capability.DEFAULT_BEAN_MANAGEMENT_PROVIDER, BeanProviderRequirement.BEAN_MANAGEMENT_PROVIDER)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, CapabilityReferenceRecorder reference) {
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
                .addCapabilities(Capability.class)
                .addRequiredSingletonChildren(LocalClientMappingsRegistryProviderResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = new DistributableEjbResourceServiceHandler();
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        // register the child resource infinispan-bean-management
        new InfinispanBeanManagementResourceDefinition().register(registration);

        // register the child resources for client-mappings-registry-provider
        new LocalClientMappingsRegistryProviderResourceDefinition().register(registration);
        new InfinispanClientMappingsRegistryProviderResourceDefinition().register(registration);

        new InfinispanTimerManagementResourceDefinition().register(registration);
    }
}
