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

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating channels.
 * @author Paul Ferraro
 */
public class JChannelFactoryBuilder implements Builder<ChannelFactory>, Value<ChannelFactory>, ProtocolStackConfiguration {

    private final InjectedValue<ProtocolDefaults> defaults = new InjectedValue<>();
    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final String name;
    private final TransportConfiguration transport;
    private final List<ProtocolConfiguration> protocols;
    private final RelayConfiguration relay;

    public JChannelFactoryBuilder(String name, TransportConfiguration transport, List<ProtocolConfiguration> protocols, RelayConfiguration relay) {
        this.name = name;
        this.transport = transport;
        this.protocols = protocols;
        this.relay = relay;
    }

    @Override
    public ServiceName getServiceName() {
        return ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(this.name);
    }

    @Override
    public ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(ProtocolDefaultsBuilder.SERVICE_NAME, ProtocolDefaults.class, this.defaults)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ChannelFactory getValue() {
        return new JChannelFactory(this);
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
        return this.transport;
    }

    @Override
    public List<ProtocolConfiguration> getProtocols() {
        return this.protocols;
    }

    @Override
    public String getNodeName() {
        return this.environment.getValue().getNodeName();
    }

    @Override
    public RelayConfiguration getRelay() {
        return this.relay;
    }

    @Override
    public ModuleLoader getModuleLoader() {
        return this.loader.getValue();
    }
}
