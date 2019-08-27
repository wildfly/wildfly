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

import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;

import java.security.KeyStore;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.AddStepHandlerDescriptor;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.RestartParentResourceAddStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRegistration;
import org.jboss.as.clustering.controller.RestartParentResourceRemoveStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceWriteAttributeHandler;
import org.jboss.as.clustering.controller.WriteAttributeStepHandlerDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
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

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(AbstractProtocolResourceDefinition.Attribute.class)
                .addExtraParameters(DeprecatedAttribute.class)
                ;
        new ProtocolResourceRegistration(this.parentServiceConfiguratorFactory, descriptor, this.handler).register(registration);

        if (registration.getPathAddress().getLastElement().isWildcard()) {
            new PropertyResourceDefinition().register(registration);
        }

        return registration;
    }

    private static class ProtocolResourceRegistration extends RestartParentResourceRegistration {

        public ProtocolResourceRegistration(ResourceServiceConfiguratorFactory parentFactory, ResourceDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, new ProtocolAddStepHandler(parentFactory, descriptor, handler), new RestartParentResourceRemoveStepHandler(parentFactory, descriptor, handler), new ProtocolWriteAttributeHandler(parentFactory, descriptor));
        }
    }

    private static class ProtocolAddStepHandler extends RestartParentResourceAddStepHandler {

        public ProtocolAddStepHandler(ResourceServiceConfiguratorFactory parentFactory, AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
            super(parentFactory, descriptor, handler);
        }

        @Override
        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            final ModelNode model = resource.getModel();
            handleCredentialReferenceUpdate(context, model.get(Attribute.KEY_CREDENTIAL.getName()), Attribute.KEY_CREDENTIAL.getName());
        }

        @Override
        protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
            rollbackCredentialStoreUpdate(Attribute.KEY_CREDENTIAL.getDefinition(), context, resource);
            super.rollbackRuntime(context, operation, resource);
        }
    }

    private static class ProtocolWriteAttributeHandler extends RestartParentResourceWriteAttributeHandler {

        private final WriteAttributeStepHandlerDescriptor descriptor;

        public ProtocolWriteAttributeHandler(ResourceServiceConfiguratorFactory parentFactory, WriteAttributeStepHandlerDescriptor descriptor) {
            super(parentFactory, descriptor);
            this.descriptor = descriptor;
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            CredentialReferenceWriteAttributeHandler credentialReferenceWriteAttributeHandler = new CredentialReferenceWriteAttributeHandler(Attribute.KEY_CREDENTIAL.getDefinition());
            for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
                if (attribute.equals(Attribute.KEY_CREDENTIAL.getDefinition())) {
                    registration.registerReadWriteAttribute(attribute, null, credentialReferenceWriteAttributeHandler);
                } else {
                    registration.registerReadWriteAttribute(attribute, null, this);
                }
            }
        }

    }
}
