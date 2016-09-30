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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.Services;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.IndexExternalizer;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.server.group.JGroupsNodeFactory;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Builds a channel-based {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactoryBuilder extends CommandDispatcherFactoryServiceNameProvider implements Builder<CommandDispatcherFactory>, Service<CommandDispatcherFactory>, ChannelCommandDispatcherFactoryConfiguration, MarshallingConfigurationContext {

    enum MarshallingVersion implements Function<MarshallingConfigurationContext, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(MarshallingConfigurationContext context) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(context.getModuleLoader()));
                config.setClassTable(new DynamicClassTable(IndexExternalizer.UNSIGNED_BYTE, context.getModule().getClassLoader()));
                return config;
            }
        },
        VERSION_2() {
            @Override
            public MarshallingConfiguration apply(MarshallingConfigurationContext context) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(context.getModuleLoader()));
                config.setClassTable(new DynamicClassTable(context.getModule().getClassLoader()));
                config.setObjectTable(new ExternalizerObjectTable(context.getModule().getClassLoader()));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_2;
    }

    private final InjectedValue<ChannelFactory> channelFactory = new InjectedValue<>();
    private final InjectedValue<Channel> channel = new InjectedValue<>();
    private final InjectedValue<JGroupsNodeFactory> nodeFactory = new InjectedValue<>();
    private final InjectedValue<Module> module = new InjectedValue<>();
    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final CapabilityServiceSupport support;

    private volatile MarshallingContext marshallingContext = null;
    private volatile ChannelCommandDispatcherFactory factory = null;
    private volatile long timeout = TimeUnit.MINUTES.toMillis(1);

    public ChannelCommandDispatcherFactoryBuilder(CapabilityServiceSupport support, String group) {
        super(group);
        this.support = support;
    }

    @Override
    public ServiceBuilder<CommandDispatcherFactory> build(ServiceTarget target) {
        ServiceBuilder<CommandDispatcherFactory> builder = new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target)
                .addDependency(GroupServiceName.NODE_FACTORY.getServiceName(this.group), JGroupsNodeFactory.class, this.nodeFactory)
                .addDependency(JGroupsRequirement.CHANNEL.getServiceName(this.support, this.group), Channel.class, this.channel)
                .addDependency(JGroupsRequirement.CHANNEL_FACTORY.getServiceName(this.support, this.group), ChannelFactory.class, this.channelFactory)
                .addDependency(JGroupsRequirement.CHANNEL_MODULE.getServiceName(this.support, this.group), Module.class, this.module)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;
        return builder;
    }

    public ChannelCommandDispatcherFactoryBuilder timeout(long value, TimeUnit unit) {
        this.timeout = unit.toMillis(value);
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, this), this.getModule().getClassLoader());
        this.factory = new ChannelCommandDispatcherFactory(this);
    }

    @Override
    public void stop(StopContext context) {
        this.factory.close();
        this.factory = null;
        this.marshallingContext = null;
    }

    @Override
    public CommandDispatcherFactory getValue() {
        return this.factory;
    }

    @Override
    public Module getModule() {
        return this.module.getValue();
    }

    @Override
    public ModuleLoader getModuleLoader() {
        return this.loader.getValue();
    }

    @Override
    public Channel getChannel() {
        return this.channel.getValue();
    }

    @Override
    public JGroupsNodeFactory getNodeFactory() {
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
    public ChannelFactory getChannelFactory() {
        return this.channelFactory.getValue();
    }
}
