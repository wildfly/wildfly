/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.BasicTCP;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class SocketTransportResourceDefinitionRegistrar<T extends BasicTCP> extends AbstractTransportResourceDefinitionRegistrar<T> {
    enum Transport implements ResourceRegistration {
        TCP,
        TCP_NIO2,
        ;
        private final PathElement path = StackResourceDefinitionRegistrar.Component.TRANSPORT.pathElement(this.name());

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    static final CapabilityReferenceAttributeDefinition<SocketBinding> CLIENT_SOCKET_BINDING = new CapabilityReferenceAttributeDefinition.Builder<>("client-socket-binding", CapabilityReference.builder(CAPABILITY, SocketBinding.SERVICE_DESCRIPTOR).build())
            .setRequired(false)
            .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    SocketTransportResourceDefinitionRegistrar(Transport registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return registration;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(CLIENT_SOCKET_BINDING));
    }

    @Override
    public ServiceDependency<TransportConfiguration<T>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String stackName = context.getCurrentAddress().getParent().getLastElement().getValue();
        boolean hasSocketBasedFailureDetectionProtocol = EnumSet.allOf(SocketProtocolResourceRegistration.class).stream().map(Enum::name).anyMatch(fd -> context.hasOptionalCapability(ProtocolConfiguration.SERVICE_DESCRIPTOR, stackName, fd, CAPABILITY, null));
        ServiceDependency<SocketBinding> clientSocketBinding = CLIENT_SOCKET_BINDING.resolve(context, model);
        return super.resolve(context, model).combine(clientSocketBinding, new BiFunction<>() {
            @Override
            public TransportConfiguration<T> apply(TransportConfiguration<T> configuration, SocketBinding clientSocketBinding) {
                return new TransportConfigurationDecorator<>(configuration) {
                    @Override
                    public T createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                        T transport = super.createProtocol(stackConfiguration);
                        if (clientSocketBinding != null) {
                            InetSocketAddress clientSocketAddress = clientSocketBinding.getSocketAddress();
                            this.setValue(transport, "client_bind_addr", clientSocketAddress.getAddress());
                            this.setValue(transport, "client_bind_port", clientSocketAddress.getPort());
                        }
                        if (!hasSocketBasedFailureDetectionProtocol && (transport.getReaperInterval() <= 0) && (transport.getConnExpireTime() <= 0)) {
                            // Auto-enable suspect events if the stack does not otherwise contain a socket-based failure-detection protocol
                            // But do not auto-enable if connection reaping is configured.
                            // See http://jgroups.org/manual5/index.html#_failure_detection_in_tcp_and_tcp_nio2
                            transport.enableSuspectEvents(true);
                        }
                        return transport;
                    }

                    @Override
                    public Map<String, SocketBinding> getSocketBindings() {
                        Map<String, SocketBinding> bindings = super.getSocketBindings();
                        if (clientSocketBinding != null) {
                            bindings = new TreeMap<>(bindings);
                            for (String serviceName : Set.of("jgroups.tcp.sock", "jgroups.nio.client")) {
                                bindings.put(serviceName, clientSocketBinding);
                            }
                        }
                        return bindings;
                    }
                };
            }
        });
    }
}
