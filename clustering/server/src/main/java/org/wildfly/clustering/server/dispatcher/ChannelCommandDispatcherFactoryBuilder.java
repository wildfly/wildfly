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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.Services;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.JChannel;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a channel-based {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactoryBuilder implements CapabilityServiceBuilder<CommandDispatcherFactory>, ChannelCommandDispatcherFactoryConfiguration, MarshallingConfigurationContext {

    enum MarshallingVersion implements Function<MarshallingConfigurationContext, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(MarshallingConfigurationContext context) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                config.setClassResolver(ModularClassResolver.getInstance(context.getModuleLoader()));
                config.setClassTable(new DynamicClassTable(context.getModule().getClassLoader()));
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

    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final ServiceName name;
    private final String group;

    private volatile ValueDependency<ChannelFactory> channelFactory;
    private volatile ValueDependency<JChannel> channel;
    private volatile ValueDependency<Module> module;
    private volatile long timeout = TimeUnit.MINUTES.toMillis(1);

    public ChannelCommandDispatcherFactoryBuilder(ServiceName name, String group) {
        this.name = name;
        this.group = group;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<CommandDispatcherFactory> configure(CapabilityServiceSupport support) {
        this.channel = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL.getServiceName(support, this.group), JChannel.class);
        this.channelFactory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_FACTORY.getServiceName(support, this.group), ChannelFactory.class);
        this.module = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_MODULE.getServiceName(support, this.group), Module.class);
        return this;
    }

    @Override
    public ServiceBuilder<CommandDispatcherFactory> build(ServiceTarget target) {
        Supplier<AutoCloseableCommandDispatcherFactory> supplier = () -> new ManagedCommandDispatcherFactory(new ChannelCommandDispatcherFactory(this));
        Service<CommandDispatcherFactory> service = new SuppliedValueService<>(Functions.identity(), supplier, Consumers.close());
        ServiceBuilder<CommandDispatcherFactory> builder = new AsynchronousServiceBuilder<>(this.name, service).build(target)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                ;
        Stream.of(this.channel, this.channelFactory, this.module).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    public ChannelCommandDispatcherFactoryBuilder timeout(long value, TimeUnit unit) {
        this.timeout = unit.toMillis(value);
        return this;
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
    public JChannel getChannel() {
        return this.channel.getValue();
    }

    @Override
    public MarshallingContext getMarshallingContext() {
        return new SimpleMarshallingContextFactory().createMarshallingContext(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, this), this.getModule().getClassLoader());
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
