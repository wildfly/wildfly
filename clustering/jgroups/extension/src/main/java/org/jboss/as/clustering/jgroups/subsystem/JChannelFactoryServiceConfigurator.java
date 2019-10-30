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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating channels.
 * @author Paul Ferraro
 */
public class JChannelFactoryServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, ProtocolStackConfiguration {

    private final PathAddress address;

    private volatile boolean statisticsEnabled;

    private volatile SupplierDependency<TransportConfiguration<? extends TP>> transport = null;
    private volatile List<SupplierDependency<ProtocolConfiguration<? extends Protocol>>> protocols = null;
    private volatile SupplierDependency<RelayConfiguration> relay = null;
    private volatile SupplierDependency<SocketBindingManager> socketBindingManager;
    private volatile Supplier<ServerEnvironment> environment;

    public JChannelFactoryServiceConfigurator(PathAddress address) {
        super(StackResourceDefinition.Capability.JCHANNEL_FACTORY, address);
        this.address = address;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<ChannelFactory> factory = new CompositeDependency(this.transport, this.relay, this.socketBindingManager).register(builder).provides(this.getServiceName());
        this.environment = builder.requires(ServerEnvironmentService.SERVICE_NAME);
        for (Dependency dependency : this.protocols) {
            dependency.register(builder);
        }
        Service service = Service.newInstance(factory, new JChannelFactory(this));
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.statisticsEnabled = StackResourceDefinition.Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        Resource resource = context.readResourceFromRoot(this.address, false);
        Iterator<Resource.ResourceEntry> transports = resource.getChildren(TransportResourceDefinition.WILDCARD_PATH.getKey()).iterator();
        if (!transports.hasNext()) {
            throw JGroupsLogger.ROOT_LOGGER.transportNotDefined(this.getName());
        }

        this.transport = new ServiceSupplierDependency<>(new SingletonProtocolServiceNameProvider(this.address, transports.next().getPathElement()));
        Set<Resource.ResourceEntry> entries = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey());
        this.protocols = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            this.protocols.add(new ServiceSupplierDependency<>(new ProtocolServiceNameProvider(this.address, entry.getPathElement())));
        }
        this.relay = resource.hasChild(RelayResourceDefinition.PATH) ? new ServiceSupplierDependency<>(new SingletonProtocolServiceNameProvider(this.address, RelayResourceDefinition.PATH)) : null;

        this.socketBindingManager = new ServiceSupplierDependency<>(CommonRequirement.SOCKET_BINDING_MANAGER.getServiceName(context));

        return this;
    }

    @Override
    public String getName() {
        return this.address.getLastElement().getValue();
    }

    @Override
    public TransportConfiguration<? extends TP> getTransport() {
        return this.transport.get();
    }

    @Override
    public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
        List<ProtocolConfiguration<? extends Protocol>> protocols = new ArrayList<>(this.protocols.size());
        for (Supplier<ProtocolConfiguration<? extends Protocol>> protocolValue : this.protocols) {
            ProtocolConfiguration<? extends Protocol> protocol = protocolValue.get();
            protocols.add(protocol);
        }
        return protocols;
    }

    @Override
    public String getNodeName() {
        return this.environment.get().getNodeName();
    }

    @Override
    public Optional<RelayConfiguration> getRelay() {
        return (this.relay != null) ? Optional.of(this.relay.get()) : Optional.empty();
    }

    @Override
    public boolean isStatisticsEnabled() {
        return this.statisticsEnabled;
    }

    @Override
    public SocketBindingManager getSocketBindingManager() {
        return this.socketBindingManager.get();
    }
}
