/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.jgroups.subsystem.SocketTransportResourceDefinition.Attribute.*;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.protocols.BasicTCP;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class SocketTransportConfigurationServiceConfigurator<TP extends BasicTCP> extends TransportConfigurationServiceConfigurator<TP> {

    private volatile SupplierDependency<SocketBinding> clientBinding;

    public SocketTransportConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public <B> ServiceBuilder<B> register(ServiceBuilder<B> builder) {
        return super.register(this.clientBinding.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = CLIENT_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        this.clientBinding = (bindingName != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, bindingName)) : new SimpleSupplierDependency<>(null);
        return super.configure(context, model);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        Map<String, SocketBinding> bindings = super.getSocketBindings();
        SocketBinding clientBinding = this.clientBinding.get();
        for (String serviceName : Set.of("jgroups.tcp.sock", "jgroups.nio.client")) {
            bindings.put(serviceName, clientBinding);
        }
        return bindings;
    }

    @Override
    public void accept(TP protocol) {
        SocketBinding clientBinding = this.clientBinding.get();
        if (clientBinding != null) {
            InetSocketAddress socketAddress = clientBinding.getSocketAddress();
            this.setValue(protocol, "client_bind_addr", socketAddress.getAddress());
            this.setValue(protocol, "client_bind_port", socketAddress.getPort());
        }
        super.accept(protocol);
    }
}
