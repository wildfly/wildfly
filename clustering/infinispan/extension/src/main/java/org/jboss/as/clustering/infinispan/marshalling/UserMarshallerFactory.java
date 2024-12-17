/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.marshalling;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.BufferSizePredictor;
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
        private final Supplier<SerializationContextBuilder<org.infinispan.protostream.SerializationContextInitializer>> builderFactory = () -> SerializationContextBuilder.newInstance().register(new org.infinispan.protostream.SerializationContextInitializer() {
            @Override
            public String getProtoFileName() {
                return null;
            }

            @Override
            public String getProtoFile() throws UncheckedIOException {
                return null;
            }

            @Override
            public void registerSchema(SerializationContext context) {
                org.infinispan.query.remote.client.impl.MarshallerRegistration.init(context);
            }

            @Override
            public void registerMarshallers(SerializationContext context) {
            }
        });

        @Override
        public ByteBufferMarshaller createByteBufferMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
            ImmutableSerializationContext context = this.build(this.builderFactory, loaders);
            return new WrappedMessageByteBufferMarshaller(context);
        }

        @Override
        public Marshaller createUserMarshaller(ModuleLoader moduleLoader, List<ClassLoader> loaders) {
            ImmutableSerializationContext context = this.build(this.builderFactory, loaders);
            ByteBufferMarshaller marshaller = new WrappedMessageByteBufferMarshaller(context);
            // N.B. RemoteQueryFactory is hard-wired to use ProtoStreamMarshaller
            BufferSizePredictor predictor = new BufferSizePredictor() {
                @Override
                public int nextSize(Object object) {
                    return marshaller.size(object).orElse(1);
                }

                @Override
                public void recordSize(int previousSize) {
                    // Do nothing
                }
            };
            return new ProtoStreamMarshaller((SerializationContext) context) {
                @Override
                public Object objectFromByteBuffer(byte[] buffer, int offset, int length) throws IOException, ClassNotFoundException {
                    return marshaller.read(java.nio.ByteBuffer.wrap(buffer, offset, length));
                }

                @Override
                public ByteBuffer objectToBuffer(Object object) throws IOException {
                    return ByteBufferImpl.create(marshaller.write(object));
                }

                @Override
                protected ByteBuffer objectToBuffer(Object object, int estimatedSize) throws IOException {
                    return this.objectToBuffer(object);
                }

                @Override
                public byte[] objectToByteBuffer(Object object) throws IOException {
                    return this.objectToBuffer(object).trim();
                }

                @Override
                public byte[] objectToByteBuffer(Object object, int estimatedSize) throws IOException {
                    return this.objectToByteBuffer(object);
                }

                @Override
                public Object objectFromInputStream(InputStream input) throws IOException {
                    return marshaller.readFrom(input);
                }

                @Override
                public BufferSizePredictor getBufferSizePredictor(Object object) {
                    return predictor;
                }
            };
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
