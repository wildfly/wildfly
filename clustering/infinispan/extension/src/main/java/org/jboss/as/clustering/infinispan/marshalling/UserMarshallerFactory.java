/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.marshalling;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.SerializationContext;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.cache.infinispan.marshalling.MediaTypes;
import org.wildfly.clustering.cache.infinispan.marshalling.UserMarshaller;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.marshalling.MarshallerConfigurationBuilder;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationBuilder;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.WrappedMessageByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.modules.ModuleClassLoaderMarshaller;

/**
 * @author Paul Ferraro
 */
public enum UserMarshallerFactory implements MarshallerFactory {

    DEFAULT(MediaTypes.INFINISPAN_PROTOSTREAM) {
        private final Supplier<SerializationContextBuilder<org.infinispan.protostream.SerializationContextInitializer>> builderFactory = () -> SerializationContextBuilder.newInstance().register(org.infinispan.query.remote.client.impl.GlobalContextInitializer.INSTANCE);

        @Override
        public ByteBufferMarshaller createByteBufferMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
            ImmutableSerializationContext context = this.build(this.builderFactory, loaders);
            return new WrappedMessageByteBufferMarshaller(context);
        }

        @Override
        public Marshaller createUserMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
            ImmutableSerializationContext context = this.build(this.builderFactory, loaders);
            // N.B. RemoteQueryFactory requires a ProtoStreamMarshaller instance via casting
            return new ProtoStreamMarshaller((SerializationContext) context);
        }
    },
    JBOSS(MediaTypes.JBOSS_MARSHALLING) {
        @Override
        public ByteBufferMarshaller createByteBufferMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
            MarshallingConfigurationBuilder builder = MarshallingConfigurationBuilder.newInstance(ModularClassResolver.getInstance(moduleLoader));
            loaders.forEach(builder::load);
            ClassLoader classLoader = (loaders.size() > 1) ? new AggregatedClassLoader(loaders) : loaders.get(0);
            return new JBossByteBufferMarshaller(builder.build(), classLoader);
        }
    },
    PROTOSTREAM(MediaTypes.WILDFLY_PROTOSTREAM) {
        @Override
        public ByteBufferMarshaller createByteBufferMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
            SerializationContextBuilder<SerializationContextInitializer> builder = SerializationContextBuilder.newInstance(new ModuleClassLoaderMarshaller(moduleLoader));
            loaders.forEach(builder::load);
            return new ProtoStreamByteBufferMarshaller(builder.build());
        }
    },
    ;
    private final MediaType type;

    UserMarshallerFactory(Supplier<MediaType> type) {
        this.type = type.get();
    }

    @Override
    public Marshaller createUserMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
        return new UserMarshaller(this.type, this.createByteBufferMarshaller(moduleLoader, loaders));
    }

    <C, E, B extends MarshallerConfigurationBuilder<C, E, B>> C build(Supplier<B> builderFactory, List<ClassLoader> loaders) {
        B builder = builderFactory.get();
        loaders.forEach(builder::load);
        return builder.build();
    }

    public static MarshallerFactory forMediaType(MediaType type) {
        for (UserMarshallerFactory factory : EnumSet.allOf(UserMarshallerFactory.class)) {
            if (factory.type.equals(type)) return factory;
        }
        throw new IllegalArgumentException(type.toString());
    }
}
