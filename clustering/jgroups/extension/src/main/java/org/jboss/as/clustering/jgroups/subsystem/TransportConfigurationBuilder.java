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

import static org.jboss.as.clustering.jgroups.subsystem.SocketBindingProtocolResourceDefinition.Attribute.*;
import static org.jboss.as.clustering.jgroups.subsystem.TransportResourceDefinition.Attribute.*;

import java.net.InetSocketAddress;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.ClassLoaderThreadFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.Value;
import org.jgroups.protocols.TP;
import org.jgroups.util.DefaultThreadFactory;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class TransportConfigurationBuilder<T extends TP> extends AbstractProtocolConfigurationBuilder<T, TransportConfiguration<T>> implements TransportConfiguration<T> {

    private final PathAddress address;
    private final EnumMap<ThreadPoolResourceDefinition, ValueDependency<ThreadPoolFactory>> threadPoolFactories = new EnumMap<>(ThreadPoolResourceDefinition.class);

    private volatile ValueDependency<TimerFactory> timerFactory;
    private volatile ValueDependency<SocketBinding> socketBinding;
    private volatile ValueDependency<SocketBinding> diagnosticsSocketBinding;
    private volatile Topology topology = null;

    public TransportConfigurationBuilder(PathAddress address) {
        super(address.getLastElement().getValue());
        this.address = address;
    }

    @Override
    public ServiceName getServiceName() {
        return new SingletonProtocolServiceNameProvider(this.address).getServiceName();
    }

    @Override
    public ServiceBuilder<TransportConfiguration<T>> build(ServiceTarget target) {
        ServiceBuilder<TransportConfiguration<T>> builder = super.build(target);
        Stream.concat(this.threadPoolFactories.values().stream(), Stream.of(this.timerFactory, this.socketBinding, this.diagnosticsSocketBinding)).filter(Objects::nonNull).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public Builder<TransportConfiguration<T>> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.socketBinding = new InjectedValueDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, SOCKET_BINDING.resolveModelAttribute(context, model).asString()), SocketBinding.class);
        this.diagnosticsSocketBinding = ModelNodes.optionalString(DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, model)).map(diagnosticsBinding -> new InjectedValueDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, diagnosticsBinding), SocketBinding.class)).orElse(null);

        Optional<String> machine = ModelNodes.optionalString(MACHINE.resolveModelAttribute(context, model));
        Optional<String> rack = ModelNodes.optionalString(RACK.resolveModelAttribute(context, model));
        Optional<String> site = ModelNodes.optionalString(SITE.resolveModelAttribute(context, model));
        if (site.isPresent() || rack.isPresent() || machine.isPresent()) {
            this.topology = new Topology() {
                @Override
                public String getMachine() {
                    return machine.orElse(null);
                }

                @Override
                public String getRack() {
                    return rack.orElse(null);
                }

                @Override
                public String getSite() {
                    return site.orElse(null);
                }
            };
        }

        PathAddress address = context.getCurrentAddress();
        EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.TIMER)).forEach(pool -> this.threadPoolFactories.put(pool, new InjectedValueDependency<>(new ThreadPoolServiceNameProvider(address, pool.getPathElement()), ThreadPoolFactory.class)));
        this.timerFactory = new InjectedValueDependency<>(new ThreadPoolServiceNameProvider(address, ThreadPoolResourceDefinition.TIMER.getPathElement()), TimerFactory.class);

        return super.configure(context, model);
    }

    @Override
    public void accept(T protocol) {
        InetSocketAddress socketAddress = this.getSocketBinding().getSocketAddress();
        protocol.setBindAddress(socketAddress.getAddress());
        protocol.setBindPort(socketAddress.getPort());

        protocol.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("", false), JChannelFactory.class.getClassLoader()));

        protocol.setDefaultThreadPool(this.threadPoolFactories.get(ThreadPoolResourceDefinition.DEFAULT).getValue().get());
        protocol.setInternalThreadPool(this.threadPoolFactories.get(ThreadPoolResourceDefinition.INTERNAL).getValue().get());
        protocol.setOOBThreadPool(this.threadPoolFactories.get(ThreadPoolResourceDefinition.OOB).getValue().get());
        protocol.setTimer(this.timerFactory.getValue().get());

        Optional<InetSocketAddress> diagnosticsSocketAddress = Optional.ofNullable(this.diagnosticsSocketBinding).map(Value::getValue).map(SocketBinding::getSocketAddress);
        protocol.setValue("enable_diagnostics", diagnosticsSocketAddress.isPresent());
        diagnosticsSocketAddress.ifPresent(address -> {
            protocol.setValue("diagnostics_addr", address.getAddress());
            protocol.setValue("diagnostics_port", address.getPort());
        });
    }

    @Override
    public TransportConfiguration<T> getValue() {
        return this;
    }

    @Override
    public SocketBinding getSocketBinding() {
        return this.socketBinding.getValue();
    }

    @Override
    public Topology getTopology() {
        return this.topology;
    }
}
