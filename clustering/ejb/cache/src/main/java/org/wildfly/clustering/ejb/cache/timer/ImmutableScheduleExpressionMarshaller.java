/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;
import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public enum ImmutableScheduleExpressionMarshaller implements FieldSetMarshaller.Supplied<ImmutableScheduleExpression, ImmutableScheduleExpressionBuilder> {
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

    private final ImmutableScheduleExpression defaultExpression = this.createInitialValue().get();

    @Override
    public ImmutableScheduleExpressionBuilder createInitialValue() {
        return new DefaultImmutableScheduleExpressionBuilder();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public ImmutableScheduleExpressionBuilder readFrom(ProtoStreamReader reader, int index, WireType type, ImmutableScheduleExpressionBuilder builder) throws IOException {
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
                reader.skipField(type);
                return builder;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ImmutableScheduleExpression expression) throws IOException {
        Instant start = expression.getStart();
        if (!Objects.equals(start, this.defaultExpression.getStart())) {
            writer.writeObject(START_FIELD, start);
        }
        Instant end = expression.getEnd();
        if (!Objects.equals(end, this.defaultExpression.getEnd())) {
            writer.writeObject(END_FIELD, end);
        }
        String year = expression.getYear();
        if (!Objects.equals(year, this.defaultExpression.getYear())) {
            writer.writeString(YEAR_FIELD, year);
        }
        String month = expression.getMonth();
        if (!Objects.equals(month, this.defaultExpression.getMonth())) {
            writer.writeString(MONTH_FIELD, month);
        }
        String dayOfMonth = expression.getDayOfMonth();
        if (!Objects.equals(dayOfMonth, this.defaultExpression.getDayOfMonth())) {
            writer.writeString(DAY_OF_MONTH_FIELD, dayOfMonth);
        }
        String dayOfWeek = expression.getDayOfWeek();
        if (!Objects.equals(dayOfWeek, this.defaultExpression.getDayOfWeek())) {
            writer.writeString(DAY_OF_WEEK_FIELD, dayOfWeek);
        }
        ZoneId zone = expression.getZone();
        if (!Objects.equals(zone, this.defaultExpression.getZone())) {
            writer.writeObject(ZONE_FIELD, zone);
        }
        String hour = expression.getHour();
        if (!Objects.equals(hour, this.defaultExpression.getHour())) {
            writer.writeString(HOUR_FIELD, hour);
        }
        String minute = expression.getMinute();
        if (!Objects.equals(minute, this.defaultExpression.getMinute())) {
            writer.writeString(MINUTE_FIELD, minute);
        }
        String second = expression.getSecond();
        if (!Objects.equals(second, this.defaultExpression.getSecond())) {
            writer.writeString(SECOND_FIELD, second);
        }
    }

    public class DefaultImmutableScheduleExpressionBuilder implements ImmutableScheduleExpressionBuilder {

        private String second = "0";
        private String minute = "0";
        private String hour = "0";

        private String dayOfMonth = "*";
        private String month = "*";
        private String dayOfWeek = "*";
        private String year = "*";

        private ZoneId zone;
        private Instant start;
        private Instant end;

        @Override
        public ImmutableScheduleExpressionBuilder second(String second) {
            this.second = second;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder minute(String minute) {
            this.minute = minute;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder hour(String hour) {
            this.hour = hour;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder dayOfMonth(String dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder month(String month) {
            this.month = month;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder dayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder year(String year) {
            this.year = year;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder zone(ZoneId zone) {
            this.zone = zone;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder start(Instant start) {
            this.start = start;
            return this;
        }

        @Override
        public ImmutableScheduleExpressionBuilder end(Instant end) {
            this.end = end;
            return this;
        }

        @Override
        public ImmutableScheduleExpression get() {
            String second = this.second;
            String minute = this.minute;
            String hour = this.hour;
            String dayOfMonth = this.dayOfMonth;
            String month = this.month;
            String dayOfWeek = this.dayOfWeek;
            String year = this.year;
            ZoneId zone = this.zone;
            Instant start = this.start;
            Instant end = this.end;

            return new ImmutableScheduleExpression() {
                @Override
                public String getSecond() {
                    return second;
                }

                @Override
                public String getMinute() {
                    return minute;
                }

                @Override
                public String getHour() {
                    return hour;
                }

                @Override
                public String getDayOfMonth() {
                    return dayOfMonth;
                }

                @Override
                public String getMonth() {
                    return month;
                }

                @Override
                public String getDayOfWeek() {
                    return dayOfWeek;
                }

                @Override
                public String getYear() {
                    return year;
                }

                @Override
                public ZoneId getZone() {
                    return zone;
                }

                @Override
                public Instant getStart() {
                    return start;
                }

                @Override
                public Instant getEnd() {
                    return end;
                }
            };
        }
    }
}
