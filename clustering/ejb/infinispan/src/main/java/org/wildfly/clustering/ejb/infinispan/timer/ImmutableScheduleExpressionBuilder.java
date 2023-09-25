/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Instant;
import java.time.ZoneId;

import org.wildfly.clustering.ejb.timer.ImmutableScheduleExpression;

/**
 * @author Paul Ferraro
 */
public class ImmutableScheduleExpressionBuilder {

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

    public ImmutableScheduleExpressionBuilder second(String second) {
        this.second = second;
        return this;
    }

    public ImmutableScheduleExpressionBuilder minute(String minute) {
        this.minute = minute;
        return this;
    }

    public ImmutableScheduleExpressionBuilder hour(String hour) {
        this.hour = hour;
        return this;
    }

    public ImmutableScheduleExpressionBuilder dayOfMonth(String dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
        return this;
    }

    public ImmutableScheduleExpressionBuilder month(String month) {
        this.month = month;
        return this;
    }

    public ImmutableScheduleExpressionBuilder dayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
        return this;
    }

    public ImmutableScheduleExpressionBuilder year(String year) {
        this.year = year;
        return this;
    }

    public ImmutableScheduleExpressionBuilder zone(ZoneId zone) {
        this.zone = zone;
        return this;
    }

    public ImmutableScheduleExpressionBuilder start(Instant start) {
        this.start = start;
        return this;
    }

    public ImmutableScheduleExpressionBuilder end(Instant end) {
        this.end = end;
        return this;
    }

    public ImmutableScheduleExpression build() {
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
