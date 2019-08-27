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

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.AddStepHandlerDescriptor;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.WriteAttributeStepHandler;
import org.jboss.as.clustering.controller.WriteAttributeStepHandlerDescriptor;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class CipherAuthTokenResourceDefinition extends AuthTokenResourceDefinition<CipherAuthToken> {

    static final PathElement PATH = pathElement("cipher");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        KEY_CREDENTIAL(CredentialReference.getAttributeBuilder("key-credential-reference", null, false, new CapabilityReference(Capability.AUTH_TOKEN, CommonUnaryRequirement.CREDENTIAL_STORE)).build()),
        KEY_ALIAS("key-alias", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true);
            }
        },
        KEY_STORE("key-store", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(new CapabilityReference(Capability.AUTH_TOKEN, CommonUnaryRequirement.KEY_STORE));
            }
        },
        ALGORITHM("algorithm", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true)
                        .setRequired(false)
                        .setDefaultValue(new ModelNode("RSA"))
                        ;
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

    CipherAuthTokenResourceDefinition() {
        super(PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), CipherAuthTokenServiceConfigurator::new);
    }

    @Override
    public org.jboss.as.controller.registry.ManagementResourceRegistration register(org.jboss.as.controller.registry.ManagementResourceRegistration parent) {
        org.jboss.as.controller.registry.ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(AuthTokenResourceDefinition.Attribute.class)
                .addCapabilities(Capability.class)
                ;
        new CipherAuthTokenResourceRegistration(descriptor, new SimpleResourceServiceHandler(this.serviceConfiguratorFactory)).register(registration);

        return registration;
    }

    private static class CipherAuthTokenResourceRegistration extends SimpleResourceRegistration {

        public CipherAuthTokenResourceRegistration(ResourceDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, new CipherAuthTokenAddStepHandler(descriptor, handler), new RemoveStepHandler(descriptor, handler), new CipherAuthTokenWriteAttributeStepHandler(descriptor));
        }
    }

    private static class CipherAuthTokenAddStepHandler extends AddStepHandler {

        public CipherAuthTokenAddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, handler);
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

    private static class CipherAuthTokenWriteAttributeStepHandler extends WriteAttributeStepHandler {

        private final WriteAttributeStepHandlerDescriptor descriptor;

        public CipherAuthTokenWriteAttributeStepHandler(WriteAttributeStepHandlerDescriptor descriptor) {
            super(descriptor, null);
            this.descriptor = descriptor;
        }

        @Override
        public void register(org.jboss.as.controller.registry.ManagementResourceRegistration registration) {
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
