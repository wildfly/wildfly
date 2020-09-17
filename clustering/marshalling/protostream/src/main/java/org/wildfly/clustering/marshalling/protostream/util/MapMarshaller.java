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
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public abstract class MapMarshaller<T extends Map<Object, Object>, C> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final Function<C, T> factory;

    @SuppressWarnings("unchecked")
    public MapMarshaller(Class<?> targetClass, Function<C, T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        C ctx = this.readContext(context, reader);
        T map = this.factory.apply(ctx);
        int size = reader.readUInt32();
        for (int i = 0; i < size; ++i) {
            map.put(ObjectMarshaller.INSTANCE.readFrom(context, reader), ObjectMarshaller.INSTANCE.readFrom(context, reader));
        }
        return map;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T map) throws IOException {
        this.writeContext(context, writer, map);
        writer.writeUInt32NoTag(map.size());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, entry.getKey());
            ObjectMarshaller.INSTANCE.writeTo(context, writer, entry.getValue());
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T map) {
        OptionalInt size = this.sizeContext(context, map);
        if (size.isPresent()) {
            size = OptionalInt.of(size.getAsInt() + Predictable.unsignedIntSize(map.size()));
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

    /**
     * Reads the map context from the specified input stream.
     * @param input an input stream
     * @return the map constructor context
     * @throws IOException if the constructor context cannot be read from the stream
     * @throws ClassNotFoundException if a class could not be found
     */
    protected abstract C readContext(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException;

    /**
     * Writes the context of the specified map to the specified output stream.
     * @param output an output stream
     * @param map the target map
     * @throws IOException if the constructor context cannot be written to the stream
     */
    protected abstract void writeContext(ImmutableSerializationContext context, RawProtoStreamWriter writer, T map) throws IOException;


    /**
     * Returns the size of context for the specified map.
     * @param output an output stream
     * @param map the target map
     * @throws IOException if the constructor context cannot be written to the stream
     */
    protected abstract OptionalInt sizeContext(ImmutableSerializationContext context, T map);
}
