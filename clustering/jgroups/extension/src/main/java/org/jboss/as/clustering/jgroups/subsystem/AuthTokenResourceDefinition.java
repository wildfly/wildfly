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

import static org.jboss.as.clustering.jgroups.subsystem.AuthTokenResourceDefinition.Attribute.SHARED_SECRET;
import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.AddStepHandlerDescriptor;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.WriteAttributeStepHandler;
import org.jboss.as.clustering.controller.WriteAttributeStepHandlerDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.AuthToken;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * @author Paul Ferraro
 */
public class AuthTokenResourceDefinition<T extends AuthToken> extends ChildResourceDefinition<ManagementResourceRegistration> {
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("token", value);
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability, UnaryRequirement {
        AUTH_TOKEN("org.wildfly.clustering.jgroups.auth-token", AuthToken.class),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name, Class<?> type) {
            this.definition = RuntimeCapability.Builder.of(name, true).setServiceType(type).setAllowMultipleRegistrations(true).setDynamicNameMapper(UnaryCapabilityNameResolver.GRANDPARENT).build();
        }

        @Override
        public RuntimeCapability<?> getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SHARED_SECRET(CredentialReference.getAttributeBuilder("shared-secret-reference", null, false, new CapabilityReference(Capability.AUTH_TOKEN, CommonUnaryRequirement.CREDENTIAL_STORE)).build()),
        ;
        private final AttributeDefinition definition;

        Attribute(AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void addTransformations(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        ProtocolResourceDefinition.addTransformations(version, builder);
    }

    protected final UnaryOperator<ResourceDescriptor> configurator;
    protected final ResourceServiceConfiguratorFactory serviceConfiguratorFactory;

    AuthTokenResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory) {
        super(path, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH));
        this.configurator = configurator;
        this.serviceConfiguratorFactory = serviceConfiguratorFactory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                ;
        new TokenResourceRegistration(descriptor, new SimpleResourceServiceHandler(this.serviceConfiguratorFactory)).register(registration);

        return registration;
    }

    private static class TokenResourceRegistration extends SimpleResourceRegistration {

        public TokenResourceRegistration(ResourceDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, new TokenAddStepHandler(descriptor, handler), new RemoveStepHandler(descriptor, handler), new TokenWriteAttributeStepHandler(descriptor));
        }
    }

    private static class TokenAddStepHandler extends AddStepHandler {

        public TokenAddStepHandler(AddStepHandlerDescriptor descriptor, ResourceServiceHandler handler) {
            super(descriptor, handler);
        }

        @Override
        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
            super.populateModel(context, operation, resource);
            final ModelNode model = resource.getModel();
            handleCredentialReferenceUpdate(context, model.get(SHARED_SECRET.getName()), SHARED_SECRET.getName());
        }

        @Override
        protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
            rollbackCredentialStoreUpdate(SHARED_SECRET.getDefinition(), context, resource);
            super.rollbackRuntime(context, operation, resource);
        }
    }

    private static class TokenWriteAttributeStepHandler extends WriteAttributeStepHandler {

        private final WriteAttributeStepHandlerDescriptor descriptor;

        public TokenWriteAttributeStepHandler(WriteAttributeStepHandlerDescriptor descriptor) {
            super(descriptor, null);
            this.descriptor = descriptor;
        }

        @Override
        public void register(ManagementResourceRegistration registration) {
            CredentialReferenceWriteAttributeHandler credentialReferenceWriteAttributeHandler = new CredentialReferenceWriteAttributeHandler(SHARED_SECRET.getDefinition());
            for (AttributeDefinition attribute : this.descriptor.getAttributes()) {
                if (attribute.equals(SHARED_SECRET.getDefinition())) {
                    registration.registerReadWriteAttribute(attribute, null, credentialReferenceWriteAttributeHandler);
                } else {
                    registration.registerReadWriteAttribute(attribute, null, this);
                }
            }
        }

    }
}
