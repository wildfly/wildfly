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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public enum ImmutableScheduleExpressionMarshaller implements FieldSetMarshaller<ImmutableScheduleExpression, ImmutableScheduleExpressionBuilder> {
    INSTANCE;

    private static final int START_FIELD = 0;
    private static final int END_FIELD = 1;
    private static final int YEAR_FIELD = 2;
    private static final int MONTH_FIELD = 3;
    private static final int DAY_OF_MONTH_FIELD = 4;
    private static final int DAY_OF_WEEK_FIELD = 5;
    private static final int ZONE_FIELD = 6;
    private static final int HOUR_FIELD = 7;
    private static final int MINUTE_FIELD = 8;
    private static final int SECOND_FIELD = 9;
    private static final int FIELDS = 10;

    private final ImmutableScheduleExpression defaultExpression = this.getBuilder().build();

    @Override
    public ImmutableScheduleExpressionBuilder getBuilder() {
        return new ImmutableScheduleExpressionBuilder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public ImmutableScheduleExpressionBuilder readField(ProtoStreamReader reader, int index, ImmutableScheduleExpressionBuilder builder) throws IOException {
        switch (index) {
            case START_FIELD:
                return builder.start(reader.readObject(Instant.class));
            case END_FIELD:
                return builder.end(reader.readObject(Instant.class));
            case YEAR_FIELD:
                return builder.year(reader.readString());
            case MONTH_FIELD:
                return builder.month(reader.readString());
            case DAY_OF_MONTH_FIELD:
                return builder.dayOfMonth(reader.readString());
            case DAY_OF_WEEK_FIELD:
                return builder.dayOfWeek(reader.readString());
            case ZONE_FIELD:
                return builder.zone(reader.readObject(ZoneId.class));
            case HOUR_FIELD:
                return builder.hour(reader.readString());
            case MINUTE_FIELD:
                return builder.minute(reader.readString());
            case SECOND_FIELD:
                return builder.second(reader.readString());
            default:
                return builder;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, ImmutableScheduleExpression expression) throws IOException {
        Instant start = expression.getStart();
        if (!Objects.equals(start, this.defaultExpression.getStart())) {
            writer.writeObject(startIndex + START_FIELD, start);
        }
        Instant end = expression.getEnd();
        if (!Objects.equals(end, this.defaultExpression.getEnd())) {
            writer.writeObject(startIndex + END_FIELD, end);
        }
        String year = expression.getYear();
        if (!Objects.equals(year, this.defaultExpression.getYear())) {
            writer.writeString(startIndex + YEAR_FIELD, year);
        }
        String month = expression.getMonth();
        if (!Objects.equals(month, this.defaultExpression.getMonth())) {
            writer.writeString(startIndex + MONTH_FIELD, month);
        }
        String dayOfMonth = expression.getDayOfMonth();
        if (!Objects.equals(dayOfMonth, this.defaultExpression.getDayOfMonth())) {
            writer.writeString(startIndex + DAY_OF_MONTH_FIELD, dayOfMonth);
        }
        String dayOfWeek = expression.getDayOfWeek();
        if (!Objects.equals(dayOfWeek, this.defaultExpression.getDayOfWeek())) {
            writer.writeString(startIndex + DAY_OF_WEEK_FIELD, dayOfWeek);
        }
        ZoneId zone = expression.getZone();
        if (!Objects.equals(zone, this.defaultExpression.getZone())) {
            writer.writeObject(startIndex + ZONE_FIELD, zone);
        }
        String hour = expression.getHour();
        if (!Objects.equals(hour, this.defaultExpression.getHour())) {
            writer.writeString(startIndex + HOUR_FIELD, hour);
        }
        String minute = expression.getMinute();
        if (!Objects.equals(minute, this.defaultExpression.getMinute())) {
            writer.writeString(startIndex + MINUTE_FIELD, minute);
        }
        String second = expression.getSecond();
        if (!Objects.equals(second, this.defaultExpression.getSecond())) {
            writer.writeString(startIndex + SECOND_FIELD, second);
        }
    }
}
