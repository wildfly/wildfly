/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TimeZone;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.IntSerializer;

/**
 * @author Paul Ferraro
 */
public class CalendarExternalizer implements Externalizer<Calendar> {
    private static final String[] CALENDAR_TYPE_NAMES;
    private static final Map<String, Integer> CALENDAR_TYPE_IDS = new HashMap<>();
    private static final IntSerializer CALENDAR_TYPE_SERIALIZER;
    static {
        CALENDAR_TYPE_NAMES = Calendar.getAvailableCalendarTypes().toArray(new String[0]);
        CALENDAR_TYPE_SERIALIZER = IndexSerializer.select(CALENDAR_TYPE_NAMES.length);
        Arrays.sort(CALENDAR_TYPE_NAMES);
        for (int i = 0; i < CALENDAR_TYPE_NAMES.length; ++i) {
            CALENDAR_TYPE_IDS.put(CALENDAR_TYPE_NAMES[i], Integer.valueOf(i));
        }
    }

    @Override
    public void writeObject(ObjectOutput output, Calendar calendar) throws IOException {
        CALENDAR_TYPE_SERIALIZER.writeInt(output, CALENDAR_TYPE_IDS.get(calendar.getCalendarType()));
        output.writeLong(calendar.getTimeInMillis());
        output.writeBoolean(calendar.isLenient());
        UtilExternalizerProvider.TIME_ZONE.cast(TimeZone.class).writeObject(output, calendar.getTimeZone());
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, calendar.getFirstDayOfWeek());
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, calendar.getMinimalDaysInFirstWeek());
    }

    @Override
    public Calendar readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new Calendar.Builder()
                .setCalendarType(CALENDAR_TYPE_NAMES[CALENDAR_TYPE_SERIALIZER.readInt(input)])
                .setInstant(input.readLong())
                .setLenient(input.readBoolean())
                .setTimeZone(UtilExternalizerProvider.TIME_ZONE.cast(TimeZone.class).readObject(input))
                .setWeekDefinition(IndexSerializer.UNSIGNED_BYTE.readInt(input), IndexSerializer.UNSIGNED_BYTE.readInt(input))
                .build();
    }

    @Override
    public OptionalInt size(Calendar calendar) {
        int calendarTypeSize = CALENDAR_TYPE_SERIALIZER.size(CALENDAR_TYPE_IDS.get(calendar.getCalendarType()));
        int timeZoneSize = UtilExternalizerProvider.TIME_ZONE.cast(TimeZone.class).size(calendar.getTimeZone()).getAsInt();
        int firstDayOfWeekSize = IndexSerializer.UNSIGNED_BYTE.size(calendar.getFirstDayOfWeek());
        int minimalDaysInFirstWeekSize = IndexSerializer.UNSIGNED_BYTE.size(calendar.getMinimalDaysInFirstWeek());
        return OptionalInt.of(calendarTypeSize + Long.BYTES + Byte.BYTES + timeZoneSize + firstDayOfWeekSize + minimalDaysInFirstWeekSize);
    }

    @Override
    public Class<Calendar> getTargetClass() {
        return Calendar.class;
    }
}
