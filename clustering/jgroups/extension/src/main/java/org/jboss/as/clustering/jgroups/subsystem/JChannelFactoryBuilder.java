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

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating channels.
 * @author Paul Ferraro
 */
public class JChannelFactoryBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<ChannelFactory>, ProtocolStackConfiguration {

    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<>();
    private final PathAddress address;

    private volatile boolean statisticsEnabled;

    private volatile ValueDependency<TransportConfiguration<? extends TP>> transport = null;
    private volatile List<ValueDependency<ProtocolConfiguration<? extends Protocol>>> protocols = null;
    private volatile ValueDependency<RelayConfiguration> relay = null;

    public JChannelFactoryBuilder(PathAddress address) {
        super(StackResourceDefinition.Capability.JCHANNEL_FACTORY, address);
        this.address = address;
    }

    @Override
    public ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        Value<ChannelFactory> value = () -> new JChannelFactory(this);
        ServiceBuilder<ChannelFactory> builder = target.addService(this.getServiceName(), new ValueService<>(value))
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
        for (Dependency dependency : this.protocols) {
            dependency.register(builder);
        }
        return new CompositeDependency(this.transport, this.relay).register(builder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<ChannelFactory> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.statisticsEnabled = StackResourceDefinition.Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        Resource resource = context.readResourceFromRoot(this.address, false);
        Iterator<Resource.ResourceEntry> transports = resource.getChildren(TransportResourceDefinition.WILDCARD_PATH.getKey()).iterator();
        if (!transports.hasNext()) {
            throw JGroupsLogger.ROOT_LOGGER.transportNotDefined(this.getName());
        }

        this.transport = new InjectedValueDependency<>(new SingletonProtocolServiceNameProvider(this.address, transports.next().getPathElement()), (Class<TransportConfiguration<? extends TP>>) (Class<?>) TransportConfiguration.class);
        Set<Resource.ResourceEntry> entries = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey());
        this.protocols = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            this.protocols.add(new InjectedValueDependency<>(new ProtocolServiceNameProvider(this.address, entry.getPathElement()), (Class<ProtocolConfiguration<? extends Protocol>>) (Class<?>) ProtocolConfiguration.class));
        }
        this.relay = resource.hasChild(RelayResourceDefinition.PATH) ? new InjectedValueDependency<>(new SingletonProtocolServiceNameProvider(this.address, RelayResourceDefinition.PATH), RelayConfiguration.class) : null;

        return this;
    }

    @Override
    public String getName() {
        return this.address.getLastElement().getValue();
    }

    @Override
    public TransportConfiguration<? extends TP> getTransport() {
        return this.transport.getValue();
    }

    @Override
    public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
        List<ProtocolConfiguration<? extends Protocol>> protocols = new ArrayList<>(this.protocols.size());
        for (Value<ProtocolConfiguration<? extends Protocol>> protocolValue : this.protocols) {
            ProtocolConfiguration<? extends Protocol> protocol = protocolValue.getValue();
            protocols.add(protocol);
        }
        return protocols;
    }

    @Override
    public String getNodeName() {
        return this.environment.getValue().getNodeName();
    }

    @Override
    public Optional<RelayConfiguration> getRelay() {
        return (this.relay != null) ? Optional.of(this.relay.getValue()) : Optional.empty();
    }

    @Override
    public boolean isStatisticsEnabled() {
        return this.statisticsEnabled;
    }
}
