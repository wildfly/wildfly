/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.ModuleServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModuleNameValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.server.service.ChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.ProvidedUnaryServiceInstallerProvider;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Definition for /subsystem=jgroups/channel=* resources
 *
 * @author Paul Ferraro
 */
public class ChannelResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> implements ResourceServiceConfigurator {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("channel", name);
    }

    static final UnaryServiceDescriptor<JChannel> CHANNEL = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel", JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL);
    static final UnaryServiceDescriptor<Module> CHANNEL_MODULE = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel-module", JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL_MODULE);
    static final UnaryServiceDescriptor<ChannelFactory> CHANNEL_SOURCE = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel-source", JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL_SOURCE);
    static final UnaryServiceDescriptor<String> CHANNEL_CLUSTER = UnaryServiceDescriptor.of("org.wildfly.clustering.jgroups.channel-cluster", JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL_CLUSTER);

    static final RuntimeCapability<Void> CHANNEL_CAPABILITY = RuntimeCapability.Builder.of(CHANNEL).setAllowMultipleRegistrations(true).build();
    static final RuntimeCapability<Void> CHANNEL_FACTORY_CAPABILITY = RuntimeCapability.Builder.of(ChannelFactory.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();
    private static final RuntimeCapability<Void> CHANNEL_MODULE_CAPABILITY = RuntimeCapability.Builder.of(CHANNEL_MODULE).build();
    private static final RuntimeCapability<Void> CHANNEL_SOURCE_CAPABILITY = RuntimeCapability.Builder.of(CHANNEL_SOURCE).build();
    private static final RuntimeCapability<Void> CHANNEL_CLUSTER_CAPABILITY = RuntimeCapability.Builder.of(CHANNEL_CLUSTER).build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        STACK("stack", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(true)
                        .setAllowExpression(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CHANNEL_FACTORY_CAPABILITY, ChannelFactory.SERVICE_DESCRIPTOR).build())
                        ;
            }
        },
        MODULE(ModelDescriptionConstants.MODULE, ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode("org.wildfly.clustering.server"))
                        .setValidator(ModuleNameValidator.INSTANCE)
                        ;
            }
        },
        CLUSTER("cluster", ModelType.STRING),
        STATISTICS_ENABLED(ModelDescriptionConstants.STATISTICS_ENABLED, ModelType.BOOLEAN) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(ModelNode.FALSE);
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    private final ServiceValueExecutorRegistry<JChannel> registry = ServiceValueExecutorRegistry.newInstance();

    ChannelResourceDefinition() {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        RuntimeCapability<Void> commandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> legacyCommandDispatcherFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();

        RuntimeCapability<Void> group = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.GROUP).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> legacyGroup = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.GROUP).setAllowMultipleRegistrations(true).build();

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(List.of(CHANNEL_CAPABILITY, CHANNEL_FACTORY_CAPABILITY, CHANNEL_MODULE_CAPABILITY, CHANNEL_SOURCE_CAPABILITY, CHANNEL_CLUSTER_CAPABILITY, commandDispatcherFactory, legacyCommandDispatcherFactory, group, legacyGroup))
                .addRuntimeResourceRegistration(new ChannelRuntimeResourceRegistration(this.registry))
                .setAddOperationTransformation(DefaultStackOperationStepHandler::new)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        if (registration.isRuntimeOnlyRegistrationValid()) {
            new MetricHandler<>(new ChannelMetricExecutor(this.registry), ChannelMetric.class).register(registration);
        }

        new ForkResourceDefinition(this.registry).register(registration);

        return registration;
    }

    private static class DefaultStackOperationStepHandler implements OperationStepHandler {
        private final OperationStepHandler handler;

        DefaultStackOperationStepHandler(OperationStepHandler handler) {
            this.handler = handler;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            // Add operations emitted by legacy Infinispan subsystem may not have a stack specified
            // In this case, fix operation to use stack of default channel
            if (!operation.hasDefined(Attribute.STACK.getName())) {
                PathAddress subsystemAddress = context.getCurrentAddress().getParent();
                Resource root = context.readResourceFromRoot(subsystemAddress.getParent(), false);
                if (!root.hasChild(subsystemAddress.getLastElement())) {
                    // Subsystem not yet added - defer operation execution
                    context.addStep(operation, this, context.getCurrentStage());
                    return;
                }
                Resource subsystem = context.readResourceFromRoot(subsystemAddress, false);
                ModelNode subsystemModel = subsystem.getModel();
                if (subsystemModel.hasDefined(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName())) {
                    String defaultChannel = subsystemModel.get(JGroupsSubsystemResourceDefinition.Attribute.DEFAULT_CHANNEL.getName()).asString();
                    if (!context.getCurrentAddressValue().equals(defaultChannel)) {
                        PathElement defaultChannelPath = pathElement(defaultChannel);
                        if (!subsystem.hasChild(defaultChannelPath)) {
                            // Default channel was not yet added, defer operation execution
                            context.addStep(operation, this, context.getCurrentStage());
                            return;
                        }
                        Resource channel = context.readResourceFromRoot(subsystemAddress.append(defaultChannelPath), false);
                        ModelNode channelModel = channel.getModel();
                        String defaultStack = channelModel.get(Attribute.STACK.getName()).asString();
                        operation.get(Attribute.STACK.getName()).set(defaultStack);
                    }
                }
            }
            this.handler.execute(context, operation);
        }
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        Collection<ResourceServiceInstaller> installers = new LinkedList<>();

        String clusterName = ChannelResourceDefinition.Attribute.CLUSTER.resolveModelAttribute(context, model).asString(name);
        String stackName = ChannelResourceDefinition.Attribute.STACK.resolveModelAttribute(context, model).asString();
        boolean statisticsEnabled = ChannelResourceDefinition.Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        ServiceDependency<ChannelFactory> channelFactory = ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, stackName);
        ChannelServiceConfiguration configuration = new ChannelServiceConfiguration() {
            @Override
            public boolean isStatisticsEnabled() {
                return statisticsEnabled;
            }

            @Override
            public org.wildfly.clustering.jgroups.ChannelFactory getChannelFactory() {
                return channelFactory.get();
            }

            @Override
            public String getClusterName() {
                return clusterName;
            }

            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                channelFactory.accept(builder);
            }
        };

        installers.add(new ChannelServiceConfigurator(CHANNEL_CAPABILITY, configuration).configure(context, model));
        installers.add(this.registry.capture(ServiceDependency.on(CHANNEL, name)));

        installers.add(new ForkChannelFactoryServiceConfigurator(CHANNEL_FACTORY_CAPABILITY, UnaryOperator.identity()).configure(context, model));
        installers.add(new ModuleServiceConfigurator(CHANNEL_MODULE_CAPABILITY, Attribute.MODULE.getDefinition()).configure(context, model));
        installers.add(CapabilityServiceInstaller.builder(CHANNEL_SOURCE_CAPABILITY, ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, stackName)).build());
        installers.add(CapabilityServiceInstaller.builder(CHANNEL_CLUSTER_CAPABILITY, clusterName).build());

        installers.add(new BinderServiceInstaller(JGroupsBindingFactory.createChannelBinding(name), context.getCapabilityServiceName(CHANNEL, name)));
        installers.add(new BinderServiceInstaller(JGroupsBindingFactory.createChannelFactoryBinding(name), context.getCapabilityServiceName(ChannelFactory.SERVICE_DESCRIPTOR, name)));

        new ProvidedUnaryServiceInstallerProvider<>(ChannelServiceInstallerProvider.class, ChannelServiceInstallerProvider.class.getClassLoader()).apply(context.getCapabilityServiceSupport(), name).forEach(installers::add);

        return ResourceServiceInstaller.combine(installers);
    }
}
