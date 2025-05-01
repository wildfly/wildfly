/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a clustered cache configuration.
 * @author Paul Ferraro
 */
public class ClusteredCacheResourceDefinitionRegistrar extends CacheResourceDefinitionRegistrar {

    static final DurationAttributeDefinition REMOTE_TIMEOUT = new DurationAttributeDefinition.Builder("remote-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMillis(17500)).build();

    public ClusteredCacheResourceDefinitionRegistrar(CacheResourceRegistration registration) {
        super(registration);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(REMOTE_TIMEOUT))
                // Requires a JGroups transport
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(CAPABILITY, JGroupsTransportResourceDefinitionRegistrar.JGROUPS).withRequirementNameResolver(UnaryCapabilityNameResolver.PARENT).build())
                ;
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Duration remoteTimeout = REMOTE_TIMEOUT.resolve(context, model);
        return super.resolve(context, model).map(new UnaryOperator<>() {
            @Override
            public ConfigurationBuilder apply(ConfigurationBuilder builder) {
                builder.clustering().remoteTimeout(remoteTimeout.toMillis(), TimeUnit.MILLISECONDS);
                return builder;
            }
        });
    }
}
