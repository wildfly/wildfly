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
import java.util.UUID;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link UUID} using fixed size longs.
 * @author Paul Ferraro
 */
public enum UUIDMarshaller implements FieldSetMarshaller<UUID, UUIDBuilder> {
    INSTANCE;

    private static final long DEFAULT_SIGNIFICANT_BITS = 0;

    private static final int MOST_SIGNIFICANT_BITS_INDEX = 0;
    private static final int LEAST_SIGNIFICANT_BITS_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public UUIDBuilder getBuilder() {
        return new DefaultUUIDBuilder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public UUIDBuilder readField(ProtoStreamReader reader, int index, UUIDBuilder builder) throws IOException {
        switch (index) {
            case MOST_SIGNIFICANT_BITS_INDEX:
                return builder.setMostSignificantBits(reader.readSFixed64());
            case LEAST_SIGNIFICANT_BITS_INDEX:
                return builder.setLeastSignificantBits(reader.readSFixed64());
            default:
                return builder;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, UUID uuid) throws IOException {
        long mostSignificantBits = uuid.getMostSignificantBits();
        if (mostSignificantBits != DEFAULT_SIGNIFICANT_BITS) {
            writer.writeSFixed64(startIndex + MOST_SIGNIFICANT_BITS_INDEX, mostSignificantBits);
        }
        long leastSignificantBits = uuid.getLeastSignificantBits();
        if (leastSignificantBits != DEFAULT_SIGNIFICANT_BITS) {
            writer.writeSFixed64(startIndex + LEAST_SIGNIFICANT_BITS_INDEX, leastSignificantBits);
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
        public UUID build() {
            return new UUID(this.mostSignificantBits, this.leastSignificantBits);
        }
    }
}
