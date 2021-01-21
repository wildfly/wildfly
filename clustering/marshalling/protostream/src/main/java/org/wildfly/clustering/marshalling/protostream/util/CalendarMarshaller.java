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
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.time.DurationFieldMarshaller;

/**
 * Mashaller for a {@link java.util.Calendar}.
 * @author Paul Ferraro
 */
public enum CalendarMarshaller implements ProtoStreamMarshaller<Calendar> {
    INSTANCE;

    private static final int TYPE_INDEX = 1;
    private static final int DURATION_INDEX = 2;
    private static final int LENIENT_INDEX = DURATION_INDEX + DurationFieldMarshaller.INSTANCE.getFields();
    private static final int TIME_ZONE_INDEX = LENIENT_INDEX + 1;
    private static final int FIRST_DAY_OF_WEEK_INDEX = TIME_ZONE_INDEX + 1;
    private static final int MIN_DAYS_IN_FIRST_WEEK_INDEX = FIRST_DAY_OF_WEEK_INDEX + 1;

    private static final Calendar DEFAULT = new Calendar.Builder().setInstant(0).build();

    @Override
    public Calendar readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Calendar.Builder builder = new Calendar.Builder().setInstant(0);
        Duration durationSinceEpoch = Duration.ZERO;
        int firstDayOfWeek = DEFAULT.getFirstDayOfWeek();
        int minDaysInFirstWeek = DEFAULT.getMinimalDaysInFirstWeek();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= TYPE_INDEX && index < DURATION_INDEX) {
                builder.setCalendarType(reader.readString());
            } else if (index >= DURATION_INDEX && index < LENIENT_INDEX) {
                durationSinceEpoch = DurationFieldMarshaller.INSTANCE.readField(context, reader, index - DURATION_INDEX, durationSinceEpoch);
            } else if (index == LENIENT_INDEX) {
                builder.setLenient(reader.readBool());
            } else if (index == TIME_ZONE_INDEX) {
                builder.setTimeZone(TimeZone.getTimeZone(reader.readString()));
            } else if (index == FIRST_DAY_OF_WEEK_INDEX) {
                firstDayOfWeek = reader.readUInt32();
            } else if (index == MIN_DAYS_IN_FIRST_WEEK_INDEX) {
                minDaysInFirstWeek = reader.readUInt32();
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        Instant instant = Instant.ofEpochSecond(durationSinceEpoch.getSeconds(), durationSinceEpoch.getNano());
        return builder.setInstant(instant.toEpochMilli()).setWeekDefinition(firstDayOfWeek, minDaysInFirstWeek).build();
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Calendar calendar) throws IOException {
        String type = calendar.getCalendarType();
        if (!type.equals(DEFAULT.getCalendarType())) {
            writer.writeString(TYPE_INDEX, type);
        }
        Date time = calendar.getTime();
        if (!time.equals(DEFAULT.getTime())) {
            Instant instant = calendar.toInstant();
            Duration duration = Duration.ofSeconds(instant.getEpochSecond(), instant.getNano());
            DurationFieldMarshaller.INSTANCE.writeFields(context, writer, DURATION_INDEX, duration);
        }
        boolean lenient = calendar.isLenient();
        if (lenient != DEFAULT.isLenient()) {
            writer.writeBool(LENIENT_INDEX, lenient);
        }
        TimeZone zone = calendar.getTimeZone();
        if (!zone.equals(DEFAULT.getTimeZone())) {
            writer.writeString(TIME_ZONE_INDEX, zone.getID());
        }
        int firstDayOfWeek = calendar.getFirstDayOfWeek();
        if (firstDayOfWeek != DEFAULT.getFirstDayOfWeek()) {
            writer.writeUInt32(FIRST_DAY_OF_WEEK_INDEX, firstDayOfWeek);
        }
        int minDaysInFirstWeek = calendar.getMinimalDaysInFirstWeek();
        if (minDaysInFirstWeek != DEFAULT.getMinimalDaysInFirstWeek()) {
            writer.writeUInt32(MIN_DAYS_IN_FIRST_WEEK_INDEX, minDaysInFirstWeek);
        }
    }

    @Override
    public Class<? extends Calendar> getJavaClass() {
        return Calendar.class;
    }
}
