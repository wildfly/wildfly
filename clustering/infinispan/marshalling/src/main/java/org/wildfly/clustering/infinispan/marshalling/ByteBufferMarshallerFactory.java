/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
