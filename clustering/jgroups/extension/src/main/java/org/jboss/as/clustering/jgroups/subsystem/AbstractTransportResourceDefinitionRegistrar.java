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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.TP;
import org.jgroups.stack.DiagnosticsHandler;
import org.jgroups.util.DefaultThreadFactory;
import org.jgroups.util.ThreadPool;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a JGroups transport.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class AbstractTransportResourceDefinitionRegistrar<T extends TP> extends ProtocolConfigurationResourceDefinitionRegistrar<T, TransportConfiguration<T>> {

    interface TransportResourceDescriptorConfigurator<T extends TP> extends ProtocolConfigurationResourceDescriptorConfigurator<T, TransportConfiguration<T>> {
        @Override
        TransportResourceDescription getResourceDescription();

        @Override
        default RuntimeCapability<Void> getCapability() {
            return TransportResourceDescription.CAPABILITY;
        }

        @Override
        default ResourceDescriptionResolver getResourceDescriptionResolver() {
            PathElement path = this.getResourceDescription().getPathElement();
            return path.isWildcard() ? JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, ProtocolResourceDescription.INSTANCE.getPathElement()) : JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(path, TransportResourceDescription.INSTANCE.getPathElement(), ProtocolResourceDescription.INSTANCE.getPathElement());
        }

        @Override
        default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
            return ProtocolConfigurationResourceDescriptorConfigurator.super.apply(builder)
                    .provideAttributes(EnumSet.complementOf(EnumSet.of(TransportResourceDescription.SocketBindingAttribute.CLIENT)))
                    .provideAttributes(EnumSet.allOf(TransportResourceDescription.TopologyAttribute.class))
                    .requireChildResources(EnumSet.allOf(ThreadPoolResourceDefinitionRegistrar.class))
                    ;
        }

        @Override
        default ServiceDependency<TransportConfiguration<T>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
            ServiceDependency<ProtocolConfiguration<T>> factory = this.getProtocolConfigurationResolver().resolve(context, model);
            ServiceDependency<ThreadPoolConfiguration> threadPool = ServiceDependency.on(ThreadPoolResourceDefinitionRegistrar.DEFAULT, context.getCurrentAddress().getParent().getLastElement().getValue());
            ServiceDependency<SocketBinding> serverSocketBinding = TransportResourceDescription.SocketBindingAttribute.SERVER.resolve(context, model);
            ServiceDependency<SocketBinding> diagnosticsSocketBinding = TransportResourceDescription.SocketBindingAttribute.DIAGNOSTICS.resolve(context, model);

            String machine = TransportResourceDescription.TopologyAttribute.MACHINE.resolveModelAttribute(context, model).asStringOrNull();
            String rack = TransportResourceDescription.TopologyAttribute.RACK.resolveModelAttribute(context, model).asStringOrNull();
            String site = TransportResourceDescription.TopologyAttribute.SITE.resolveModelAttribute(context, model).asStringOrNull();
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
    }

    AbstractTransportResourceDefinitionRegistrar(TransportResourceDescriptorConfigurator<T> configurator) {
        super(configurator);
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
