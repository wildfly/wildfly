/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.dispatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.clustering.marshalling.DynamicClassTable;
import org.jboss.as.clustering.marshalling.MarshallingConfigurationFactory;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.as.clustering.marshalling.SimpleMarshallingContextFactory;
import org.jboss.as.clustering.marshalling.VersionedMarshallingConfiguration;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.server.Services;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.server.group.ChannelNodeFactory;
import org.wildfly.clustering.spi.ChannelServiceNames;

/**
 * Service providing a CommandDispatcherFactory.
 * Multiple command dispatchers share a single {@link MessageDispatcher}.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactoryService implements Service<CommandDispatcherFactory>, ChannelCommandDispatcherFactoryConfiguration, VersionedMarshallingConfiguration {

    private static final int CURRENT_VERSION = 1;

    public static ServiceBuilder<CommandDispatcherFactory> build(ServiceTarget target, ServiceName name, String cluster, ModuleIdentifier identifier) {
        ChannelCommandDispatcherFactoryService service = new ChannelCommandDispatcherFactoryService(identifier);
        return AsynchronousService.addService(target, name, service)
                // Make sure Infinispan starts its channel before we try to use it.
                .addDependency(GlobalComponentRegistryService.getServiceName(cluster))
                .addDependency(ChannelServiceNames.GROUP.getServiceName(cluster), Group.class, service.group)
                .addDependency(ChannelServiceNames.NODE_FACTORY.getServiceName(cluster), ChannelNodeFactory.class, service.nodeFactory)
                .addDependency(ChannelService.getServiceName(cluster), Channel.class, service.channel)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, service.loader)
        ;
    }

    private final Map<Integer, MarshallingConfiguration> configurations = new HashMap<>();

    private final InjectedValue<Channel> channel = new InjectedValue<>();
    private final InjectedValue<Group> group = new InjectedValue<>();
    private final InjectedValue<ChannelNodeFactory> nodeFactory = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final ModuleIdentifier identifier;
    private final long timeout = TimeUnit.MINUTES.toMillis(1);

    private volatile MarshallingContext marshallingContext = null;
    private volatile ChannelCommandDispatcherFactory factory = null;

    private ChannelCommandDispatcherFactoryService(ModuleIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ModuleLoader loader = this.loader.getValue();
        MarshallingConfiguration config = MarshallingConfigurationFactory.createMarshallingConfiguration(loader);
        try {
            Module module = loader.loadModule(this.identifier);
            config.setClassTable(new DynamicClassTable(module.getClassLoader()));
            this.configurations.put(CURRENT_VERSION, config);
            this.marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(this, module.getClassLoader());
        } catch (ModuleLoadException e) {
            throw new StartException(e);
        }

        this.factory = new ChannelCommandDispatcherFactory(this);
    }

    @Override
    public void stop(StopContext context) {
        try {
            this.factory.close();
            this.factory = null;
            this.marshallingContext = null;
        } finally {
            this.configurations.clear();
        }
    }

    @Override
    public CommandDispatcherFactory getValue() {
        return this.factory;
    }

    @Override
    public Group getGroup() {
        return this.group.getValue();
    }

    @Override
    public Channel getChannel() {
        return this.channel.getValue();
    }

    @Override
    public NodeFactory<Address> getNodeFactory() {
        return this.nodeFactory.getValue();
    }

    @Override
    public MarshallingContext getMarshallingContext() {
        return this.marshallingContext;
    }

    @Override
    public long getTimeout() {
        return this.timeout;
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        MarshallingConfiguration configuration = this.configurations.get(version);
        if (configuration == null) {
            throw new IllegalArgumentException(Integer.toString(version));
        }
        return configuration;
    }
}
