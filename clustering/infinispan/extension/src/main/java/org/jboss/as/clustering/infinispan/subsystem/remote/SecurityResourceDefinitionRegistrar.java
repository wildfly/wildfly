/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.infinispan.subsystem.ConfigurationResourceDefinitionRegistrar;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition of the security component of a remote cache container.
 * @author Paul Ferraro
 */
public class SecurityResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<SecurityConfiguration, SecurityConfigurationBuilder> {

    static final UnaryServiceDescriptor<SecurityConfiguration> SERVICE_DESCRIPTOR = UnaryServiceDescriptorFactory.createServiceDescriptor(RemoteComponentResourceRegistration.SECURITY, SecurityConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).setAllowMultipleRegistrations(true).build();

    public static final CapabilityReferenceAttributeDefinition<SSLContext> SSL_CONTEXT = new CapabilityReferenceAttributeDefinition.Builder<>(ModelDescriptionConstants.SSL_CONTEXT, CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.SSL_CONTEXT).build())
            .setRequired(false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .build();

    SecurityResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return RemoteComponentResourceRegistration.SECURITY;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(SSL_CONTEXT));
    }

    @Override
    public ServiceDependency<SecurityConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return SSL_CONTEXT.resolve(context, model).map(new Function<>() {
            @Override
            public SecurityConfigurationBuilder apply(SSLContext context) {
                return new ConfigurationBuilder().security().ssl()
                        .hostnameValidation(false)
                        .sslContext(context)
                        .enabled(context != null) // Set this last!
                        .security();
            }
        });
    }
}
