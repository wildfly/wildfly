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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
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
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating channels.
 * @author Paul Ferraro
 */
public class JChannelFactoryBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<ChannelFactory>, ProtocolStackConfiguration {

    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<>();
    private final String name;
    private volatile boolean statisticsEnabled;

    @SuppressWarnings("rawtypes")
    private volatile ValueDependency<TransportConfiguration> transport = null;
    @SuppressWarnings("rawtypes")
    private volatile List<ValueDependency<ProtocolConfiguration>> protocols = null;
    private volatile ValueDependency<RelayConfiguration> relay = null;

    public JChannelFactoryBuilder(PathAddress address) {
        super(StackResourceDefinition.Capability.JCHANNEL_FACTORY, address);
        this.name = address.getLastElement().getValue();
    }

    @Override
    public ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        Value<ChannelFactory> value = () -> new JChannelFactory(this);
        ServiceBuilder<ChannelFactory> builder = target.addService(this.getServiceName(), new ValueService<>(value))
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
        Stream.concat(Stream.of(this.transport, this.relay).filter(Objects::nonNull), this.protocols.stream()).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public Builder<ChannelFactory> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        this.statisticsEnabled = StackResourceDefinition.Attribute.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        Optional<PathElement> transport = resource.getChildren(TransportResourceDefinition.WILDCARD_PATH.getKey()).stream().map(Resource.ResourceEntry::getPathElement).findFirst();
        if (!transport.isPresent()) {
            throw JGroupsLogger.ROOT_LOGGER.transportNotDefined(this.getName());
        }
        this.transport = new InjectedValueDependency<>(new SingletonProtocolServiceNameProvider(address, transport.get()), TransportConfiguration.class);
        this.protocols = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).stream().map(entry -> new InjectedValueDependency<>(new ProtocolServiceNameProvider(address, entry.getPathElement()), ProtocolConfiguration.class)).collect(Collectors.toList());
        this.relay = resource.hasChild(RelayResourceDefinition.PATH) ? new InjectedValueDependency<>(new SingletonProtocolServiceNameProvider(address, RelayResourceDefinition.PATH), RelayConfiguration.class) : null;

        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public TransportConfiguration<? extends TP> getTransport() {
        return this.transport.getValue();
    }

    @Override
    public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
        // This works in eclipse, but fails in openjdk 1.8.0_121
        // return this.protocols.stream().map(Value::getValue).collect(Collectors.toList());
        List<ProtocolConfiguration<? extends Protocol>> protocols = new ArrayList<>(this.protocols.size());
        for (@SuppressWarnings("rawtypes") Value<ProtocolConfiguration> protocolValue : this.protocols) {
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
    public RelayConfiguration getRelay() {
        return (this.relay != null) ? this.relay.getValue() : null;
    }

    @Override
    public boolean isStatisticsEnabled() {
        return this.statisticsEnabled;
    }
}
