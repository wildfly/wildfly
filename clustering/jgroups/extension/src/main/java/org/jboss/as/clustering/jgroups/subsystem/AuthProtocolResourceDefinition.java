/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceCapabilityReference;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jgroups.conf.ClassConfigurator;

/**
 * @author Paul Ferraro
 */
public class AuthProtocolResourceDefinition extends ProtocolResourceDefinition {

    static {
        ClassConfigurator.add((short) 1100, BinaryAuthToken.class);
        ClassConfigurator.add((short) 1101, CipherAuthToken.class);
    }

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
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
