/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CredentialReferenceAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.BinaryCapabilityNameResolver;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.AuthToken;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for an authentication token for use by the AUTH protocol.
 * @author Paul Ferraro
 */
public abstract class AuthTokenResourceDefinitionRegistrar<T extends AuthToken> implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<ServiceDependency<Function<byte[], T>>>, UnaryOperator<ResourceDescriptor.Builder> {

    enum Token implements ResourceRegistration {
        WILDCARD(PathElement.WILDCARD_VALUE),
        PLAIN("plain"),
        DIGEST("digest"),
        CIPHER("cipher"),
        ;
        private final PathElement path;

        Token(String value) {
            this.path = PathElement.pathElement("token", value);
        }

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    static final BinaryServiceDescriptor<AuthToken> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of("org.wildfly.clustering.jgroups.auth-token", AuthToken.class);
    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(BinaryCapabilityNameResolver.GRANDPARENT_PARENT).setAllowMultipleRegistrations(true).build();

    static final CredentialReferenceAttributeDefinition SHARED_SECRET = new CredentialReferenceAttributeDefinition.Builder("shared-secret-reference", CAPABILITY).build();

    static final Function<CredentialSource, char[]> CLEAR_PASSWORD_CREDENTIAL = new Function<>() {
        @Override
        public char[] apply(CredentialSource sharedSecretSource) {
            try {
                PasswordCredential credential = sharedSecretSource.getCredential(PasswordCredential.class);
                ClearPassword password = credential.getPassword(ClearPassword.class);
                return password.getPassword();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    };
    private static final Function<char[], byte[]> CHARS_TO_BYTES = new Function<>() {
        @Override
        public byte[] apply(char[] value) {
            return String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        }
    };

    private final ResourceRegistration registration;

    AuthTokenResourceDefinitionRegistrar(Token registration) {
        this.registration = registration;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.registration.getPathElement(), Token.WILDCARD.getPathElement());
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver))
                .addCapability(CAPABILITY)
                .addAttribute(SHARED_SECRET, CredentialReferenceWriteAttributeHandler.INSTANCE)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();
        ResourceDefinition definition = ResourceDefinition.builder(this.registration, resolver).build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<Function<byte[], T>> tokenFactory = this.resolve(context, model);
        ServiceDependency<byte[]> secret = SHARED_SECRET.resolve(context, model).map(CLEAR_PASSWORD_CREDENTIAL).map(CHARS_TO_BYTES);
        return CapabilityServiceInstaller.builder(CAPABILITY, tokenFactory.combine(secret, Function::apply)).blocking().build();
    }
}
