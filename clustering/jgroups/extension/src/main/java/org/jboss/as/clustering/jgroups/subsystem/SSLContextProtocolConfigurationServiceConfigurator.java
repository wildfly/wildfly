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

import static org.jboss.as.clustering.jgroups.subsystem.SSLContextProtocolResourceDefinition.Attribute.CLIENT_SSL_CONTEXT;
import static org.jboss.as.clustering.jgroups.subsystem.SSLContextProtocolResourceDefinition.Attribute.SERVER_SSL_CONTEXT;
import static org.jboss.as.clustering.jgroups.subsystem.SSLContextProtocolResourceDefinition.Attribute.SOCKET_BINDING;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.protocols.SSL_KEY_EXCHANGE;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service that provides a {@link SSL_KEY_EXCHANGE} protocol with client and server {@link SSLContext}s.
 *
 * @author Radoslav Husar
 */
public class SSLContextProtocolConfigurationServiceConfigurator extends ProtocolConfigurationServiceConfigurator<SSL_KEY_EXCHANGE> {

    private volatile SupplierDependency<SSLContext> clientSslContext;
    private volatile SupplierDependency<SSLContext> serverSslContext;
    private volatile SupplierDependency<SocketBinding> socketBinding;

    public SSLContextProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String clientSslContextName = CLIENT_SSL_CONTEXT.resolveModelAttribute(context, model).asString();
        this.clientSslContext = new ServiceSupplierDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, clientSslContextName));
        String serverSslContextName = SERVER_SSL_CONTEXT.resolveModelAttribute(context, model).asString();
        this.serverSslContext = new ServiceSupplierDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, serverSslContextName));

        String bindingName = SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        socketBinding = (bindingName != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, bindingName)) : new SimpleSupplierDependency<>(null);

        return super.configure(context, model);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(new CompositeDependency(this.clientSslContext, this.serverSslContext, this.socketBinding).register(builder));
    }

    @Override
    public void accept(SSL_KEY_EXCHANGE protocol) {
        protocol.setClientSSLContext(clientSslContext.get());
        protocol.setServerSSLContext(serverSslContext.get());

        SocketBinding binding = this.socketBinding.get();
        if (binding != null) {
            protocol.setBindAddress(binding.getAddress());
            protocol.setPort(binding.getPort());
        }
    }
}
