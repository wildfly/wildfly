/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.ejb.EjbDefaultProviderRequirement;
import org.wildfly.clustering.ejb.EjbProviderRequirement;

/**
 * Definition of the /subsystem=distributable-ejb resource.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class DistributableEjbResourceDefinition extends SubsystemResourceDefinition<SubsystemRegistration> {

    static final PathElement PATH = pathElement(DistributableEjbExtension.SUBSYSTEM_NAME);

    enum Capability implements CapabilityProvider {
        DEFAULT_BEAN_MANAGEMENT_PROVIDER(EjbDefaultProviderRequirement.BEAN_MANAGEMENT_PROVIDER),
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
        DEFAULT_BEAN_MANAGEMENT("default-bean-management", ModelType.STRING, new CapabilityReference(Capability.DEFAULT_BEAN_MANAGEMENT_PROVIDER, EjbProviderRequirement.BEAN_MANAGEMENT_PROVIDER)),
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
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        // register the child resource infinispan-bean-management
        new InfinispanBeanManagementResourceDefinition().register(registration);

        // register the child resources for client-mappings-registry-provider
        new LocalClientMappingsRegistryProviderResourceDefinition().register(registration);
        new InfinispanClientMappingsRegistryProviderResourceDefinition().register(registration);

        new InfinispanTimerManagementResourceDefinition().register(registration);
    }
}
