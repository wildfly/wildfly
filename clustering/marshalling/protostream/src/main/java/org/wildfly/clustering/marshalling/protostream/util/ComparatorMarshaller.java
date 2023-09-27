/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Comparator;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for the fields of a {@link Comparator}.
 * @author Paul Ferraro
 */
public enum ComparatorMarshaller implements FieldSetMarshaller<Comparator<?>, Comparator<?>> {
    INSTANCE;

    private static final int REVERSE_INDEX = 0;
    private static final int COMPARATOR_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public Comparator<?> getBuilder() {
        return Comparator.naturalOrder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Comparator<?> readField(ProtoStreamReader reader, int index, Comparator<?> comparator) throws IOException {
        switch (index) {
            case REVERSE_INDEX:
                return reader.readBool() ? Comparator.reverseOrder() : Comparator.naturalOrder();
            case COMPARATOR_INDEX:
                return reader.readAny(Comparator.class);
            default:
                throw new IllegalArgumentException(Integer.toString(index));
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, Comparator<?> comparator) throws IOException {
        boolean natural = comparator == Comparator.naturalOrder();
        boolean reverse = comparator == Comparator.reverseOrder();
        if (natural || reverse) {
            writer.writeBool(startIndex + REVERSE_INDEX, reverse);
        } else {
            writer.writeAny(startIndex + COMPARATOR_INDEX, comparator);
        }
    }
}
