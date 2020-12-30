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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Mashaller for a {@link java.util.Calendar}.
 * @author Paul Ferraro
 */
public class CalendarMarshaller implements ProtoStreamMarshaller<Calendar> {

    private static final int TYPE_INDEX = 1;
    private static final int TIME_INDEX = 2;
    private static final int LENIENT_INDEX = 3;
    private static final int TIME_ZONE_INDEX = 4;
    private static final int FIRST_DAY_OF_WEEK_INDEX = 5;
    private static final int MIN_DAYS_IN_FIRST_WEEK_INDEX = 6;

    private static final Calendar DEFAULT = new Calendar.Builder().setInstant(0).build();

    @Override
    public Calendar readFrom(ProtoStreamReader reader) throws IOException {
        Calendar.Builder builder = new Calendar.Builder().setInstant(0);
        int firstDayOfWeek = DEFAULT.getFirstDayOfWeek();
        int minDaysInFirstWeek = DEFAULT.getMinimalDaysInFirstWeek();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TYPE_INDEX:
                    builder.setCalendarType(reader.readString());
                    break;
                case TIME_INDEX:
                    builder.setInstant(reader.readObject(Date.class));
                    break;
                case LENIENT_INDEX:
                    builder.setLenient(reader.readBool());
                    break;
                case TIME_ZONE_INDEX:
                    builder.setTimeZone(TimeZone.getTimeZone(reader.readString()));
                    break;
                case FIRST_DAY_OF_WEEK_INDEX:
                    firstDayOfWeek = reader.readUInt32();
                    break;
                case MIN_DAYS_IN_FIRST_WEEK_INDEX:
                    minDaysInFirstWeek = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return builder.setWeekDefinition(firstDayOfWeek, minDaysInFirstWeek).build();
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Calendar calendar) throws IOException {
        String type = calendar.getCalendarType();
        if (!type.equals(DEFAULT.getCalendarType())) {
            writer.writeString(TYPE_INDEX, type);
        }
        Date time = calendar.getTime();
        if (!time.equals(DEFAULT.getTime())) {
            writer.writeObject(TIME_INDEX, time);
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
