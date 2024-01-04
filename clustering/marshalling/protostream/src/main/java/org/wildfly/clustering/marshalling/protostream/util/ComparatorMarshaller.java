/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Comparator;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for the fields of a {@link Comparator}.
 * @author Paul Ferraro
 */
public enum ComparatorMarshaller implements FieldSetMarshaller.Simple<Comparator<?>> {
    INSTANCE;

    private static final int REVERSE_INDEX = 0;
    private static final int COMPARATOR_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public Comparator<?> createInitialValue() {
        return Comparator.naturalOrder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Comparator<?> readFrom(ProtoStreamReader reader, int index, WireType type, Comparator<?> comparator) throws IOException {
        switch (index) {
            case REVERSE_INDEX:
                return reader.readBool() ? Comparator.reverseOrder() : Comparator.naturalOrder();
            case COMPARATOR_INDEX:
                return reader.readAny(Comparator.class);
            default:
                reader.skipField(type);
                throw new IllegalArgumentException(Integer.toString(index));
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Comparator<?> comparator) throws IOException {
        boolean natural = comparator == Comparator.naturalOrder();
        boolean reverse = comparator == Comparator.reverseOrder();
        if (natural || reverse) {
            writer.writeBool(REVERSE_INDEX, reverse);
        } else {
            writer.writeAny(COMPARATOR_INDEX, comparator);
        }
    }
}
