/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.dispatcher;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.server.Services;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleLoader;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.protostream.DefaultSerializationContext;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.server.dispatcher.Command;
import org.wildfly.clustering.server.jgroups.dispatcher.ChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.jgroups.dispatcher.JChannelCommandDispatcherFactory;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Builds a channel-based {@link org.wildfly.clustering.dispatcher.CommandDispatcherFactory} service.
 * @author Paul Ferraro
 */
public enum ChannelCommandDispatcherFactoryServiceInstallerFactory implements Function<String, ServiceInstaller> {
    INSTANCE;

    @Override
    public ServiceInstaller apply(String name) {
        ServiceDependency<ForkChannelFactory> channelFactory = ServiceDependency.on(ChannelFactory.SERVICE_DESCRIPTOR, name).map(ForkChannelFactory.class::cast);
        ServiceDependency<ModuleLoader> moduleLoader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        Function<ClassLoader, ByteBufferMarshaller> marshallerFactory = new Function<>() {
            @Override
            public ByteBufferMarshaller apply(ClassLoader loader) {
                // Use protostream if any Command marshallers exist
                AtomicBoolean supportsProtoStream = new AtomicBoolean(false);
                ImmutableSerializationContext context = SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(moduleLoader.get()), ctx -> new DefaultSerializationContext(ctx) {
                    @Override
                    public void registerMarshaller(ProtoStreamMarshaller<?> marshaller) {
                        supportsProtoStream.compareAndSet(false, Command.class.isAssignableFrom(marshaller.getJavaClass()));
                        super.registerMarshaller(marshaller);
                    }
                }).load(loader).build();
                if (supportsProtoStream.get()) {
                    return new ProtoStreamByteBufferMarshaller(context);
                }
                MarshallingConfigurationBuilder builder = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(moduleLoader.get())).load(loader);
                @SuppressWarnings("deprecation")
                MarshallingConfiguration configuration = new org.wildfly.clustering.marshalling.jboss.externalizer.LegacyExternalizerConfiguratorFactory(loader).apply(builder).build();
                return new JBossByteBufferMarshaller(configuration, loader);
            }
        };
        JChannelCommandDispatcherFactory.Configuration configuration = new JChannelCommandDispatcherFactory.Configuration() {
            @Override
            public JChannel getChannel() {
                return channelFactory.get().getConfiguration().getChannel();
            }

            @Override
            public ByteBufferMarshaller getMarshaller() {
                ClassLoader loader = channelFactory.get().getConfiguration().getChannelConfiguration().getModule().getClassLoader();
                return new ProtoStreamByteBufferMarshaller(SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(moduleLoader.get())).load(loader).build());
            }

            @Override
            public Predicate<Message> getUnknownForkPredicate() {
                return channelFactory.get()::isUnknownForkResponse;
            }

            @Override
            public Function<ClassLoader, ByteBufferMarshaller> getMarshallerFactory() {
                return marshallerFactory;
            }
        };
        Supplier<ChannelCommandDispatcherFactory> factory = new Supplier<>() {
            @Override
            public ChannelCommandDispatcherFactory get() {
                return new JChannelCommandDispatcherFactory(configuration);
            }
        };
        return ServiceInstaller.builder(factory)
                .provides(ServiceNameFactory.resolveServiceName(ClusteringServiceDescriptor.COMMAND_DISPATCHER_FACTORY, name))
                .requires(List.of(channelFactory, moduleLoader))
                .onStop(Consumer.close())
                .blocking()
                .startWhen(StartWhen.AVAILABLE)
                .build();
    }
}
