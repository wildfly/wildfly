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

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.jgroups.protocol.MulticastSocketProtocol;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Custom builder for protocols that need to configure a multicast socket.
 * @author Paul Ferraro
 */
public class MulticastSocketProtocolConfigurationBuilder<P extends Protocol & MulticastSocketProtocol> extends ProtocolConfigurationBuilder<P> {

    private volatile ValueDependency<SocketBinding> binding;

    public MulticastSocketProtocolConfigurationBuilder(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceBuilder<ProtocolConfiguration<P>> build(ServiceTarget target) {
        return this.binding.register(super.build(target));
    }

    @Override
    public Builder<ProtocolConfiguration<P>> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String bindingName = SocketBindingProtocolResourceDefinition.Attribute.SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        this.binding = new InjectedValueDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, bindingName), SocketBinding.class);
        return super.configure(context, model);
    }

    @Override
    public void accept(P protocol) {
        protocol.setMulticastSocketAddress(this.binding.getValue().getMulticastSocketAddress());
    }
}
