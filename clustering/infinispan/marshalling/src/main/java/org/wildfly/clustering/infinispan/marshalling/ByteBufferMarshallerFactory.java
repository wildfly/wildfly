/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.commons.dataconversion.MediaType;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshalling.protostream.WrappedMessageByteBufferMarshaller;
import org.wildfly.clustering.infinispan.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.DynamicExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ModuleClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * Creates a {@link ByteBufferMarshaller} suitable for the given {@link MediaType}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshallerFactory implements Function<ClassLoader, ByteBufferMarshaller> {

    enum MarshallingVersion implements Function<Map.Entry<ClassResolver, ClassLoader>, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(Map.Entry<ClassResolver, ClassLoader> entry) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                ClassLoader loader = entry.getValue();
                config.setClassResolver(entry.getKey());
                config.setClassTable(new DynamicClassTable(loader));
                config.setObjectTable(new DynamicExternalizerObjectTable(loader));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_1;
    }

    private final MediaType type;
    private final ModuleLoader loader;

    public ByteBufferMarshallerFactory(MediaType type, ModuleLoader loader) {
        this.type = type;
        this.loader = loader;
    }

    @Override
    public ByteBufferMarshaller apply(ClassLoader loader) {
        switch (this.type.toString()) {
            case ProtoStreamMarshaller.MEDIA_TYPE_NAME:
                return new ProtoStreamByteBufferMarshaller(new SerializationContextBuilder(new ModuleClassLoaderMarshaller(this.loader)).load(loader).build());
            case MediaType.APPLICATION_JBOSS_MARSHALLING_TYPE:
                return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, new AbstractMap.SimpleImmutableEntry<>(ModularClassResolver.getInstance(this.loader), loader)), loader);
            case MediaType.APPLICATION_PROTOSTREAM_TYPE:
                return new WrappedMessageByteBufferMarshaller(loader);
            default:
                throw new IllegalArgumentException(this.type.toString());
        }
    }
}
