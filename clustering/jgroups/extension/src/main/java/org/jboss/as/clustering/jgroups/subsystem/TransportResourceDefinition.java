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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.protocols.TP;
import org.jgroups.stack.DiagnosticsHandler;
import org.jgroups.util.DefaultThreadFactory;
import org.jgroups.util.ThreadPool;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for /subsystem=jgroups/stack=X/transport=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Paul Ferraro
 */
public class TransportResourceDefinition<T extends TP> extends AbstractProtocolResourceDefinition<T, TransportConfiguration<T>> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("transport", name);
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(TransportConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        SOCKET_BINDING("socket-binding", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build());
            }
        },
        DIAGNOSTICS_SOCKET_BINDING("diagnostics-socket-binding", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build());
            }
        },
        SITE("site", ModelType.STRING),
        RACK("rack", ModelType.STRING),
        MACHINE("machine", ModelType.STRING),
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

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor)
                    .addAttributes(Attribute.class)
                    .addRequiredChildren(ThreadPoolResourceDefinition.class)
                    ;
        }
    }

    TransportResourceDefinition() {
        this(new Parameters(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), UnaryOperator.identity());
    }

    TransportResourceDefinition(String name) {
        this(pathElement(name), UnaryOperator.identity());
    }

    TransportResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> resourceConfigurator) {
        this(new Parameters(path, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), resourceConfigurator);
    }

    private TransportResourceDefinition(Parameters parameters, UnaryOperator<ResourceDescriptor> resourceConfigurator) {
        super(parameters, CAPABILITY, new ResourceDescriptorConfigurator(resourceConfigurator), null);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        if (registration.getPathAddress().getLastElement().isWildcard()) {
            for (ThreadPoolResourceDefinition pool : EnumSet.allOf(ThreadPoolResourceDefinition.class)) {
                pool.register(registration);
            }
        }

        return registration;
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<T>, TransportConfiguration<T>>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<ThreadPoolConfiguration> threadPool = ServiceDependency.on(ThreadPoolResourceDefinition.DEFAULT, context.getCurrentAddress().getParent().getLastElement().getValue());
        ServiceDependency<SocketBinding> binding = ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, Attribute.SOCKET_BINDING.resolveModelAttribute(context, model).asString());
        String diagnosticsBindingName = Attribute.DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        ServiceDependency<SocketBinding> diagnostics = (diagnosticsBindingName != null) ? ServiceDependency.on(SocketBinding.SERVICE_DESCRIPTOR, diagnosticsBindingName) : ServiceDependency.of(null);

        ModelNode machine = Attribute.MACHINE.resolveModelAttribute(context, model);
        ModelNode rack = Attribute.RACK.resolveModelAttribute(context, model);
        ModelNode site = Attribute.SITE.resolveModelAttribute(context, model);
        TransportConfiguration.Topology topology = (site.isDefined() || rack.isDefined() || machine.isDefined()) ? new TransportConfiguration.Topology() {
            @Override
            public String getMachine() {
                return machine.asStringOrNull();
            }

            @Override
            public String getRack() {
                return rack.asStringOrNull();
            }

            @Override
            public String getSite() {
                return site.asStringOrNull();
            }
        } : null;

        return Map.entry(new Function<>() {
            @Override
            public TransportConfiguration<T> apply(ProtocolConfiguration<T> configuration) {
                return new TransportProtocolConfigurationDecorator<>(configuration) {
                    @Override
                    public T createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        T protocol = configuration.createProtocol(stackConfiguration);
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
                        threadPool.setKeepAliveTime(threadPoolConfiguration.getKeepAliveTime());
                        // Let JGroups retransmit if pool is full
                        threadPool.setRejectionPolicy("discard");

                        // JGroups propagates this to the ThreadPool
                        protocol.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("jgroups", false, true).useVirtualThreads(protocol.useVirtualThreads()), JChannelFactory.class.getClassLoader()));

                        SocketBinding diagnosticsBinding = diagnostics.get();
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
                        return binding.get();
                    }
                };
            }
        }, threadPool.andThen(binding).andThen(diagnostics));
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
    }
}
