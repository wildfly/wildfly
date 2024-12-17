/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.AuthToken;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public abstract class AuthTokenResourceDefinition<T extends AuthToken> extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator, ResourceModelResolver<Map.Entry<Function<String, T>, Consumer<RequirementServiceBuilder<?>>>> {
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String value) {
        return PathElement.pathElement("token", value);
    }

    static final BinaryServiceDescriptor<AuthToken> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.jgroups.auth-token", AuthToken.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SHARED_SECRET(CredentialReference.getAttributeBuilder("shared-secret-reference", null, false, CapabilityReferenceRecorder.builder(CAPABILITY, CommonServiceDescriptor.CREDENTIAL_STORE).build()).build()),
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

    private static final Function<CredentialSource, String> CREDENTIAL_SOURCE_MAPPER = new Function<>() {
        @Override
        public String apply(CredentialSource sharedSecretSource) {
            try {
                PasswordCredential credential = sharedSecretSource.getCredential(PasswordCredential.class);
                ClearPassword password = credential.getPassword(ClearPassword.class);
                return String.valueOf(password.getPassword());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    };

    protected final UnaryOperator<ResourceDescriptor> configurator;

    AuthTokenResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator) {
        super(path, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH));
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttribute(Attribute.SHARED_SECRET, new CredentialReferenceWriteAttributeHandler(Attribute.SHARED_SECRET.getDefinition()))
                .addCapabilities(List.of(CAPABILITY))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);
        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<CredentialSource> credentialSource = ServiceDependency.from(CredentialReference.getCredentialSourceDependency(context, Attribute.SHARED_SECRET.getDefinition(), model));
        Map.Entry<Function<String, T>, Consumer<RequirementServiceBuilder<?>>> entry = this.resolve(context, model);
        return CapabilityServiceInstaller.builder(CAPABILITY, CREDENTIAL_SOURCE_MAPPER.andThen(entry.getKey()), credentialSource)
                .requires(List.of(credentialSource, entry.getValue()))
                .build();
    }
}
