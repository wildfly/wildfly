/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.infinispan.transport.ChannelConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactoryConfiguration;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/transport=jgroups
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public enum JGroupsTransportResourceDescription implements TransportResourceDescription {
    INSTANCE;

    private static final PathElement PATH = TransportResourceDescription.pathElement("jgroups");

    static final UnaryServiceDescriptor<Void> JGROUPS = UnaryServiceDescriptor.of(String.join(".", InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION.getName(), PATH.getKey(), PATH.getValue()), Void.class);
    private static final RuntimeCapability<Void> JGROUPS_CAPABILITY = RuntimeCapability.Builder.of(JGROUPS).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    static final CapabilityReferenceAttributeDefinition<ForkChannelFactory> CHANNEL_FACTORY = new CapabilityReferenceAttributeDefinition.Builder<>("channel", CapabilityReference.builder(CAPABILITY, ForkChannelFactory.SERVICE_DESCRIPTOR).build()).setRequired(false).build();
    static final DurationAttributeDefinition LOCK_TIMEOUT = new DurationAttributeDefinition.Builder("lock-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(4)).build();

    @Override
    public PathElement getPathElement() {
        return PATH;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return TransportResourceDescription.super.apply(builder)
                .addCapability(JGROUPS_CAPABILITY)
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(JGROUPS_CAPABILITY, ForkChannelFactory.DEFAULT_SERVICE_DESCRIPTOR).build())
                ;
    }

    @Override
    public String resolveGroupName(OperationContext context, ModelNode model) throws OperationFailedException {
        return CHANNEL_FACTORY.resolveModelAttribute(context, model).asStringOrNull();
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(CHANNEL_FACTORY, LOCK_TIMEOUT);
    }

    @Override
    public ServiceDependency<TransportConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String containerName = context.getCurrentAddress().getParent().getLastElement().getValue();
        Duration lockTimeout = LOCK_TIMEOUT.resolve(context, model);
        return CHANNEL_FACTORY.resolve(context, model).map(new Function<>() {
            @Override
            public TransportConfigurationBuilder apply(ForkChannelFactory factory) {
                Properties properties = new Properties();
                properties.put(JGroupsTransport.CHANNEL_CONFIGURATOR, new ChannelConfigurator(factory, containerName));
                ForkChannelFactoryConfiguration configuration = factory.getConfiguration();
                org.wildfly.clustering.jgroups.spi.TransportConfiguration.Topology topology = configuration.getTransport().getTopology();
                TransportConfigurationBuilder builder = new GlobalConfigurationBuilder().transport()
                        .clusterName(configuration.getChannelConfiguration().getClusterName())
                        .distributedSyncTimeout(lockTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        .transport(new JGroupsTransport())
                        .withProperties(properties)
                        ;
                if (topology != null) {
                    builder.siteId(topology.getSite()).rackId(topology.getRack()).machineId(topology.getMachine());
                }
                return builder;
            }
        });
    }
}
