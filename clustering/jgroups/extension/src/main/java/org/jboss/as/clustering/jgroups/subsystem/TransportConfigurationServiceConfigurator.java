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

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.jgroups.ClassLoaderThreadFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jgroups.protocols.TP;
import org.jgroups.util.DefaultThreadFactory;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.service.ServiceSupplierDependency;
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
        this.diagnosticsSocketBinding = (diagnosticsSocketBinding != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, diagnosticsSocketBinding)) : null;

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
        Map<String, SocketBinding> bindings = new HashMap<>();
        SocketBinding binding = this.getSocketBinding();
        for (String serviceName : Arrays.asList("jgroups.udp.mcast_sock", "jgroups.udp.sock", "jgroups.tcp.server", "jgroups.nio.server", "jgroups.tunnel.ucast_sock")) {
            bindings.put(serviceName, binding);
        }
        if (this.diagnosticsSocketBinding != null) {
            bindings.put("jgroups.tp.diag.mcast_sock", this.diagnosticsSocketBinding.get());
        }
        return bindings;
    }

    @Override
    public void accept(T protocol) {
        InetSocketAddress socketAddress = this.getSocketBinding().getSocketAddress();
        protocol.setBindAddress(socketAddress.getAddress());
        protocol.setBindPort(socketAddress.getPort());

        protocol.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("jgroups", false, true), JChannelFactory.class.getClassLoader()));
        protocol.setThreadPool(this.threadPoolFactory.get().apply(protocol.getThreadFactory()));

        protocol.setInternalThreadPoolThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("jgroups-int", false, false), JChannelFactory.class.getClassLoader()));
        // Because we provide the transport with a thread pool, TP.init() won't auto-create the internal thread pool
        // So create one explicitly matching the logic in TP.init() but with our thread factory
        QueuelessThreadPoolFactory factory = new QueuelessThreadPoolFactory()
                .setMaxThreads(Math.max(4, Runtime.getRuntime().availableProcessors()))
                .setKeepAliveTime(30000)
                ;
        protocol.setInternalThreadPool(factory.apply(protocol.getInternalThreadPoolThreadFactory()));

        protocol.setValue("enable_diagnostics", this.diagnosticsSocketBinding != null);
        if (this.diagnosticsSocketBinding != null) {
            InetSocketAddress address = this.diagnosticsSocketBinding.get().getSocketAddress();
            protocol.setValue("diagnostics_addr", address.getAddress());
            protocol.setValue("diagnostics_port", address.getPort());
        }
    }

    @Override
    public Topology getTopology() {
        return this.topology;
    }

    SocketBinding getSocketBinding() {
        return this.socketBinding.get();
    }
}
