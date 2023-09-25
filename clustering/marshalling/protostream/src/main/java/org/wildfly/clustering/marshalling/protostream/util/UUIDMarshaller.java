/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.UUID;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link UUID} using fixed size longs.
 * @author Paul Ferraro
 */
public enum UUIDMarshaller implements FieldSetMarshaller.Supplied<UUID, UUIDBuilder> {
    INSTANCE;

    private static final long DEFAULT_SIGNIFICANT_BITS = 0;

    private static final int MOST_SIGNIFICANT_BITS_INDEX = 0;
    private static final int LEAST_SIGNIFICANT_BITS_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public UUIDBuilder createInitialValue() {
        return new DefaultUUIDBuilder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public UUIDBuilder readFrom(ProtoStreamReader reader, int index, WireType type, UUIDBuilder builder) throws IOException {
        switch (index) {
            case MOST_SIGNIFICANT_BITS_INDEX:
                return builder.setMostSignificantBits(reader.readSFixed64());
            case LEAST_SIGNIFICANT_BITS_INDEX:
                return builder.setLeastSignificantBits(reader.readSFixed64());
            default:
                reader.skipField(type);
                return builder;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, UUID uuid) throws IOException {
        long mostSignificantBits = uuid.getMostSignificantBits();
        if (mostSignificantBits != DEFAULT_SIGNIFICANT_BITS) {
            writer.writeSFixed64(MOST_SIGNIFICANT_BITS_INDEX, mostSignificantBits);
        }
        long leastSignificantBits = uuid.getLeastSignificantBits();
        if (leastSignificantBits != DEFAULT_SIGNIFICANT_BITS) {
            writer.writeSFixed64(LEAST_SIGNIFICANT_BITS_INDEX, leastSignificantBits);
        }
    }

    static class DefaultUUIDBuilder implements UUIDBuilder {
        private long mostSignificantBits = DEFAULT_SIGNIFICANT_BITS;
        private long leastSignificantBits = DEFAULT_SIGNIFICANT_BITS;

        @Override
        public UUIDBuilder setMostSignificantBits(long bits) {
            this.mostSignificantBits = bits;
            return this;
        }

        @Override
        public UUIDBuilder setLeastSignificantBits(long bits) {
            this.leastSignificantBits = bits;
            return this;
        }

        @Override
        public UUID get() {
            return new UUID(this.mostSignificantBits, this.leastSignificantBits);
        }
    }
}
