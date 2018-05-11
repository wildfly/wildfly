/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.SocketDiscoveryProtocolResourceDefinition.Attribute.OUTBOUND_SOCKET_BINDINGS;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
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
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.security.manager.WildFlySecurityManager;

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
            PrivilegedAction<Protocol> action = () -> protocol.setValue("initial_hosts", initialHosts);
            WildFlySecurityManager.doUnchecked(action);
        }
    }
}
