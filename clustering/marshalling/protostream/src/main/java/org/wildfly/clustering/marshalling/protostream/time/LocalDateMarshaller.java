/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalDate;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshals a {@link LocalDate} as an epoch day.
 * @author Paul Ferraro
 */
public enum LocalDateMarshaller implements FieldSetMarshaller<LocalDate, LocalDate> {
    INSTANCE;

    private static final int POST_EPOCH_DAY = 0;
    private static final int PRE_EPOCH_DAY = 1;
    private static final int FIELDS = 2;

    private static final LocalDate EPOCH = LocalDate.ofEpochDay(0);

    @Override
    public LocalDate getBuilder() {
        return EPOCH;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public LocalDate readField(ProtoStreamReader reader, int index, LocalDate date) throws IOException {
        switch (index) {
            case POST_EPOCH_DAY:
                return LocalDate.ofEpochDay(reader.readUInt64());
            case PRE_EPOCH_DAY:
                return LocalDate.ofEpochDay(0L - reader.readUInt64());
            default:
                return date;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, LocalDate date) throws IOException {
        long epochDay = date.toEpochDay();
        if (epochDay > 0) {
            writer.writeUInt64(startIndex + POST_EPOCH_DAY, epochDay);
        } else if (epochDay < 0) {
            writer.writeUInt64(startIndex + PRE_EPOCH_DAY, 0L - epochDay);
        }
    }
}
