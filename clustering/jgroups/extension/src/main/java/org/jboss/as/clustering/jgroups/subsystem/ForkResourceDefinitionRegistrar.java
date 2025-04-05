/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ForkChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Registers a resource definition for a fork channel.
 * @author Paul Ferraro
 */
public class ForkResourceDefinitionRegistrar extends AbstractChannelResourceDefinitionRegistrar<ForkChannelConfiguration> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ForkChannelConfiguration.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    static final ResourceCapabilityReference<ForkChannelFactory> CHANNEL_FACTORY_REFERENCE = ResourceCapabilityReference.builder(CAPABILITY, ForkChannelFactory.SERVICE_DESCRIPTOR).withRequirementNameResolver(UnaryCapabilityNameResolver.PARENT).build();

    private final ResourceServiceConfigurator configurator;

    ForkResourceDefinitionRegistrar(ServiceValueExecutorRegistry<JChannel> registry) {
        this(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return JGroupsResourceRegistration.FORK;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }

            @Override
            public ResourceModelResolver<ServiceDependency<ForkChannelConfiguration>> getChannelConfigurationResolver() {
                return new ResourceModelResolver<>() {
                    @Override
                    public ServiceDependency<ForkChannelConfiguration> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                        return ServiceDependency.on(ForkChannelFactory.SERVICE_DESCRIPTOR, context.getCurrentAddressValue()).map(new Function<>() {
                            @Override
                            public ForkChannelConfiguration apply(ForkChannelFactory factory) {
                                return new ForkChannelConfiguration() {
                                    @Override
                                    public ForkChannelFactory getChannelFactory() {
                                        return factory;
                                    }
                                };
                            }
                        });
                    }
                };
            }

            @Override
            public ResourceModelResolver<ServiceDependency<ForkChannelFactoryConfiguration>> getForkChannelFactoryConfigurationResolver() {
                return new ResourceModelResolver<>() {
                    @Override
                    public ServiceDependency<ForkChannelFactoryConfiguration> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                        String name = context.getCurrentAddressValue();
                        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                        Set<Resource.ResourceEntry> entries = resource.getChildren(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement().getKey());
                        List<ServiceDependency<ProtocolConfiguration<Protocol>>> protocols = new ArrayList<>(entries.size());
                        for (Resource.ResourceEntry entry : entries) {
                            protocols.add(ServiceDependency.on(ProtocolConfiguration.SERVICE_DESCRIPTOR, name, entry.getName()));
                        }
                        ServiceDependency<ForkChannelFactory> channelFactory = CHANNEL_FACTORY_REFERENCE.resolve(context, resource);
                        return new ServiceDependency<>() {
                            @Override
                            public void accept(RequirementServiceBuilder<?> builder) {
                                channelFactory.accept(builder);
                                for (ServiceDependency<ProtocolConfiguration<Protocol>> protocol : protocols) {
                                    protocol.accept(builder);
                                }
                            }

                            @Override
                            public ForkChannelFactoryConfiguration get() {
                                return new ForkChannelFactoryConfiguration() {
                                    @Override
                                    public JChannel getChannel() {
                                        return channelFactory.get().getConfiguration().getChannel();
                                    }

                                    @Override
                                    public ChannelConfiguration getChannelConfiguration() {
                                        return channelFactory.get().getConfiguration().getChannelConfiguration();
                                    }

                                    @Override
                                    public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
                                        return !protocols.isEmpty() ? protocols.stream().map(Supplier::get).collect(Collectors.toList()) : List.of();
                                    }
                                };
                            }
                        };
                    }
                };
            }

            @Override
            public ResourceModelResolver<PathAddress> getStackAddressResolver() {
                return new ResourceModelResolver<>() {
                    @Override
                    public PathAddress resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                        return context.getCurrentAddress();
                    }
                };
            }
        }, registry);
    }

    ForkResourceDefinitionRegistrar(Configurator<ForkChannelConfiguration> configurator, ServiceValueExecutorRegistry<JChannel> channelRegistry) {
        super(configurator, channelRegistry);
        this.configurator = configurator;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addResourceCapabilityReference(CHANNEL_FACTORY_REFERENCE);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new ProtocolResourceDefinitionRegistrar(ResourceOperationRuntimeHandler.restartParent(ResourceOperationRuntimeHandler.configureService(this.configurator))).register(registration, context);

        return registration;
    }
}
