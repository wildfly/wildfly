/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceCapabilityReference;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.conf.ClassConfigurator;

/**
 * @author Paul Ferraro
 */
public class AuthProtocolResourceDefinition extends ProtocolResourceDefinition {

    static {
        ClassConfigurator.add((short) 1100, BinaryAuthToken.class);
        ClassConfigurator.add((short) 1101, CipherAuthToken.class);
    }

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .setAddOperationTransformation(new LegacyAddOperationTransformation("auth_class"))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    .addResourceCapabilityReference(new ResourceCapabilityReference(Capability.PROTOCOL, AuthTokenResourceDefinition.Capability.AUTH_TOKEN, UnaryCapabilityNameResolver.PARENT))
                    ;
        }
    }

    AuthProtocolResourceDefinition(String name, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), AuthProtocolConfigurationServiceConfigurator::new, parentServiceConfiguratorFactory);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PlainAuthTokenResourceDefinition().register(registration);
        new DigestAuthTokenResourceDefinition().register(registration);
        new CipherAuthTokenResourceDefinition().register(registration);

        return registration;
    }
}
