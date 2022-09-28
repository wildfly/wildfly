/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.DIAGNOSTICS_SOCKET_BINDING;
import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.MACHINE;
import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.RACK;
import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.SITE;
import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.SOCKET_BINDING;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.jgroups.ClassLoaderThreadFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jgroups.protocols.TP;
import org.jgroups.stack.DiagnosticsHandler;
import org.jgroups.util.DefaultThreadFactory;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class TransportConfigurationServiceConfigurator<T extends TP> extends AbstractProtocolConfigurationServiceConfigurator<T, TransportConfiguration<T>> implements TransportConfiguration<T> {

    private final ServiceNameProvider provider;
    private final SupplierDependency<ThreadPoolFactory> threadPoolFactory;

    private volatile SupplierDependency<SocketBinding> socketBinding;
    private volatile SupplierDependency<SocketBinding> diagnosticsSocketBinding;
    private volatile Topology topology = null;

    public TransportConfigurationServiceConfigurator(PathAddress address) {
        super(address.getLastElement().getValue());
        this.provider = new SingletonProtocolServiceNameProvider(address);
        this.threadPoolFactory = new ServiceSupplierDependency<>(new ThreadPoolServiceNameProvider(address, ThreadPoolResourceDefinition.DEFAULT.getPathElement()));
    }

    @Override
    public ServiceName getServiceName() {
        return this.provider.getServiceName();
    }

    @Override
    public TransportConfiguration<T> get() {
        return this;
    }

    @Override
    public <B> ServiceBuilder<B> register(ServiceBuilder<B> builder) {
        return super.register(new CompositeDependency(this.threadPoolFactory, this.socketBinding, this.diagnosticsSocketBinding).register(builder));
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.socketBinding = new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, SOCKET_BINDING.resolveModelAttribute(context, model).asString()));
        String diagnosticsSocketBinding = DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        this.diagnosticsSocketBinding = (diagnosticsSocketBinding != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, diagnosticsSocketBinding)) : new SimpleSupplierDependency<>(null);

        ModelNode machine = MACHINE.resolveModelAttribute(context, model);
        ModelNode rack = RACK.resolveModelAttribute(context, model);
        ModelNode site = SITE.resolveModelAttribute(context, model);
        if (site.isDefined() || rack.isDefined() || machine.isDefined()) {
            this.topology = new Topology() {
                @Override
                public String getMachine() {
                    return machine.asStringOrNull();
                }

                @Override
                public String getRack() {
                    return rack.asStringOrNull();
                }

                @Override
                public String getSite() {
                    return site.asStringOrNull();
                }
            };
        }

        return super.configure(context, model);
    }

    @Override
    public Map<String, SocketBinding> getSocketBindings() {
        Map<String, SocketBinding> bindings = new TreeMap<>();
        SocketBinding binding = this.getSocketBinding();
        for (String serviceName : Set.of("jgroups.udp.mcast_sock", "jgroups.udp.sock", "jgroups.tcp.server", "jgroups.nio.client", "jgroups.nio.server", "jgroups.tunnel.ucast_sock")) {
            bindings.put(serviceName, binding);
        }
        bindings.put("jgroups.tp.diag.mcast_sock", this.diagnosticsSocketBinding.get());
        return bindings;
    }

    @Override
    public void accept(T protocol) {
        SocketBinding binding = this.getSocketBinding();
        InetSocketAddress socketAddress = binding.getSocketAddress();
        protocol.setBindAddress(socketAddress.getAddress());
        protocol.setBindPort(socketAddress.getPort());

        List<ClientMapping> clientMappings = binding.getClientMappings();
        if (!clientMappings.isEmpty()) {
            // JGroups cannot select a client mapping based on the source address, so just use the first one
            ClientMapping mapping = clientMappings.get(0);
            try {
                protocol.setExternalAddr(InetAddress.getByName(mapping.getDestinationAddress()));
                protocol.setExternalPort(mapping.getDestinationPort());
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        }

        protocol.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("jgroups", false, true), JChannelFactory.class.getClassLoader()));
        protocol.setThreadPool(this.threadPoolFactory.get().apply(protocol.getThreadFactory()));

        SocketBinding diagnosticsBinding = this.diagnosticsSocketBinding.get();
        if (diagnosticsBinding != null) {
            DiagnosticsHandler handler = new DiagnosticsHandler(protocol.getLog(), protocol.getSocketFactory(), protocol.getThreadFactory());
            InetSocketAddress address = diagnosticsBinding.getSocketAddress();
            handler.setBindAddress(address.getAddress());
            if (diagnosticsBinding.getMulticastAddress() != null) {
                handler.setMcastAddress(diagnosticsBinding.getMulticastAddress());
                handler.setPort(diagnosticsBinding.getMulticastPort());
            } else {
                handler.setPort(diagnosticsBinding.getPort());
            }
            try {
                protocol.setDiagnosticsHandler(handler);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public Topology getTopology() {
        return this.topology;
    }

    @Override
    public SocketBinding getSocketBinding() {
        return this.socketBinding.get();
    }
}
