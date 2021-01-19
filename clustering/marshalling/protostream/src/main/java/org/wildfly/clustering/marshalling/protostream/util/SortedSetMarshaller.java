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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Comparator;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public class SortedSetMarshaller<T extends SortedSet<Object>> extends AbstractCollectionMarshaller<T> {
    @SuppressWarnings("unchecked")
    private static final Comparator<Object> DEFAULT_COMPARATOR = (Comparator<Object>) (Comparator<?>) Comparator.naturalOrder();

    private static final int COMPARATOR_INDEX = 2;

    private final Function<Comparator<? super Object>, T> factory;

    @SuppressWarnings("unchecked")
    public SortedSetMarshaller(Function<Comparator<? super Object>, T> factory) {
        super((Class<T>) factory.apply((Comparator<Object>) (Comparator<?>) Comparator.naturalOrder()).getClass());
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        T set = this.factory.apply(DEFAULT_COMPARATOR);
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index == 1) {
                set.add(ProtoStreamMarshaller.read(context, reader.readByteBuffer(), Any.class).get());
            } else if ((index >= COMPARATOR_INDEX) && (index < COMPARATOR_INDEX + ComparatorFieldMarshaller.INSTANCE.getFields())) {
                T existing = set;
                Comparator<Object> comparator = (Comparator<Object>) ComparatorFieldMarshaller.INSTANCE.readField(context, reader, index - COMPARATOR_INDEX, DEFAULT_COMPARATOR);
                set = this.factory.apply(comparator);
                set.addAll(existing);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return set;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T set) throws IOException {
        super.writeTo(context, writer, set);
        Comparator<?> comparator = set.comparator();
        if (comparator != DEFAULT_COMPARATOR) {
            ComparatorFieldMarshaller.INSTANCE.writeFields(context, writer, COMPARATOR_INDEX, set.comparator());
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T set) {
        int size = 0;
        OptionalInt elementsSize = super.size(context, set);
        if (elementsSize.isPresent()) {
            size += elementsSize.getAsInt();
        } else {
            return elementsSize;
        }
        Comparator<?> comparator = set.comparator();
        if (comparator != DEFAULT_COMPARATOR) {
            OptionalInt comparatorSize = ComparatorFieldMarshaller.INSTANCE.size(context, COMPARATOR_INDEX, comparator);
            if (comparatorSize.isPresent()) {
                size += comparatorSize.getAsInt();
            } else {
                return comparatorSize;
            }
        }
        return OptionalInt.of(size);
    }
}
