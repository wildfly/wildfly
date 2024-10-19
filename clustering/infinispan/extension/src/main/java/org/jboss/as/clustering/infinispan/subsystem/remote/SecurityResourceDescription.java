/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SecurityConfiguration;
import org.infinispan.client.hotrod.configuration.SecurityConfigurationBuilder;
import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 *
 */
public enum SecurityResourceDescription implements RemoteCacheContainerComponentResourceDescription<SecurityConfiguration, SecurityConfigurationBuilder> {
    INSTANCE;

    private static final PathElement PATH = ComponentResourceDescription.pathElement("security");
    static final UnaryServiceDescriptor<SecurityConfiguration> SERVICE_DESCRIPTOR = RemoteCacheContainerComponentResourceDescription.createServiceDescriptor(PATH, SecurityConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    public static final CapabilityReferenceAttributeDefinition<SSLContext> SSL_CONTEXT = new CapabilityReferenceAttributeDefinition.Builder<>("ssl-context", CapabilityReference.builder(CAPABILITY, CommonServiceDescriptor.SSL_CONTEXT).build())
            .setRequired(false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SSL_REF)
            .build();

    @Override
    public PathElement getPathElement() {
        return PATH;
    }

    @Override
    public UnaryServiceDescriptor<SecurityConfiguration> getServiceDescriptor() {
        return SERVICE_DESCRIPTOR;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(SSL_CONTEXT);
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
