/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.jgroups.ClassLoaderThreadFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.TP;
import org.jgroups.stack.DiagnosticsHandler;
import org.jgroups.util.DefaultThreadFactory;
import org.jgroups.util.ThreadPool;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a JGroups transport.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class AbstractTransportResourceDefinitionRegistrar<T extends TP> extends ProtocolConfigurationResourceDefinitionRegistrar<T, TransportConfiguration<T>> {

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(TransportConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    enum SocketBindingAttribute implements AttributeDefinitionProvider, ResourceModelResolver<ServiceDependency<SocketBinding>> {
        SERVER(ModelDescriptionConstants.SOCKET_BINDING, true),
        DIAGNOSTICS("diagnostics-socket-binding", false),
        ;
        private final CapabilityReferenceAttributeDefinition<SocketBinding> attribute;

        SocketBindingAttribute(String name, boolean required) {
            this.attribute = new CapabilityReferenceAttributeDefinition.Builder<>(name, CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
                    .setRequired(required)
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                    .build();
        }

        @Override
        public ServiceDependency<SocketBinding> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            return this.attribute.resolve(context, model);
        }

        @Override
        public AttributeDefinition get() {
            return this.attribute;
        }
    }

    enum TopologyAttribute implements AttributeDefinitionProvider, ResourceModelResolver<String> {
        SITE("site"),
        RACK("rack"),
        MACHINE("machine"),
        ;
        private final AttributeDefinition definition;

        TopologyAttribute(String name) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, ModelType.STRING)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }

        @Override
        public String resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            return this.resolveModelAttribute(context, model).asStringOrNull();
        }
    }

    interface Configurator extends ProtocolConfigurationResourceDefinitionRegistrar.Configurator {
        @Override
        default RuntimeCapability<Void> getCapability() {
            return CAPABILITY;
        }

        @Override
        default ResourceDescriptionResolver getResourceDescriptionResolver() {
            PathElement path = this.getResourceRegistration().getPathElement();
            return path.isWildcard() ? JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement()) : JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, StackResourceDefinitionRegistrar.Component.TRANSPORT.getPathElement(), StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement());
        }
    }

    AbstractTransportResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .provideAttributes(EnumSet.allOf(SocketBindingAttribute.class))
                .provideAttributes(EnumSet.allOf(TopologyAttribute.class))
                .requireChildResources(EnumSet.allOf(ThreadPoolResourceDefinitionRegistrar.class))
                ;
    }

    @Override
    public ServiceDependency<TransportConfiguration<T>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<ProtocolConfiguration<T>> factory = this.resolver.resolve(context, model);
        ServiceDependency<ThreadPoolConfiguration> threadPool = ServiceDependency.on(ThreadPoolResourceDefinitionRegistrar.DEFAULT, context.getCurrentAddress().getParent().getLastElement().getValue());
        ServiceDependency<SocketBinding> serverSocketBinding = SocketBindingAttribute.SERVER.resolve(context, model);
        ServiceDependency<SocketBinding> diagnosticsSocketBinding = SocketBindingAttribute.DIAGNOSTICS.resolve(context, model);

        String machine = TopologyAttribute.MACHINE.resolveModelAttribute(context, model).asStringOrNull();
        String rack = TopologyAttribute.RACK.resolveModelAttribute(context, model).asStringOrNull();
        String site = TopologyAttribute.SITE.resolveModelAttribute(context, model).asStringOrNull();
        TransportConfiguration.Topology topology = (site != null || rack != null || machine != null) ? new TransportConfiguration.Topology() {
            @Override
            public String getMachine() {
                return machine;
            }

            @Override
            public String getRack() {
                return rack;
            }

            @Override
            public String getSite() {
                return site;
            }
        } : null;
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                factory.accept(builder);
                threadPool.accept(builder);
                serverSocketBinding.accept(builder);
                diagnosticsSocketBinding.accept(builder);
            }

            @Override
            public TransportConfiguration<T> get() {
                return new TransportProtocolConfigurationDecorator<>(factory.get()) {
                    @Override
                    public T createProtocol(ChannelFactoryConfiguration configuration) {
                        T protocol = super.createProtocol(configuration);
                        SocketBinding binding = this.getSocketBinding();
                        InetSocketAddress socketAddress = binding.getSocketAddress();
                        protocol.setBindAddress(socketAddress.getAddress());
                        protocol.setBindPort(socketAddress.getPort());

                        List<ClientMapping> clientMappings = binding.getClientMappings();
                        if (!clientMappings.isEmpty()) {
                            // JGroups cannot select a client mapping based on the source address, so just use the first one
                            ClientMapping mapping = clientMappings.get(0);
                            try {
                                protocol.setExternalAddr(InetAddress.getByName(mapping.getDestinationAddress()));
                                protocol.setExternalPort(mapping.getDestinationPort());
                            } catch (UnknownHostException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }

                        ThreadPoolConfiguration threadPoolConfiguration = threadPool.get();
                        ThreadPool threadPool = protocol.getThreadPool();
                        threadPool.setMinThreads(threadPoolConfiguration.getMinThreads());
                        threadPool.setMaxThreads(threadPoolConfiguration.getMaxThreads());
                        threadPool.setKeepAliveTime(threadPoolConfiguration.getKeepAlive().toMillis());
                        // Let JGroups retransmit if pool is full
                        threadPool.setRejectionPolicy("discard");

                        // JGroups propagates this to the ThreadPool
                        protocol.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("jgroups", false, true).useVirtualThreads(protocol.useVirtualThreads()), JChannelFactory.class.getClassLoader()));

                        SocketBinding diagnosticsBinding = diagnosticsSocketBinding.get();
                        if (diagnosticsBinding != null) {
                            DiagnosticsHandler handler = new DiagnosticsHandler(protocol.getLog(), protocol.getSocketFactory(), protocol.getThreadFactory());
                            InetSocketAddress address = diagnosticsBinding.getSocketAddress();
                            handler.setBindAddress(address.getAddress());
                            if (diagnosticsBinding.getMulticastAddress() != null) {
                                handler.setMcastAddress(diagnosticsBinding.getMulticastAddress());
                                handler.setPort(diagnosticsBinding.getMulticastPort());
                            } else {
                                handler.setPort(diagnosticsBinding.getPort());
                            }
                            try {
                                protocol.setDiagnosticsHandler(handler);
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                        return protocol;
                    }

                    @Override
                    public Topology getTopology() {
                        return topology;
                    }

                    @Override
                    public SocketBinding getSocketBinding() {
                        return serverSocketBinding.get();
                    }
                };
            }
        };
    }

    abstract static class TransportProtocolConfigurationDecorator<T extends TP> extends ProtocolConfigurationDecorator<T> implements TransportConfiguration<T> {

        TransportProtocolConfigurationDecorator(ProtocolConfiguration<T> configuration) {
            super(configuration);
        }
    }

    abstract static class TransportConfigurationDecorator<T extends TP> extends TransportProtocolConfigurationDecorator<T> {
        private final TransportConfiguration<T> configuration;

        TransportConfigurationDecorator(TransportConfiguration<T> configuration) {
            super(configuration);
            this.configuration = configuration;
        }

        @Override
        public Topology getTopology() {
            return this.configuration.getTopology();
        }

        @Override
        public SocketBinding getSocketBinding() {
            return this.configuration.getSocketBinding();
        }
    }}
