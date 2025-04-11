/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.management.MBeanServer;

import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.controller.descriptions.SimpleResourceDescriptionResolver;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.Attribute;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.protocols.FORK;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.JGroupsServiceDescriptor;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Abstract resource definition registrar for channel and fork resources.
 * @author Paul Ferraro
 * @param <C> the cache configuration type
 */
public abstract class AbstractChannelResourceDefinitionRegistrar<C extends ChannelConfiguration> implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator, ResourceOperationRuntimeHandler, UnaryOperator<ResourceDescriptor.Builder> {

    private static final RuntimeCapability<Void> CHANNEL = RuntimeCapability.Builder.of(JGroupsServiceDescriptor.CHANNEL).setAllowMultipleRegistrations(true).build();
    private static final RuntimeCapability<Void> CHANNEL_FACTORY = RuntimeCapability.Builder.of(ForkChannelFactory.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    interface Configurator<C> extends ResourceServiceConfigurator {

        ResourceRegistration getResourceRegistration();

        RuntimeCapability<Void> getCapability();

        ResourceModelResolver<ServiceDependency<ForkChannelFactoryConfiguration>> getForkChannelFactoryConfigurationResolver();

        ResourceModelResolver<ServiceDependency<C>> getChannelConfigurationResolver();

        ResourceModelResolver<PathAddress> getStackAddressResolver();

        @Override
        default ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
            String name = context.getCurrentAddressValue();
            ServiceDependency<ForkChannelFactoryConfiguration> configuration = this.getForkChannelFactoryConfigurationResolver().resolve(context, model);
            Consumer<ForkChannelFactoryConfiguration> stop = new Consumer<>() {
                @Override
                public void accept(ForkChannelFactoryConfiguration configuration) {
                    ProtocolStack stack = configuration.getChannel().getProtocolStack();
                    FORK fork = (FORK) stack.findProtocol(FORK.class);
                    fork.remove(name);
                }
            };
            return CapabilityServiceInstaller.builder(CHANNEL_FACTORY, org.jboss.as.clustering.jgroups.ForkChannelFactory::new, configuration)
                    .requires(List.of(configuration))
                    .blocking()
                    .onStop(stop)
                    .asPassive()
                    .build();
        }

        default UnaryOperator<OperationStepHandler> getAddOperationTransformation() {
            return UnaryOperator.identity();
        }
    }

    private final Configurator<C> configurator;
    private final ServiceValueExecutorRegistry<JChannel> channelRegistry;

    AbstractChannelResourceDefinitionRegistrar(Configurator<C> configurator, ServiceValueExecutorRegistry<JChannel> channelRegistry) {
        this.configurator = configurator;
        this.channelRegistry = channelRegistry;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        UnaryOperator<OperationStepHandler> addOperationTransformer = this.configurator.getAddOperationTransformation();
        return builder.addCapabilities(List.of(this.configurator.getCapability(), CHANNEL, CHANNEL_FACTORY))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(ResourceServiceConfigurator.combine(this.configurator, this)))
                .withOperationTransformation(ModelDescriptionConstants.ADD, new UnaryOperator<OperationStepHandler>() {
                    @Override
                    public OperationStepHandler apply(OperationStepHandler handler) {
                        return new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                addOperationTransformer.apply(handler).execute(context, operation);

                                // Register runtime resources
                                if (context.isDefaultRequiresRuntime()) {
                                    Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

                                    context.addStep(new OperationStepHandler() {
                                        @Override
                                        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                            AbstractChannelResourceDefinitionRegistrar.this.addRuntime(context, resource);
                                        }
                                    }, OperationContext.Stage.MODEL);
                                }
                            }
                        };
                    }
                })
                .withOperationTransformation(ModelDescriptionConstants.REMOVE, new UnaryOperator<OperationStepHandler>() {
                    @Override
                    public OperationStepHandler apply(OperationStepHandler handler) {
                        return new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

                                handler.execute(context, operation);

                                // Unregister runtime resources
                                if (context.isDefaultRequiresRuntime()) {
                                    AbstractChannelResourceDefinitionRegistrar.this.removeRuntime(context, resource);
                                }
                            }
                        };
                    }
                });
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(this.configurator.getResourceRegistration().getPathElement());
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();

        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(this.configurator.getResourceRegistration(), resolver).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    private interface MBeanRegistration {
        void accept(JChannel channel, MBeanServer server, String name) throws Exception;
    }

    private static class MBeanRegistrationTask implements Consumer<JChannel> {
        private final Supplier<MBeanServer> server;
        private final MBeanRegistration registration;
        private final String name;

        MBeanRegistrationTask(Supplier<MBeanServer> server, MBeanRegistration registration, String name) {
            this.server = server;
            this.registration = registration;
            this.name = name;
        }

        @Override
        public void accept(JChannel channel) {
            MBeanServer server = this.server.get();
            if (server != null) {
                try {
                    this.registration.accept(channel, server, this.name);
                } catch (Exception e) {
                    JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        Collection<ResourceServiceInstaller> installers = new ArrayList<>(4);

        // Create installer for service providing the channel configuration
        installers.add(CapabilityServiceInstaller.builder(this.configurator.getCapability(), this.configurator.getChannelConfigurationResolver().resolve(context, model)).build());

        // Create installer for service providing a connected JChannel
        ServiceDependency<ChannelConfiguration> channelConfiguration = ServiceDependency.on(ChannelConfiguration.SERVICE_DESCRIPTOR, name);
        ServiceDependency<MBeanServer> server = new MBeanServerResolver(CHANNEL).resolve(context, model);
        Supplier<JChannel> factory = new Supplier<>() {
            @Override
            public JChannel get() {
                ChannelConfiguration configuration = channelConfiguration.get();
                try {
                    JChannel channel = configuration.getChannelFactory().createChannel(name);
                    if (JGroupsLogger.ROOT_LOGGER.isTraceEnabled())  {
                        JGroupsLogger.ROOT_LOGGER.tracef("JGroups channel %s created with configuration:%n %s", name, channel.getProtocolStack().printProtocolSpec(true));
                    }
                    return channel.stats(configuration.isStatisticsEnabled());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        ServiceValueExecutorRegistry<JChannel> registry = this.channelRegistry;
        ServiceDependency<JChannel> registryKey = ServiceDependency.on(JGroupsServiceDescriptor.CHANNEL, name);
        Consumer<JChannel> connect = new Consumer<>() {
            @Override
            public void accept(JChannel disconnectedChannel) {
                TP transport = disconnectedChannel.getProtocolStack().getTransport();
                ChannelConfiguration configuration = channelConfiguration.get();
                JGroupsLogger.ROOT_LOGGER.connecting(name, disconnectedChannel.getName(), configuration.getClusterName(), new InetSocketAddress(transport.getBindAddress(), transport.getBindPort()));
                try {
                    registry.add(registryKey).accept(disconnectedChannel.connect(configuration.getClusterName()));
                } catch (Exception e) {
                    disconnectedChannel.close();
                    throw new IllegalStateException(e);
                }
                JGroupsLogger.ROOT_LOGGER.connected(name, disconnectedChannel.getName(), configuration.getClusterName(), disconnectedChannel.getView());
            }
        };
        Consumer<JChannel> disconnect = new Consumer<>() {
            @Override
            public void accept(JChannel connectedChannel) {
                registry.remove(registryKey);
                ChannelConfiguration configuration = channelConfiguration.get();
                JGroupsLogger.ROOT_LOGGER.disconnecting(name, connectedChannel.getName(), configuration.getClusterName(), connectedChannel.getView());
                connectedChannel.disconnect();
                JGroupsLogger.ROOT_LOGGER.disconnected(name, connectedChannel.getName(), configuration.getClusterName());
            }
        };
        installers.add(CapabilityServiceInstaller.builder(CHANNEL, factory).blocking()
                .requires(List.of(channelConfiguration, server))
                .onStart(new MBeanRegistrationTask(server, JmxConfigurator::registerChannel, name).andThen(connect))
                .onStop(disconnect.andThen(new MBeanRegistrationTask(server, JmxConfigurator::unregisterChannel, name)).andThen(Functions.closingConsumer()))
                .build());

        // Create installers for jndi bindings
        installers.add(new BinderServiceInstaller(JGroupsBindingFactory.CHANNEL.apply(name), context.getCapabilityServiceName(JGroupsServiceDescriptor.CHANNEL, name)));
        installers.add(new BinderServiceInstaller(JGroupsBindingFactory.CHANNEL_FACTORY.apply(name), context.getCapabilityServiceName(ForkChannelFactory.SERVICE_DESCRIPTOR, name)));

        return ResourceServiceInstaller.combine(installers);
    }

    /*
     * Registers override channel model containing runtime resources per protocol.
     */
    @Override
    public void addRuntime(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        PathAddress stackAddress = this.configurator.getStackAddressResolver().resolve(context, resource);
        Resource stackResource = address.equals(stackAddress) ? resource : context.readResourceFromRoot(stackAddress);
        List<PathElement> protocolTypes = List.of(StackResourceDefinitionRegistrar.Component.TRANSPORT.getPathElement(), StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement(), StackResourceDefinitionRegistrar.Component.RELAY.getPathElement());
        if (protocolTypes.stream().anyMatch(protocolType -> stackResource.hasChildren(protocolType.getKey()))) {
            ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
            if (resource != stackResource) {
                // This is a channel resource, we need to create an override for the stack-specific model
                OverrideDescriptionProvider provider = new OverrideDescriptionProvider() {
                    @Override
                    public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                        return Collections.emptyMap();
                    }

                    @Override
                    public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                        String description = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.getChildTypeDescription(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement().getKey(), locale, JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.getResourceBundle(locale));
                        ModelNode result = new ModelNode();
                        result.get(ModelDescriptionConstants.DESCRIPTION).set(description);
                        return Collections.singletonMap(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement().getKey(), result);
                    }
                };
                registration = registration.registerOverrideModel(context.getCurrentAddressValue(), provider);
            }

            for (PathElement path : protocolTypes) {
                for (Resource.ResourceEntry protocolResource : stackResource.getChildren(path.getKey())) {
                    String protocolName = protocolResource.getName();
                    Class<? extends Protocol> protocolClass = findProtocolClass(context, protocolName, protocolResource.getModel());
                    this.register(registration, protocolName, protocolClass);
                    resource.registerChild(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(protocolName), PlaceholderResource.INSTANCE);
                }
            }
        }
    }

    /*
     * Unregisters override channel model
     */
    @Override
    public void removeRuntime(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress stackAddress = this.configurator.getStackAddressResolver().resolve(context, model);
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        if (address.equals(stackAddress)) {
            // This is a fork channel resource, unregister runtime attributes of protocols
            for (PathElement protocolPath : registration.getChildAddresses(stackAddress)) {
                ManagementResourceRegistration protocolRegistration = registration.getSubModel(PathAddress.pathAddress(protocolPath));
                for (Map.Entry<String, AttributeAccess> entry : protocolRegistration.getAttributes(PathAddress.EMPTY_ADDRESS).entrySet()) {
                    if (entry.getValue().getStorageType() == AttributeAccess.Storage.RUNTIME) {
                        protocolRegistration.unregisterAttribute(entry.getKey());
                    }
                }
            }
        } else {
            // This is a channel resource, unregister entire override model
            context.getResourceRegistrationForUpdate().unregisterOverrideModel(context.getCurrentAddressValue());
        }
    }

    // Register a sub-model or override model for the specified protocol
    private ManagementResourceRegistration register(ManagementResourceRegistration parent, String protocolName, Class<? extends Protocol> protocolClass) {
        Map<String, ProtocolMetricsHandler.Attribute> attributes = ProtocolMetricsHandler.findProtocolAttributes(protocolClass);

        PathElement path = StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement(protocolName);
        ManagementResourceRegistration registration = parent.getSubModel(PathAddress.pathAddress(path));
        if (registration == null) {
            // Parent is a channel resource, create sub-model for this protocol
            SimpleResourceDescriptionResolver resolver = new SimpleResourceDescriptionResolver(protocolName, protocolClass.getSimpleName());
            for (Map.Entry<String, ProtocolMetricsHandler.Attribute> entry: attributes.entrySet()) {
                resolver.addDescription(entry.getKey(), entry.getValue().getDescription());
            }
            ResourceDefinition definition = ResourceDefinition.builder(ResourceRegistration.of(path), resolver).asRuntime().build();
            registration = parent.registerSubModel(definition);
        } else if (registration.getPathAddress().getLastElement().isWildcard()) {
            // This a generic protocol resource of a fork channel resource, create a model override for this protocol
            OverrideDescriptionProvider provider = new OverrideDescriptionProvider() {
                @Override
                public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                    Map<String, ModelNode> result = new HashMap<>();
                    for (Attribute attribute : attributes.values()) {
                        ModelNode value = new ModelNode();
                        value.get(ModelDescriptionConstants.DESCRIPTION).set(attribute.getDescription());
                        result.put(attribute.getName(), value);
                    }
                    return result;
                }

                @Override
                public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                    return Map.of();
                }
            };
            registration = registration.registerOverrideModel(protocolName, provider);
        }

        ProtocolMetricsHandler handler = new ProtocolMetricsHandler(this.channelRegistry);
        for (Map.Entry<String, ProtocolMetricsHandler.Attribute> entry: attributes.entrySet()) {
            String name = entry.getKey();
            ProtocolMetricsHandler.Attribute attribute = entry.getValue();
            ProtocolMetricsHandler.FieldType type = ProtocolMetricsHandler.FieldType.valueOf(attribute.getType());
            // Register read-only runtime attribute per managed protocol attribute
            registration.registerReadOnlyAttribute(new SimpleAttributeDefinitionBuilder(name, type.getModelType(), true).setStorageRuntime().build(), handler);
        }

        return registration;
    }

    static Class<? extends Protocol> findProtocolClass(OperationContext context, String protocolName, ModelNode protocolModel) throws OperationFailedException {
        String moduleName = ProtocolChildResourceDefinitionRegistrar.MODULE.resolveModelAttribute(context, protocolModel).asString();
        String className = protocolName;
        if (moduleName.equals(ProtocolChildResourceDefinitionRegistrar.MODULE.getDefaultValue().asString()) && !protocolName.startsWith(Global.PREFIX)) {
            className = Global.PREFIX + protocolName;
        }
        try {
            return Module.getContextModuleLoader().loadModule(moduleName).getClassLoader().loadClass(className).asSubclass(Protocol.class);
        } catch (ClassNotFoundException | ModuleLoadException e) {
            throw JGroupsLogger.ROOT_LOGGER.unableToLoadProtocolClass(className);
        }
    }
}
