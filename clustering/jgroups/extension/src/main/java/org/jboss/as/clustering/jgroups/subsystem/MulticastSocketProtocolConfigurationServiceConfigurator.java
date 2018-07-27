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

import java.util.Collections;
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
        String bindingName = SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        this.binding = new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, bindingName));
        return super.configure(context, model);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        return Collections.singletonMap("jgroups.mping.mcast_sock", this.binding.get());
    }

    @Override
    public void accept(MPING protocol) {
        SocketBinding binding = this.binding.get();
        protocol.setMcastAddr(binding.getMulticastAddress());
        protocol.setMcastPort(binding.getMulticastPort());
    }
}
