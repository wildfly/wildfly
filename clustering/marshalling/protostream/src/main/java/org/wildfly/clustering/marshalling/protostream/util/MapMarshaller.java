/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ScalarMarshaller;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Generic marshaller for {@link Map} implementations.
 * @author Paul Ferraro
 */
public class MapMarshaller<T extends Map<Object, Object>, C, CC> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final Function<CC, T> factory;
    private final Function<Map.Entry<C, Integer>, CC> constructorContext;
    private final Function<T, C> context;
    private final ScalarMarshaller<C> contextMarshaller;

    public MapMarshaller(Class<T> targetClass, Function<CC, T> factory, Function<Map.Entry<C, Integer>, CC> constructorContext, Function<T, C> context, ScalarMarshaller<C> contextMarshaller) {
        this.targetClass = targetClass;
        this.factory = factory;
        this.constructorContext = constructorContext;
        this.context = context;
        this.contextMarshaller = contextMarshaller;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        C mapContext = this.contextMarshaller.readFrom(context, reader);
        int size = reader.readUInt32();
        CC constructorContext = this.constructorContext.apply(new AbstractMap.SimpleImmutableEntry<>(mapContext, size));
        T map = this.factory.apply(constructorContext);
        for (int i = 0; i < size; ++i) {
            map.put(ObjectMarshaller.INSTANCE.readFrom(context, reader), ObjectMarshaller.INSTANCE.readFrom(context, reader));
        }
        return map;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T map) throws IOException {
        C mapContext = this.context.apply(map);
        this.contextMarshaller.writeTo(context, writer, mapContext);
        writer.writeUInt32NoTag(map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, entry.getKey());
            ObjectMarshaller.INSTANCE.writeTo(context, writer, entry.getValue());
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T map) {
        C mapContext = this.context.apply(map);
        OptionalInt size = this.contextMarshaller.size(context, mapContext);
        if (size.isPresent()) {
            size = OptionalInt.of(size.getAsInt() + CodedOutputStream.computeUInt32SizeNoTag(map.size()));
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                OptionalInt keySize = ObjectMarshaller.INSTANCE.size(context, entry.getKey());
                OptionalInt valueSize = ObjectMarshaller.INSTANCE.size(context, entry.getValue());
                size = size.isPresent() && keySize.isPresent() && valueSize.isPresent() ? OptionalInt.of(size.getAsInt() + keySize.getAsInt() + valueSize.getAsInt()) : OptionalInt.empty();
            }
        }
        return size;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
