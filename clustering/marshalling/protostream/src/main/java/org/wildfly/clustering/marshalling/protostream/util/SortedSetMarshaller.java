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
import java.util.SortedSet;
import java.util.function.Function;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link SortedSet}.
 * @author Paul Ferraro
 * @param <T> the set type of this marshaller
 */
public class SortedSetMarshaller<T extends SortedSet<Object>> extends AbstractCollectionMarshaller<T> {

    private static final int COMPARATOR_INDEX = 2;

    private final Function<Comparator<? super Object>, T> factory;

    @SuppressWarnings("unchecked")
    public SortedSetMarshaller(Function<Comparator<? super Object>, T> factory) {
        super((Class<T>) factory.apply((Comparator<Object>) ComparatorMarshaller.INSTANCE.getBuilder()).getClass());
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        Comparator<Object> comparator = (Comparator<Object>) ComparatorMarshaller.INSTANCE.getBuilder();
        T set = this.factory.apply(comparator);
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index == 1) {
                set.add(reader.readObject(Any.class).get());
            } else if ((index >= COMPARATOR_INDEX) && (index < COMPARATOR_INDEX + ComparatorMarshaller.INSTANCE.getFields())) {
                T existing = set;
                comparator = (Comparator<Object>) ComparatorMarshaller.INSTANCE.readField(reader, index - COMPARATOR_INDEX, comparator);
                set = this.factory.apply(comparator);
                set.addAll(existing);
            } else {
                reading = reader.ignoreField(tag);
            }
        }
        return set;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T set) throws IOException {
        super.writeTo(writer, set);
        Comparator<?> comparator = set.comparator();
        if (comparator != ComparatorMarshaller.INSTANCE.getBuilder()) {
            ComparatorMarshaller.INSTANCE.writeFields(writer, COMPARATOR_INDEX, set.comparator());
        }
    }
}
