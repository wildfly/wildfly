/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jgroups.auth.AuthToken;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;
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
public abstract class AuthTokenResourceDefinitionRegistrar<T extends AuthToken> implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceModelResolver<ServiceDependency<Function<byte[], T>>> {

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

    interface TokenResourceRegistration extends ResourceRegistration, UnaryOperator<ResourceDescriptor.Builder> {
    }

    private final AuthTokenResourceDescription description;

    AuthTokenResourceDefinitionRegistrar(AuthTokenResourceDescription description) {
        this.description = description;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.description.getPathElement(), this.description.getPathKey());
        ResourceDescriptor descriptor = this.description.apply(ResourceDescriptor.builder(resolver))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();

        ResourceDefinition definition = ResourceDefinition.builder(this.description, resolver).build();

        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<Function<byte[], T>> tokenFactory = this.resolve(context, model);
        ServiceDependency<byte[]> secret = AuthTokenResourceDescription.SHARED_SECRET.resolve(context, model).map(CLEAR_PASSWORD_CREDENTIAL).map(CHARS_TO_BYTES);
        return CapabilityServiceInstaller.builder(AuthTokenResourceDescription.CAPABILITY, tokenFactory.combine(secret, Function::apply)).blocking().build();
    }
}
