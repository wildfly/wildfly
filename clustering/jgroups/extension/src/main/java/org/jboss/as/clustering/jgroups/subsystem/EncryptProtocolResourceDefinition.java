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

import java.security.KeyStore;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * Resource definition override for protocols that require an encryption key.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceDefinition<E extends KeyStore.Entry> extends ProtocolResourceDefinition {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        KEY_CREDENTIAL(CredentialReference.getAttributeBuilder("key-credential-reference", null, false, new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.CREDENTIAL_STORE)).build()),
        KEY_ALIAS("key-alias", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true);
            }
        },
        KEY_STORE("key-store", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(new CapabilityReference(Capability.PROTOCOL, CommonUnaryRequirement.KEY_STORE));
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        Attribute(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
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
                    .addAttributes(Attribute.class)
                    .setAddOperationTransformation(new LegacyAddOperationTransformation(Attribute.class))
                    .setOperationTransformation(LEGACY_OPERATION_TRANSFORMER)
                    ;
        }
    }

    private static class EncryptProtocolConfigurationConfiguratorFactory<E extends KeyStore.Entry> implements ResourceServiceConfiguratorFactory {
        private final Class<E> entryClass;

        EncryptProtocolConfigurationConfiguratorFactory(Class<E> entryClass) {
            this.entryClass = entryClass;
        }

        @Override
        public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
            return new EncryptProtocolConfigurationServiceConfigurator<>(address, this.entryClass);
        }
    }

    public EncryptProtocolResourceDefinition(String name, Class<E> entryClass, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory parentServiceConfiguratorFactory) {
        super(pathElement(name), new ResourceDescriptorConfigurator(configurator), new EncryptProtocolConfigurationConfiguratorFactory<>(entryClass), parentServiceConfiguratorFactory);
    }
}
