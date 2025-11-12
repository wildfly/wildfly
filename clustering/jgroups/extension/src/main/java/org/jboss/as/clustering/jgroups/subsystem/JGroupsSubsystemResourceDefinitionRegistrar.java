/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.jgroups.subsystem.ProtocolChildResourceDefinitionRegistrar.findProtocolClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import org.jboss.as.clustering.jgroups.ProtocolPropertiesRepository;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.jgroups.Version;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelConfiguration;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsServiceDescriptor;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.DefaultChannelServiceInstallerProvider;
import org.wildfly.clustering.server.service.ProvidedUnaryServiceInstallerProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers the resource definition for the JGroups subsystem.
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 */
public class JGroupsSubsystemResourceDefinitionRegistrar implements SubsystemResourceDefinitionRegistrar, BiPredicate<OperationContext, Resource>, ResourceServiceConfigurator {

    public static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of("jgroups");
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(REGISTRATION.getName(), JGroupsSubsystemResourceDefinitionRegistrar.class);

    static final RuntimeCapability<Void> DEFAULT_CHANNEL_CAPABILITY = RuntimeCapability.Builder.of(JGroupsServiceDescriptor.DEFAULT_CHANNEL).build();

    static final CapabilityReferenceAttributeDefinition<JChannel> DEFAULT_CHANNEL = new CapabilityReferenceAttributeDefinition.Builder<>("default-channel", CapabilityReference.builder(DEFAULT_CHANNEL_CAPABILITY, JGroupsServiceDescriptor.CHANNEL).build())
            .setXmlName(ModelDescriptionConstants.DEFAULT)
            .setAttributeGroup("channels")
            .setRequired(false)
            .build();

    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_CONFIGURATION_CAPABILITY = RuntimeCapability.Builder.of(ChannelConfiguration.DEFAULT_SERVICE_DESCRIPTOR).build();
    private static final RuntimeCapability<Void> DEFAULT_CHANNEL_FACTORY_CAPABILITY = RuntimeCapability.Builder.of(ForkChannelFactory.DEFAULT_SERVICE_DESCRIPTOR).build();

    private static final String PROTOCOL_DEFAULTS_RESOURCE = "jgroups-defaults.xml";

    private static ProtocolStackConfigurator loadDefaultProtocolStackConfigurator() throws IllegalStateException {
        URL url = find(PROTOCOL_DEFAULTS_RESOURCE, JGroupsExtension.class.getClassLoader());
        ROOT_LOGGER.debugf("Loading JGroups protocol defaults from %s", url.toString());
        try (InputStream input = url.openStream()) {
            return XmlConfigurator.getInstance(input);
        } catch (IOException e) {
            throw new IllegalArgumentException(JGroupsLogger.ROOT_LOGGER.parserFailure(url));
        }
    }

    private static URL find(String resource, ClassLoader... loaders) {
        for (ClassLoader loader: loaders) {
            if (loader != null) {
                URL url = loader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        }
        throw new IllegalArgumentException(JGroupsLogger.ROOT_LOGGER.notFound(resource));
    }

    @Override
    public boolean test(OperationContext context, Resource resource) {
        return resource.getModel().hasDefined(DEFAULT_CHANNEL.getName());
    }

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        RuntimeCapability<Void> defaultCommandDispatcherFactory = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> defaultLegacyCommandDispatcherFactory = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_COMMAND_DISPATCHER_FACTORY).setAllowMultipleRegistrations(true).build();

        RuntimeCapability<Void> defaultGroup = RuntimeCapability.Builder.of(ClusteringServiceDescriptor.DEFAULT_GROUP).setAllowMultipleRegistrations(true).build();
        @SuppressWarnings("deprecation")
        RuntimeCapability<Void> defaultLegacyGroup = RuntimeCapability.Builder.of(org.wildfly.clustering.server.service.LegacyClusteringServiceDescriptor.DEFAULT_GROUP).setAllowMultipleRegistrations(true).build();

        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .addAttributes(List.of(DEFAULT_CHANNEL))
                .addCapabilities(List.of(DEFAULT_CHANNEL_CAPABILITY, DEFAULT_CHANNEL_CONFIGURATION_CAPABILITY, DEFAULT_CHANNEL_FACTORY_CAPABILITY, defaultCommandDispatcherFactory, defaultLegacyCommandDispatcherFactory, defaultGroup, defaultLegacyGroup), this)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(this))
                .build();

        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, RESOLVER).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        new ChannelResourceDefinitionRegistrar().register(registration, context);
        new StackResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        JGroupsLogger.ROOT_LOGGER.activatingSubsystem(Version.printVersion());

        PathAddress address = context.getCurrentAddress();

        // Handle case where JGroups subsystem is added to a running server
        // In this case, the Infinispan subsystem may have already registered default group capabilities
        if (context.getProcessType().isServer() && !context.isBooting()) {
            if (context.readResourceFromRoot(address.getParent(), false).hasChild(SubsystemResourceRegistration.of("infinispan").getPathElement())) {
                // Following restart, default group services will be installed by this handler, rather than the infinispan subsystem handler
                context.addStep((ctx, operation) -> {
                    ctx.reloadRequired();
                    ctx.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                }, OperationContext.Stage.RUNTIME);
                return ResourceServiceInstaller.combine();
            }
        }

        List<ResourceServiceInstaller> installers = new LinkedList<>();

        Supplier<ProtocolPropertiesRepository> factory = new Supplier<>() {
            @Override
            public ProtocolPropertiesRepository get() {
                ProtocolStackConfigurator configurator = loadDefaultProtocolStackConfigurator();
                Map<Class<? extends Protocol>, Map<String, String>> protocolProperties = new IdentityHashMap<>();
                try {
                    for (ProtocolConfiguration config : configurator.getProtocolStack()) {
                        // n.b. these defaults are only applicable to protocols loadable from the jgroups main module, so there is no module=".." attribute to account for
                        Class<? extends Protocol> protocolClass = findProtocolClass(Protocol.class.getClassLoader(), config.getProtocolName());
                        protocolProperties.put(protocolClass, Collections.unmodifiableMap(config.getProperties()));
                    }
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
                return new ProtocolPropertiesRepository() {
                    @Override
                    public Map<String, String> getProperties(Class<? extends Protocol> protocolClass) {
                        return protocolProperties.getOrDefault(protocolClass, Map.of());
                    }
                };
            }
        };
        installers.add(ServiceInstaller.builder(factory).provides(ServiceNameFactory.resolveServiceName(ProtocolPropertiesRepository.SERVICE_DESCRIPTOR))
                .blocking()
                .build());

        String defaultChannel = DEFAULT_CHANNEL.resolveModelAttribute(context, model).asStringOrNull();
        if (defaultChannel != null) {
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_CAPABILITY, ServiceDependency.on(JGroupsServiceDescriptor.CHANNEL, defaultChannel)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_CONFIGURATION_CAPABILITY, ServiceDependency.on(ChannelConfiguration.SERVICE_DESCRIPTOR, defaultChannel)).build());
            installers.add(CapabilityServiceInstaller.builder(DEFAULT_CHANNEL_FACTORY_CAPABILITY, ServiceDependency.on(ForkChannelFactory.SERVICE_DESCRIPTOR, defaultChannel)).build());

            if (!defaultChannel.equals(ModelDescriptionConstants.DEFAULT)) {
                installers.add(new BinderServiceInstaller(JGroupsBindingFactory.CHANNEL.apply(ModelDescriptionConstants.DEFAULT), context.getCapabilityServiceName(JGroupsServiceDescriptor.CHANNEL, defaultChannel)));
                installers.add(new BinderServiceInstaller(JGroupsBindingFactory.CHANNEL_FACTORY.apply(ModelDescriptionConstants.DEFAULT), context.getCapabilityServiceName(ForkChannelFactory.SERVICE_DESCRIPTOR, defaultChannel)));
            }

            new ProvidedUnaryServiceInstallerProvider<>(DefaultChannelServiceInstallerProvider.class, DefaultChannelServiceInstallerProvider.class.getClassLoader()).apply(defaultChannel).forEach(installers::add);
        }
        return ResourceServiceInstaller.combine(installers);
    }
}
