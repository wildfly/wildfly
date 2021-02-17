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

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.clustering.function.Functions;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.Services;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.JChannel;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Builds a channel-based {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactoryServiceConfigurator extends SimpleServiceNameProvider implements CapabilityServiceConfigurator, ChannelCommandDispatcherFactoryConfiguration, Supplier<AutoCloseableCommandDispatcherFactory>, Function<ClassLoader, ByteBufferMarshaller> {

    enum MarshallingVersion implements Function<Map.Entry<ClassResolver, ClassLoader>, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(Map.Entry<ClassResolver, ClassLoader> entry) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                ClassLoader userLoader = entry.getValue();
                ClassLoader loader = WildFlySecurityManager.getClassLoaderPrivileged(ChannelCommandDispatcherFactory.class);
                ClassLoader[] loaders = userLoader.equals(loader) ? new ClassLoader[] { userLoader } : new ClassLoader[] { userLoader, loader };
                config.setClassResolver(entry.getKey());
                config.setClassTable(new DynamicClassTable(loaders));
                config.setObjectTable(new ExternalizerObjectTable(loaders));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_1;
    }

    private final String group;

    private volatile SupplierDependency<ChannelFactory> channelFactory;
    private volatile SupplierDependency<JChannel> channel;
    private volatile SupplierDependency<Module> module;
    private volatile Supplier<ModuleLoader> loader;
    private volatile Duration timeout = Duration.ofMinutes(1);

    public ChannelCommandDispatcherFactoryServiceConfigurator(ServiceName name, String group) {
        super(name);
        this.group = group;
    }

    @Override
    public AutoCloseableCommandDispatcherFactory get() {
        return new ManagedCommandDispatcherFactory(new ChannelCommandDispatcherFactory(this));
    }

    @Override
    public ServiceConfigurator configure(CapabilityServiceSupport support) {
        this.channel = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL.getServiceName(support, this.group));
        this.channelFactory = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(support, this.group));
        this.module = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_MODULE.getServiceName(support, this.group));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        this.loader = builder.requires(Services.JBOSS_SERVICE_MODULE_LOADER);
        Consumer<CommandDispatcherFactory> factory = new CompositeDependency(this.channel, this.channelFactory, this.module).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(factory, Functions.identity(), this, Consumers.close());
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    public ChannelCommandDispatcherFactoryServiceConfigurator timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
        return this;
    }

    @Override
    public ByteBufferMarshaller apply(ClassLoader loader) {
        ModuleLoader moduleLoader = this.loader.get();
        try {
            return new ProtoStreamByteBufferMarshaller(new SerializationContextBuilder(new ModuleClassLoaderMarshaller(moduleLoader)).require(loader).build());
        } catch (NoSuchElementException e) {
            return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, new AbstractMap.SimpleImmutableEntry<>(ModularClassResolver.getInstance(moduleLoader), loader)), loader);
        }
    }

    @Override
    public JChannel getChannel() {
        return this.channel.get();
    }

    @Override
    public ByteBufferMarshaller getMarshaller() {
        return new ProtoStreamByteBufferMarshaller(new SerializationContextBuilder(new ModuleClassLoaderMarshaller(this.loader.get())).load(this.module.get().getClassLoader()).build());
    }

    @Override
    public Duration getTimeout() {
        return this.timeout;
    }

    @Override
    public ChannelFactory getChannelFactory() {
        return this.channelFactory.get();
    }
}
