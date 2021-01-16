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
import java.util.Collection;
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
 * ProtoStream optimized collection marshaller.
 * @author Paul Ferraro
 */
public class CollectionMarshaller<T extends Collection<Object>, C, CC> implements ProtoStreamMarshaller<T> {

    private final Class<T> targetClass;
    private final Function<CC, T> factory;
    private final Function<Map.Entry<C, Integer>, CC> constructorContext;
    private final Function<T, C> context;
    private final ProtoStreamMarshaller<C> contextMarshaller;

    public CollectionMarshaller(Class<T> targetClass, Function<CC, T> factory, Function<Map.Entry<C, Integer>, CC> constructorContext, Function<T, C> context, ProtoStreamMarshaller<C> contextMarshaller) {
        this.targetClass = targetClass;
        this.factory = factory;
        this.constructorContext = constructorContext;
        this.context = context;
        this.contextMarshaller = contextMarshaller;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        C collectionContext = this.contextMarshaller.readFrom(context, reader);
        int size = reader.readUInt32();
        CC constructorContext = this.constructorContext.apply(new AbstractMap.SimpleImmutableEntry<>(collectionContext, size));
        T collection = this.factory.apply(constructorContext);
        for (int i = 0; i < size; ++i) {
            collection.add(ObjectMarshaller.INSTANCE.readFrom(context, reader));
        }
        return collection;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T collection) throws IOException {
        C collectionContext = this.context.apply(collection);
        this.contextMarshaller.writeTo(context, writer, collectionContext);
        writer.writeUInt32NoTag(collection.size());
        for (Object element : collection) {
            ObjectMarshaller.INSTANCE.writeTo(context, writer, element);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T collection) {
        C collectionContext = this.context.apply(collection);
        OptionalInt size = this.contextMarshaller.size(context, collectionContext);
        if (size.isPresent()) {
            size = OptionalInt.of(size.getAsInt() + Predictable.unsignedIntSize(collection.size()));
            for (Object element : collection) {
                OptionalInt elementSize = ObjectMarshaller.INSTANCE.size(context, element);
                size = size.isPresent() && elementSize.isPresent() ? OptionalInt.of(size.getAsInt() + elementSize.getAsInt()) : OptionalInt.empty();
            }
        }
        return size;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
