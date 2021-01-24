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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.FieldMarshaller;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public enum ComparatorFieldMarshaller implements FieldMarshaller<Comparator<?>, Comparator<?>> {
    INSTANCE;

    private static final int REVERSE_INDEX = 0;
    private static final int COMPARATOR_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Comparator<?> readField(ImmutableSerializationContext context, RawProtoStreamReader reader, int index, Comparator<?> comparator) throws IOException {
        switch (index) {
            case REVERSE_INDEX:
                return reader.readBool() ? Comparator.reverseOrder() : Comparator.naturalOrder();
            case COMPARATOR_INDEX:
                return (Comparator<?>) ProtoStreamMarshaller.read(context, reader.readByteBuffer(), Any.class).get();
            default:
                throw new IllegalArgumentException(Integer.toString(index));
        }
    }

    @Override
    public void writeFields(ImmutableSerializationContext context, RawProtoStreamWriter writer, int startIndex, Comparator<?> comparator) throws IOException {
        boolean natural = comparator == Comparator.naturalOrder();
        boolean reverse = comparator == Comparator.reverseOrder();
        if (natural || reverse) {
            writer.writeBool(startIndex + REVERSE_INDEX, reverse);
        } else {
            writer.writeBytes(startIndex + COMPARATOR_INDEX, ProtoStreamMarshaller.write(context, new Any(comparator)));
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, int startIndex, Comparator<?> comparator) {
        boolean natural = comparator == Comparator.naturalOrder();
        boolean reverse = comparator == Comparator.reverseOrder();
        return (natural || reverse) ? OptionalInt.of(CodedOutputStream.computeBoolSize(startIndex + REVERSE_INDEX, reverse)) : Predictable.computeSize(context, startIndex + COMPARATOR_INDEX, new Any(comparator));
    }
}
