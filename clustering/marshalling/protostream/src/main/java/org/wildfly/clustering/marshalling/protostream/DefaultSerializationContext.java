/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.EnumDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.GenericDescriptor;
import org.infinispan.protostream.impl.SerializationContextImpl;

/**
 * Decorates {@link SerializationContextImpl}, ensuring that all registered marshallers implement {@link ProtoStreamMarshaller}.
 * We have to use the decorator pattern since SerializationContextImpl is final.
 * @author Paul Ferraro
 */
public class DefaultSerializationContext implements SerializationContext, Supplier<ImmutableSerializationContext> {

    private final SerializationContext context = new SerializationContextImpl(Configuration.builder().build());

    @Override
    public ImmutableSerializationContext get() {
        return this.context;
    }

    @Override
    public Configuration getConfiguration() {
        return this.context.getConfiguration();
    }

    @Override
    public Map<String, FileDescriptor> getFileDescriptors() {
        return this.context.getFileDescriptors();
    }

    @Override
    public Map<String, GenericDescriptor> getGenericDescriptors() {
        return this.context.getGenericDescriptors();
    }

    @Override
    public Descriptor getMessageDescriptor(String fullTypeName) {
        return this.context.getMessageDescriptor(fullTypeName);
    }

    @Override
    public EnumDescriptor getEnumDescriptor(String fullTypeName) {
        return this.context.getEnumDescriptor(fullTypeName);
    }

    @Override
    public boolean canMarshall(Class<?> javaClass) {
        return this.context.canMarshall(javaClass);
    }

    @Override
    public boolean canMarshall(String fullTypeName) {
        return this.context.canMarshall(fullTypeName);
    }

    @Override
    public boolean canMarshall(Object object) {
        return this.context.canMarshall(object);
    }

    @Override
    public <T> BaseMarshaller<T> getMarshaller(T object) {
        return this.context.getMarshaller(object);
    }

    @Override
    public <T> BaseMarshaller<T> getMarshaller(String fullTypeName) {
        return this.context.getMarshaller(fullTypeName);
    }

    @Override
    public <T> BaseMarshaller<T> getMarshaller(Class<T> clazz) {
        return this.context.getMarshaller(clazz);
    }

    @Deprecated
    @Override
    public String getTypeNameById(Integer typeId) {
        return this.context.getTypeNameById(typeId);
    }

    @Deprecated
    @Override
    public Integer getTypeIdByName(String fullTypeName) {
        return this.context.getTypeIdByName(fullTypeName);
    }

    @Override
    public GenericDescriptor getDescriptorByTypeId(Integer typeId) {
        return this.context.getDescriptorByTypeId(typeId);
    }

    @Override
    public GenericDescriptor getDescriptorByName(String fullTypeName) {
        return this.context.getDescriptorByName(fullTypeName);
    }

    @Override
    public void registerProtoFiles(FileDescriptorSource source) throws DescriptorParserException {
        this.context.registerProtoFiles(source);
    }

    @Override
    public void unregisterProtoFile(String fileName) {
        this.context.unregisterProtoFile(fileName);
    }

    @Override
    public void unregisterProtoFiles(Set<String> fileNames) {
        this.context.unregisterProtoFiles(fileNames);
    }

    @Override
    public void registerMarshaller(BaseMarshaller<?> marshaller) {
        this.context.registerMarshaller(this.adapt(marshaller));
    }

    @Override
    public void unregisterMarshaller(BaseMarshaller<?> marshaller) {
        this.context.unregisterMarshaller(marshaller);
    }

    @Deprecated
    @Override
    public void registerMarshallerProvider(MarshallerProvider provider) {
        this.context.registerMarshallerProvider(this.adapt(provider));
    }

    @Deprecated
    @Override
    public void unregisterMarshallerProvider(MarshallerProvider provider) {
        this.context.unregisterMarshallerProvider(provider);
    }

    @Override
    public void registerMarshallerProvider(InstanceMarshallerProvider<?> provider) {
        this.context.registerMarshallerProvider(this.adapt(provider));
    }

    @Override
    public void unregisterMarshallerProvider(InstanceMarshallerProvider<?> provider) {
        this.context.unregisterMarshallerProvider(provider);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    <T> BaseMarshaller<T> adapt(BaseMarshaller<T> marshaller) {
        if (marshaller instanceof ProtoStreamMarshaller) {
            return marshaller;
        }
        if (marshaller instanceof ProtobufTagMarshaller) {
            return new ProtoStreamMarshallerAdapter<>((ProtobufTagMarshaller<T>) marshaller);
        }
        if (marshaller instanceof org.infinispan.protostream.EnumMarshaller) {
            return new EnumMarshallerAdapter<>((org.infinispan.protostream.EnumMarshaller) marshaller);
        }
        throw new IllegalArgumentException(marshaller.getTypeName());
    }

    private <T> InstanceMarshallerProvider<T> adapt(InstanceMarshallerProvider<T> provider) {
        return new InstanceMarshallerProvider<T>() {
            @Override
            public Class<T> getJavaClass() {
                return provider.getJavaClass();
            }

            @Override
            public Set<String> getTypeNames() {
                return provider.getTypeNames();
            }

            @Override
            public String getTypeName(T instance) {
                return provider.getTypeName(instance);
            }

            @Override
            public BaseMarshaller<T> getMarshaller(T instance) {
                BaseMarshaller<T> marshaller = provider.getMarshaller(instance);
                return (marshaller != null) ? DefaultSerializationContext.this.adapt(marshaller) : null;
            }

            @Override
            public BaseMarshaller<T> getMarshaller(String typeName) {
                BaseMarshaller<T> marshaller = provider.getMarshaller(typeName);
                return (marshaller != null) ? DefaultSerializationContext.this.adapt(marshaller) : null;
            }
        };
    }

    @Deprecated
    private MarshallerProvider adapt(MarshallerProvider provider) {
        return new MarshallerProvider() {
            @Override
            public BaseMarshaller<?> getMarshaller(String typeName) {
                BaseMarshaller<?> marshaller = provider.getMarshaller(typeName);
                return (marshaller != null) ? DefaultSerializationContext.this.adapt(marshaller) : null;
            }

            @Override
            public BaseMarshaller<?> getMarshaller(Class<?> javaClass) {
                BaseMarshaller<?> marshaller = provider.getMarshaller(javaClass);
                return (marshaller != null) ? DefaultSerializationContext.this.adapt(marshaller) : null;
            }
        };
    }
}
