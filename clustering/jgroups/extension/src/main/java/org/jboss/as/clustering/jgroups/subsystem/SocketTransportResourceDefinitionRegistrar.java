/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.BasicTCP;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * @author Paul Ferraro
 */
public class SocketTransportResourceDefinitionRegistrar<T extends BasicTCP> extends AbstractTransportResourceDefinitionRegistrar<T> {

    SocketTransportResourceDefinitionRegistrar(SocketTransportResourceDescription description, ResourceOperationRuntimeHandler parentRuntimeHandler) {
        super(new TransportResourceDescriptorConfigurator<>() {
            @Override
            public TransportResourceDescription getResourceDescription() {
                return description;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }

            @Override
            public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
                return TransportResourceDescriptorConfigurator.super.apply(builder).provideAttributes(EnumSet.of(TransportResourceDescription.SocketBindingAttribute.CLIENT));
            }

            @Override
            public ServiceDependency<TransportConfiguration<T>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                ServiceDependency<SocketBinding> clientSocketBinding = TransportResourceDescription.SocketBindingAttribute.CLIENT.resolve(context, model);
                return TransportResourceDescriptorConfigurator.super.resolve(context, model).combine(clientSocketBinding, new BiFunction<>() {
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
        });
    }
}
