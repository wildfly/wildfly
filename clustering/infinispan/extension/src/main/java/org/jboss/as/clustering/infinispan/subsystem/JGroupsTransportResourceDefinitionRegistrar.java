/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.infinispan.transport.ChannelConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactoryConfiguration;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for the JGroups transport of a cache container.
 * @author Paul Ferraro
 */
public class JGroupsTransportResourceDefinitionRegistrar extends TransportResourceDefinitionRegistrar {

    static final UnaryServiceDescriptor<Void> JGROUPS = UnaryServiceDescriptor.of(String.join(".", InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION.getName(), TransportResourceRegistration.JGROUPS.getPathElement().getKey(), TransportResourceRegistration.JGROUPS.getPathElement().getValue()), Void.class);
    private static final RuntimeCapability<Void> JGROUPS_CAPABILITY = RuntimeCapability.Builder.of(JGROUPS).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    static final CapabilityReferenceAttributeDefinition<ForkChannelFactory> CHANNEL_FACTORY = new CapabilityReferenceAttributeDefinition.Builder<>("channel", CapabilityReference.builder(CAPABILITY, ForkChannelFactory.SERVICE_DESCRIPTOR).build()).setRequired(false).build();
    static final DurationAttributeDefinition LOCK_TIMEOUT = DurationAttributeDefinition.builder("lock-timeout", ChronoUnit.MILLIS).setDefaultValue(Duration.ofMinutes(4)).build();

    JGroupsTransportResourceDefinitionRegistrar() {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return TransportResourceRegistration.JGROUPS;
            }

            @Override
            public String resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return CHANNEL_FACTORY.resolveModelAttribute(context, model).asStringOrNull();
            }

            @Override
            public CapabilityServiceInstaller.Builder<TransportConfiguration, TransportConfiguration> apply(CapabilityServiceInstaller.Builder<TransportConfiguration, TransportConfiguration> builder) {
                // Allow cache manager to auto-start when the channel starts
                return builder.startWhen(StartWhen.AVAILABLE);
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(CHANNEL_FACTORY, LOCK_TIMEOUT))
                .addCapability(JGROUPS_CAPABILITY)
                .addResourceCapabilityReference(ResourceCapabilityReference.builder(JGROUPS_CAPABILITY, ForkChannelFactory.DEFAULT_SERVICE_DESCRIPTOR).build())
                ;
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
                topology.getSite().ifPresent(builder::siteId);
                topology.getRack().ifPresent(builder::rackId);
                topology.getSite().ifPresent(builder::siteId);
                return builder;
            }
        });
    }
}
