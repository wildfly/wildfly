/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.StatisticsEnabledAttributeDefinition;
import org.wildfly.subsystem.resource.capability.ResourceCapabilityReference;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a JGroups protocol stack.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class StackResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {

    public enum Component implements ResourceRegistration {
        TRANSPORT("transport"),
        PROTOCOL("protocol"),
        RELAY("relay"),
        ;
        private final PathElement path;

        Component(String name) {
            this.path = PathElement.pathElement(name);
        }

        @Override
        public PathElement getPathElement() {
            return this.path;
        }

        public PathElement pathElement(String value) {
            return PathElement.pathElement(this.getPathElement().getKey(), value);
        }
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(ChannelFactory.SERVICE_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    private static final ResourceOperationRuntimeHandler BINDING_RUNTIME_HANDLER = ResourceOperationRuntimeHandler.configureService(new ResourceServiceConfigurator() {
        @Override
        public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
            return new BinderServiceInstaller(JGroupsBindingFactory.CHANNEL_FACTORY.apply(context.getCurrentAddressValue()), CAPABILITY.getCapabilityServiceName(context.getCurrentAddress()));
        }
    });

    private static final ResourceCapabilityReference<TransportConfiguration<TP>> TRANSPORT_REFERENCE = ResourceCapabilityReference.builder(CAPABILITY, TransportConfiguration.SERVICE_DESCRIPTOR).build();

    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(JGroupsResourceRegistration.STACK.getPathElement());
        ResourceOperationRuntimeHandler runtimeHandler = ResourceOperationRuntimeHandler.configureService(this);
        ResourceDescriptor descriptor = ResourceDescriptor.builder(resolver)
                .addAttributes(List.of(STATISTICS_ENABLED))
                .addCapability(CAPABILITY)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(runtimeHandler, BINDING_RUNTIME_HANDLER))
                .addResourceCapabilityReference(TRANSPORT_REFERENCE)
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(JGroupsResourceRegistration.STACK, resolver).build());

        ManagementResourceRegistrar.of(descriptor).register(registration);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new StackOperationHandler().register(registration);
        }

        new TransportResourceDefinitionRegistrar(runtimeHandler).register(registration, context);

        new ProtocolResourceDefinitionRegistrar(runtimeHandler).register(registration, context);

        new RelayResourceDefinitionRegistrar(runtimeHandler).register(registration, context);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ServiceDependency<TransportConfiguration<TP>> transport = TRANSPORT_REFERENCE.resolve(context, resource);
        ServiceDependency<RelayConfiguration> relay = resource.hasChildren(StackResourceDefinitionRegistrar.Component.RELAY.getPathElement().getKey()) ? ServiceDependency.on(RelayConfiguration.SERVICE_DESCRIPTOR, name) : ServiceDependency.empty();

        Set<String> protocolNames = resource.getChildrenNames(Component.PROTOCOL.getPathElement().getKey());
        List<ServiceDependency<ProtocolConfiguration<Protocol>>> protocols = new ArrayList<>(protocolNames.size());
        for (String protocolName : protocolNames) {
            protocols.add(ServiceDependency.on(ProtocolConfiguration.SERVICE_DESCRIPTOR, name, protocolName));
        }

        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        ServiceDependency<SocketBindingManager> socketBindingManager = ServiceDependency.on(SocketBindingManager.SERVICE_DESCRIPTOR);

        ChannelFactoryConfiguration configuration = new ChannelFactoryConfiguration() {
            @Override
            public boolean isStatisticsEnabled() {
                return statisticsEnabled;
            }

            @Override
            public TransportConfiguration<? extends TP> getTransport() {
                return transport.get();
            }

            @Override
            public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
                return protocols.stream().map(Supplier::get).collect(Collectors.toUnmodifiableList());
            }

            @Override
            public String getMemberName() {
                return environment.get().getNodeName();
            }

            @Override
            public Optional<RelayConfiguration> getRelay() {
                return Optional.ofNullable(relay.get());
            }

            @Override
            public SocketBindingManager getSocketBindingManager() {
                return socketBindingManager.get();
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, JChannelFactory::new, Functions.constantSupplier(configuration))
                .requires(List.of(transport, relay, environment, socketBindingManager))
                .requires(protocols)
                .blocking()
                .build();
    }
}
