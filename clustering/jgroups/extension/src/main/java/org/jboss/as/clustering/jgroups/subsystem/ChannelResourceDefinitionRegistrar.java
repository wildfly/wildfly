/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ModuleAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.JGroupsServiceDescriptor;
import org.wildfly.clustering.server.service.ChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.ProvidedUnaryServiceInstallerProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Registers a resource definition for a JGroups channel.
 *
 * @author Paul Ferraro
 */
public class ChannelResourceDefinitionRegistrar extends AbstractChannelResourceDefinitionRegistrar<ChannelConfiguration> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ChannelConfiguration.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    static final CapabilityReferenceAttributeDefinition<ChannelFactory> STACK = new CapabilityReferenceAttributeDefinition.Builder<>("stack", CapabilityReference.builder(CAPABILITY, ChannelFactory.SERVICE_DESCRIPTOR).build()).build();
    static final ModuleAttributeDefinition MODULE = new ModuleAttributeDefinition.Builder().setRequired(false).setDefaultValue(new ModelNode("org.wildfly.clustering.server")).build();
    static final AttributeDefinition CLUSTER = new SimpleAttributeDefinitionBuilder("cluster", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();

    private final ServiceValueExecutorRegistry<JChannel> channelRegistry;

    ChannelResourceDefinitionRegistrar() {
        this(ServiceValueExecutorRegistry.newInstance());
    }

    private ChannelResourceDefinitionRegistrar(ServiceValueExecutorRegistry<JChannel> channelRegistry) {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return JGroupsResourceRegistration.CHANNEL;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }

            @Override
            public ResourceModelResolver<ServiceDependency<ForkChannelFactoryConfiguration>> getForkChannelFactoryConfigurationResolver() {
                return new ResourceModelResolver<>() {
                    @Override
                    public ServiceDependency<ForkChannelFactoryConfiguration> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                        String name = context.getCurrentAddressValue();
                        return ServiceDependency.on(JGroupsServiceDescriptor.CHANNEL, name).combine(ServiceDependency.on(ChannelConfiguration.SERVICE_DESCRIPTOR, name), new BiFunction<>() {
                            @Override
                            public ForkChannelFactoryConfiguration apply(JChannel channel, ChannelConfiguration configuration) {
                                return new ForkChannelFactoryConfiguration() {
                                    @Override
                                    public JChannel getChannel() {
                                        return channel;
                                    }

                                    @Override
                                    public ChannelConfiguration getChannelConfiguration() {
                                        return configuration;
                                    }
                                };
                            }
                        });
                    }
                };
            }

            @Override
            public ResourceModelResolver<ServiceDependency<ChannelConfiguration>> getChannelConfigurationResolver() {
                return new ResourceModelResolver<>() {
                    @Override
                    public ServiceDependency<ChannelConfiguration> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                        String name = context.getCurrentAddressValue();
                        String clusterName = CLUSTER.resolveModelAttribute(context, model).asString(name);
                        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);
                        return STACK.resolve(context, model).combine(MODULE.resolve(context, model), new BiFunction<>() {
                            @Override
                            public ChannelConfiguration apply(ChannelFactory factory, Module module) {
                                return new ChannelConfiguration() {
                                    @Override
                                    public boolean isStatisticsEnabled() {
                                        return statisticsEnabled;
                                    }

                                    @Override
                                    public ChannelFactory getChannelFactory() {
                                        return factory;
                                    }

                                    @Override
                                    public Module getModule() {
                                        return module;
                                    }

                                    @Override
                                    public String getClusterName() {
                                        return clusterName;
                                    }
                                };
                            }
                        });
                    }
                };
            }

            @Override
            public ResourceModelResolver<PathAddress> getStackAddressResolver() {
                return new ResourceModelResolver<>() {
                    @Override
                    public PathAddress resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                        String stack = STACK.resolveModelAttribute(context, model).asString();
                        return context.getCurrentAddress().getParent().append(JGroupsResourceRegistration.STACK.pathElement(stack));
                    }
                };
            }

            @Override
            public UnaryOperator<OperationStepHandler> getAddOperationTransformation() {
                return new UnaryOperator<>() {
                    @Override
                    public OperationStepHandler apply(OperationStepHandler handler) {
                        return new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                // Add operations emitted by legacy Infinispan subsystem may not have a stack specified
                                // In this case, fix operation to use stack of default channel
                                if (!operation.hasDefined(STACK.getName())) {
                                    PathAddress subsystemAddress = context.getCurrentAddress().getParent();
                                    Resource root = context.readResourceFromRoot(subsystemAddress.getParent(), false);
                                    if (!root.hasChild(subsystemAddress.getLastElement())) {
                                        // Subsystem not yet added - defer operation execution
                                        context.addStep(operation, this, context.getCurrentStage());
                                        return;
                                    }
                                    Resource subsystem = context.readResourceFromRoot(subsystemAddress, false);
                                    ModelNode subsystemModel = subsystem.getModel();
                                    if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL.getName())) {
                                        String defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinitionRegistrar.DEFAULT_CHANNEL.getName()).asString();
                                        if (!context.getCurrentAddressValue().equals(defaultChannel)) {
                                            PathElement defaultChannelPath = JGroupsResourceRegistration.CHANNEL.pathElement(defaultChannel);
                                            if (!subsystem.hasChild(defaultChannelPath)) {
                                                // Default channel was not yet added, defer operation execution
                                                context.addStep(operation, this, context.getCurrentStage());
                                                return;
                                            }
                                            Resource channel = context.readResourceFromRoot(subsystemAddress.append(defaultChannelPath), false);
                                            ModelNode channelModel = channel.getModel();
                                            String defaultStack = channelModel.get(STACK.getName()).asString();
                                            operation.get(STACK.getName()).set(defaultStack);
                                        }
                                    }
                                }

                                handler.execute(context, operation);
                            }
                        };
                    }
                };
            }
        }, channelRegistry);
        this.channelRegistry = channelRegistry;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        RuntimeCapability<Void> commandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> legacyCommandDispatcherFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();

        RuntimeCapability<Void> group = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setAllowMultipleRegistrations(true).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> legacyGroup = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.GROUP).setAllowMultipleRegistrations(true).build();

        return super.apply(builder)
                .addAttributes(List.of(STACK, MODULE, CLUSTER, STATISTICS_ENABLED))
                .addCapabilities(List.of(commandDispatcherFactory, legacyCommandDispatcherFactory, group, legacyGroup));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new MetricOperationStepHandler<>(new ChannelMetricExecutor(this.channelRegistry), ChannelMetric.class).register(registration);
        }

        new ForkResourceDefinitionRegistrar(this.channelRegistry).register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        List<ResourceServiceInstaller> installers = new LinkedList<>();

        installers.add(super.configure(context, model));

        new ProvidedUnaryServiceInstallerProvider<>(ChannelServiceInstallerProvider.class, ChannelServiceInstallerProvider.class.getClassLoader()).apply(name).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }
}
