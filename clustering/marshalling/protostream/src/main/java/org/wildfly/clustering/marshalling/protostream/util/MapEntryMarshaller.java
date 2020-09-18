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
import java.util.function.BiFunction;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public class MapEntryMarshaller<T extends Map.Entry<Object, Object>> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final BiFunction<Object, Object, T> factory;

    @SuppressWarnings("unchecked")
    public MapEntryMarshaller(Class<?> targetClass, BiFunction<Object, Object, T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Object key = ObjectMarshaller.INSTANCE.readFrom(context, reader);
        Object value = ObjectMarshaller.INSTANCE.readFrom(context, reader);
        return this.factory.apply(key, value);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T entry) throws IOException {
        ObjectMarshaller.INSTANCE.writeTo(context, writer, entry.getKey());
        ObjectMarshaller.INSTANCE.writeTo(context, writer, entry.getValue());
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T entry) {
        OptionalInt size = ObjectMarshaller.INSTANCE.size(context, entry.getKey());
        if (size.isPresent()) {
            OptionalInt valueSize = ObjectMarshaller.INSTANCE.size(context, entry.getValue());
            size = valueSize.isPresent() ? OptionalInt.of(size.getAsInt() + valueSize.getAsInt()) : OptionalInt.empty();
        }
        return size;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
