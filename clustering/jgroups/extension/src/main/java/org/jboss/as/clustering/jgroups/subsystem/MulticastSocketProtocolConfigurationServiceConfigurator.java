/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.MulticastProtocolResourceDefinition.Attribute.SOCKET_BINDING;

import java.util.Map;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.protocols.MPING;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Custom builder for protocols that need to configure a multicast socket.
 * @author Paul Ferraro
 */
public class MulticastSocketProtocolConfigurationServiceConfigurator extends ProtocolConfigurationServiceConfigurator<MPING> {

    private volatile SupplierDependency<SocketBinding> binding;

    public MulticastSocketProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.binding.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        this.binding = new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, bindingName));
        return super.configure(context, model);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        SocketBinding binding = this.binding.get();
        return Map.of("jgroups.mping.mcast_sock", binding, "jgroups.mping.mcast-send-sock", binding);
    }

    @Override
    public void accept(MPING protocol) {
        SocketBinding binding = this.binding.get();
        protocol.setBindAddr(binding.getAddress());
        protocol.setMcastAddr(binding.getMulticastAddress());
        protocol.setMcastPort(binding.getMulticastPort());
    }
}
