/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.ClusteredCacheServiceInstallerProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a clustered cache resource.
 * @author Paul Ferraro
 */
public interface ClusteredCacheResourceDescription extends CacheResourceDescription<ClusteredCacheServiceInstallerProvider> {

    DurationAttributeDefinition REMOTE_TIMEOUT = new DurationAttributeDefinition.Builder("remote-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMillis(17500)).build();

    @Override
    default Class<ClusteredCacheServiceInstallerProvider> getProviderClass() {
        return ClusteredCacheServiceInstallerProvider.class;
    }

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return CacheResourceDescription.super.apply(builder)
                // Requires a JGroups transport
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(CAPABILITY, JGroupsTransportResourceDescription.JGROUPS).withRequirementNameResolver(UnaryCapabilityNameResolver.PARENT).build())
                ;
    }

    @Override
    default Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(CacheResourceDescription.super.getAttributes(), Stream.of(REMOTE_TIMEOUT));
    }

    @Override
    default ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Duration remoteTimeout = REMOTE_TIMEOUT.resolve(context, model);
        return CacheResourceDescription.super.resolve(context, model).map(new UnaryOperator<>() {
            @Override
            public ConfigurationBuilder apply(ConfigurationBuilder builder) {
                builder.clustering().remoteTimeout(remoteTimeout.toMillis(), TimeUnit.MILLISECONDS);
                return builder;
            }
        });
    }
}
