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

import static org.jboss.as.clustering.jgroups.subsystem.OptionalSocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.protocols.FD_SOCK;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service that provides a FD_SOCK protocol.
 * @author Paul Ferraro
 */
public class SocketProtocolConfigurationServiceConfigurator extends ProtocolConfigurationServiceConfigurator<FD_SOCK> {

    private volatile SupplierDependency<SocketBinding> binding;

    public SocketProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.binding.register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = SOCKET_BINDING.resolveModelAttribute(context, model).asString(null);
        this.binding = (bindingName != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, bindingName)) : new SimpleSupplierDependency<>(null);
        return super.configure(context, model);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        return Collections.singletonMap("jgroups.fd_sock.srv_sock", this.binding.get());
    }

    @Override
    public void accept(FD_SOCK protocol) {
        // If binding is undefined, protocol will use bind address of transport and a random ephemeral port
        SocketBinding binding = this.binding.get();
        if (binding != null) {
            protocol.setValue("bind_addr", binding.getAddress());
            protocol.setValue("start_port", binding.getPort());

            List<ClientMapping> clientMappings = binding.getClientMappings();
            if (!clientMappings.isEmpty()) {
                // JGroups cannot select a client mapping based on the source address, so just use the first one
                ClientMapping mapping = clientMappings.get(0);
                try {
                    this.setValue(protocol, "external_addr", InetAddress.getByName(mapping.getDestinationAddress()));
                    this.setValue(protocol, "external_port", mapping.getDestinationPort());
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }
}
