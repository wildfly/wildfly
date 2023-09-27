/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.SocketDiscoveryProtocolResourceDefinition.Attribute.OUTBOUND_SOCKET_BINDINGS;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.protocols.Discovery;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class SocketDiscoveryProtocolConfigurationServiceConfigurator<A, P extends Discovery> extends ProtocolConfigurationServiceConfigurator<P> {

    private final Function<InetSocketAddress, A> hostTransformer;
    private final List<SupplierDependency<OutboundSocketBinding>> bindings = new LinkedList<>();

    public SocketDiscoveryProtocolConfigurationServiceConfigurator(PathAddress address, Function<InetSocketAddress, A> hostTransformer) {
        super(address);
        this.hostTransformer = hostTransformer;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.bindings.clear();
        for (String binding : StringListAttributeDefinition.unwrapValue(context, OUTBOUND_SOCKET_BINDINGS.resolveModelAttribute(context, model))) {
            this.bindings.add(new ServiceSupplierDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, binding)));
        }
        return super.configure(context, model);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        for (Dependency dependency : this.bindings) {
            dependency.register(builder);
        }
        return super.register(builder);
    }

    @Override
    public void accept(P protocol) {
        if (!this.bindings.isEmpty()) {
            List<A> initialHosts = new ArrayList<>(this.bindings.size());
            for (Supplier<OutboundSocketBinding> bindingValue : this.bindings) {
                OutboundSocketBinding binding = bindingValue.get();
                try {
                    initialHosts.add(this.hostTransformer.apply(new InetSocketAddress(binding.getResolvedDestinationAddress(), binding.getDestinationPort())));
                } catch (UnknownHostException e) {
                    throw JGroupsLogger.ROOT_LOGGER.failedToResolveSocketBinding(e, binding);
                }
            }
            // In the absence of some common interface, we need to use reflection
            this.setValue(protocol, "initial_hosts", initialHosts);
        }
    }
}
