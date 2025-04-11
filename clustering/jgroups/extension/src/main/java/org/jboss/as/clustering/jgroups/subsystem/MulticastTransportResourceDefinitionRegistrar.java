/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Function;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jgroups.protocols.UDP;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a multicast-capable transport, i.e. UDP.
 * @author Paul Ferraro
 */
public class MulticastTransportResourceDefinitionRegistrar extends AbstractTransportResourceDefinitionRegistrar<UDP> {
    enum Transport implements ResourceRegistration {
        UDP;

        private final PathElement path = StackResourceDefinitionRegistrar.Component.TRANSPORT.pathElement(this.name());

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    MulticastTransportResourceDefinitionRegistrar(Transport registration, ResourceOperationRuntimeHandler parentRuntimeHandler) {
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
    public ServiceDependency<TransportConfiguration<UDP>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return super.resolve(context, model).map(new Function<>() {
            @Override
            public TransportConfiguration<UDP> apply(TransportConfiguration<UDP> configuration) {
                return new TransportConfigurationDecorator<>(configuration) {
                    @Override
                    public UDP createProtocol(ChannelFactoryConfiguration stackConfiguration) {
                        SocketBinding binding = configuration.getSocketBinding();
                        UDP transport = super.createProtocol(stackConfiguration);
                        transport.setMulticasting(binding.getMulticastAddress() != null);
                        if (transport.supportsMulticasting()) {
                            transport.setMulticastAddress(binding.getMulticastAddress());
                            transport.setMulticastPort(binding.getMulticastPort());
                        }
                        return transport;
                    }
                };
            }
        });
    }
}
