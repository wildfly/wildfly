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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.clustering.jgroups.ProtocolDefaults;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.Services;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating channels.
 * @author Paul Ferraro
 */
public class JChannelFactoryBuilder implements Builder<ChannelFactory>, Value<ChannelFactory>, ProtocolStackConfiguration {

    private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<>();
    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final String name;
    private ValueDependency<TransportConfiguration> transport = null;
    private final List<ValueDependency<ProtocolConfiguration>> protocols = new LinkedList<>();
    private ValueDependency<RelayConfiguration> relay = null;

    public JChannelFactoryBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(this.name);
    }

    @Override
    public ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        ServiceBuilder<ChannelFactory> builder = target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(ProtocolDefaultsBuilder.SERVICE_NAME, ProtocolDefaults.class, this.defaults)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
        if (this.transport != null) {
            this.transport.register(builder);
        }
        for (Dependency protocol : this.protocols) {
            protocol.register(builder);
        }
        if (this.relay != null) {
            this.relay.register(builder);
        }
        return builder;
    }

    @Override
    public ChannelFactory getValue() {
        return new JChannelFactory(this);
    }

    public TransportConfigurationBuilder setTransport(String type) {
        TransportConfigurationBuilder builder = new TransportConfigurationBuilder(this.name, type);
        this.transport = new InjectedValueDependency<>(builder, TransportConfiguration.class);
        return builder;
    }

    public ProtocolConfigurationBuilder addProtocol(String type) {
        ProtocolConfigurationBuilder builder = new ProtocolConfigurationBuilder(this.name, type);
        this.protocols.add(new InjectedValueDependency<>(builder, ProtocolConfiguration.class));
        return builder;
    }

    public RelayConfigurationBuilder setRelay(String site) {
        RelayConfigurationBuilder builder = new RelayConfigurationBuilder(this.name).setSiteName(site);
        this.relay = new InjectedValueDependency<>(builder, RelayConfiguration.class);
        return builder;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<String, String> getDefaultProperties(String protocol) {
        return this.defaults.getValue().getProperties(protocol);
    }

    @Override
    public TransportConfiguration getTransport() {
        return (this.transport != null) ? this.transport.getValue() : null;
    }

    @Override
    public List<ProtocolConfiguration> getProtocols() {
        List<ProtocolConfiguration> protocols = new ArrayList<>(this.protocols.size());
        for (Value<ProtocolConfiguration> protocol : this.protocols) {
            protocols.add(protocol.getValue());
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
    public ModuleLoader getModuleLoader() {
        return this.loader.getValue();
    }
}
