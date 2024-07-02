/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.LinkedList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.DefaultSubsystemDescribeHandler;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SubsystemRegistration;
import org.jboss.as.clustering.controller.SubsystemResourceDefinition;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.jgroups.JChannel;
import org.jgroups.Version;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedUnaryServiceInstallerProvider;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * The root resource of the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinition extends SubsystemResourceDefinition implements ResourceServiceConfigurator {

    public static final PathElement PATH = pathElement(JGroupsExtension.SUBSYSTEM_NAME);

    static final NullaryServiceDescriptor<JChannel> DEFAULT_CHANNEL = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel", JChannel.class);
    static final NullaryServiceDescriptor<Module> DEFAULT_CHANNEL_MODULE = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel-module", Module.class);
    static final NullaryServiceDescriptor<ChannelFactory> DEFAULT_CHANNEL_SOURCE = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel-source", ChannelFactory.class);
    static final NullaryServiceDescriptor<String> DEFAULT_CHANNEL_CLUSTER = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.default-channel-cluster", String.class);

    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_FACTORY_CAPABILITY = RuntimeCapability.Builder.of(ChannelFactory.DEFAULT_SERVICE_DESCRIPTOR).build();
    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_CAPABILITY = RuntimeCapability.Builder.of(DEFAULT_CHANNEL).build();
    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_MODULE_CAPABILITY = RuntimeCapability.Builder.of(DEFAULT_CHANNEL_MODULE).build();
    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_SOURCE_CAPABILITY = RuntimeCapability.Builder.of(DEFAULT_CHANNEL_SOURCE).build();
    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_CLUSTER_CAPABILITY = RuntimeCapability.Builder.of(DEFAULT_CHANNEL_CLUSTER).build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        DEFAULT_CHANNEL("default-channel", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReferenceRecorder.builder(DEFAULT_CHANNEL_FACTORY_CAPABILITY, ChannelFactory.SERVICE_DESCRIPTOR).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setRequired(false)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setXmlName(XMLAttribute.DEFAULT.getLocalName())
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    JGroupsSubsystemResourceDefinition() {
        super(PATH, JGroupsExtension.SUBSYSTEM_RESOLVER);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(SubsystemRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubsystemModel(this);

        new DefaultSubsystemDescribeHandler().register(registration);

        RuntimeCapability<Void> defaultCommandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> defaultLegacyCommandDispatcherFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();

        RuntimeCapability<Void> defaultGroup = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_GROUP).setAllowMultipleRegistrations(true).build();
        RuntimeCapability<Void> defaultLegacyGroup = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_GROUP).setAllowMultipleRegistrations(true).build();

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addCapabilities(model -> model.hasDefined(Attribute.DEFAULT_CHANNEL.getName()), List.of(DEFAULT_CHANNEL_CAPABILITY, DEFAULT_CHANNEL_FACTORY_CAPABILITY, DEFAULT_CHANNEL_MODULE_CAPABILITY, DEFAULT_CHANNEL_SOURCE_CAPABILITY, DEFAULT_CHANNEL_CLUSTER_CAPABILITY, defaultCommandDispatcherFactory, defaultLegacyCommandDispatcherFactory, defaultGroup, defaultLegacyGroup))
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);

        new ChannelResourceDefinition().register(registration);
        new StackResourceDefinition().register(registration);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        JGroupsLogger.ROOT_LOGGER.activatingSubsystem(Version.printVersion());

        PathAddress address = context.getCurrentAddress();

        // Handle case where JGroups subsystem is added to a running server
        // In this case, the Infinispan subsystem may have already registered default group capabilities
        if (context.getProcessType().isServer() && !context.isBooting()) {
            if (context.readResourceFromRoot(address.getParent(),false).hasChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "infinispan"))) {
                // Following restart, default group services will be installed by this handler, rather than the infinispan subsystem handler
                context.addStep((ctx, operation) -> {
                    ctx.reloadRequired();
                    ctx.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }, OperationContext.Stage.RUNTIME);
                return ResourceServiceInstaller.combine();
            }
        }

        List<ResourceServiceInstaller> installers = new LinkedList<>();
        installers.add(new ProtocolDefaultsServiceInstaller());

        String defaultChannel = Attribute.DEFAULT_CHANNEL.resolveModelAttribute(context, model).asStringOrNull();
        if (defaultChannel != null) {
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_CAPABILITY, ServiceDependency.on(ChannelResourceDefinition.CHANNEL, defaultChannel)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_FACTORY_CAPABILITY, ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, defaultChannel)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_MODULE_CAPABILITY, ServiceDependency.on(ChannelResourceDefinition.CHANNEL_MODULE, defaultChannel)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_SOURCE_CAPABILITY, ServiceDependency.on(ChannelResourceDefinition.CHANNEL_SOURCE, defaultChannel)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_CLUSTER_CAPABILITY, ServiceDependency.on(ChannelResourceDefinition.CHANNEL_CLUSTER, defaultChannel)).build());

            if (!defaultChannel.equals(ModelDescriptionConstants.DEFAULT)) {
                installers.add(new BinderServiceInstaller(JGroupsBindingFactory.createChannelBinding(ModelDescriptionConstants.DEFAULT), JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL_CAPABILITY.getCapabilityServiceName()));
                installers.add(new BinderServiceInstaller(JGroupsBindingFactory.createChannelFactoryBinding(ModelDescriptionConstants.DEFAULT), JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL_FACTORY_CAPABILITY.getCapabilityServiceName()));
            }

            new ProvidedUnaryServiceInstallerProvider<>(DefaultChannelServiceInstallerProvider.class, DefaultChannelServiceInstallerProvider.class.getClassLoader()).apply(context.getCapabilityServiceSupport(), defaultChannel).forEach(installers::add);
        }
        return ResourceServiceInstaller.combine(installers);
    }
}
