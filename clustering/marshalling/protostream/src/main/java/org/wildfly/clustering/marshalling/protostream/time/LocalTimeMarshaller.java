/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link LocalTime} instances, using the following strategy:
 * <ol>
 * <li>Marshal {@link LocalTime#MIDNIGHT} as zero bytes</li>
 * <li>Marshal number of seconds in day as unsigned integer, using hours or minutes precision, if possible</li>
 * <li>Marshal sub-second value of day as unsigned integer, using millisecond precision if possible</li>
 * </ol>
 * @author Paul Ferraro
 */
public enum LocalTimeMarshaller implements FieldSetMarshaller.Simple<LocalTime> {
    INSTANCE;

    private static final int HOURS_OF_DAY_INDEX = 0;
    private static final int MINUTES_OF_DAY_INDEX = 1;
    private static final int SECONDS_OF_DAY_INDEX = 2;
    private static final int MILLIS_INDEX = 3;
    private static final int NANOS_INDEX = 4;
    private static final int FIELDS = 5;

    @Override
    public LocalTime createInitialValue() {
        return LocalTime.MIDNIGHT;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public LocalTime readFrom(ProtoStreamReader reader, int index, WireType type, LocalTime time) throws IOException {
        switch (index) {
            case HOURS_OF_DAY_INDEX:
                return time.with(ChronoField.HOUR_OF_DAY, reader.readUInt32());
            case MINUTES_OF_DAY_INDEX:
                return time.with(ChronoField.MINUTE_OF_DAY, reader.readUInt32());
            case SECONDS_OF_DAY_INDEX:
                return time.with(ChronoField.SECOND_OF_DAY, reader.readUInt32());
            case MILLIS_INDEX:
                return time.with(ChronoField.MILLI_OF_SECOND, reader.readUInt32());
            case NANOS_INDEX:
                return time.withNano(reader.readUInt32());
            default:
                reader.skipField(type);
                return time;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, LocalTime time) throws IOException {
        int secondOfDay = time.toSecondOfDay();
        if (secondOfDay > 0) {
            if (secondOfDay % 60 == 0) {
                int minutesOfDay = secondOfDay / 60;
                if (minutesOfDay % 60 == 0) {
                    int hoursOfDay = minutesOfDay / 60;
                    writer.writeUInt32(HOURS_OF_DAY_INDEX, hoursOfDay);
                } else {
                    writer.writeUInt32(MINUTES_OF_DAY_INDEX, minutesOfDay);
                }
            } else {
                writer.writeUInt32(SECONDS_OF_DAY_INDEX, secondOfDay);
            }
        }
        int nanos = time.getNano();
        if (nanos > 0) {
            // Use ms precision, if possible
            if (nanos % 1_000_000 == 0) {
                writer.writeUInt32(MILLIS_INDEX, time.get(ChronoField.MILLI_OF_SECOND));
            } else {
                writer.writeUInt32(NANOS_INDEX, nanos);
            }
        }
    }
}
