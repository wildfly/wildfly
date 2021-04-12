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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;

import org.junit.Test;

/**
 * Generic tests for java.time.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractTimeTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractTimeTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testDayOfWeek() throws IOException {
        this.factory.createTester(DayOfWeek.class).test();
    }

    @Test
    public void testDuration() throws IOException {
        MarshallingTester<Duration> tester = this.factory.createTester();
        tester.test(Duration.between(Instant.EPOCH, Instant.now()));
        tester.test(Duration.ofMillis(1234567890));
        tester.test(Duration.ofSeconds(100));
        tester.test(Duration.ZERO);
    }

    @Test
    public void testInstant() throws IOException {
        MarshallingTester<Instant> tester = this.factory.createTester();
        tester.test(Instant.MAX);
        tester.test(Instant.MIN);
        tester.test(Instant.now());
        tester.test(Instant.ofEpochMilli(System.currentTimeMillis()));
    }

    @Test
    public void testLocalDate() throws IOException {
        MarshallingTester<LocalDate> tester = this.factory.createTester();
        tester.test(LocalDate.MAX);
        tester.test(LocalDate.MIN);
        tester.test(LocalDate.now());
        tester.test(LocalDate.ofEpochDay(0));
    }

    @Test
    public void testLocalDateTime() throws IOException {
        MarshallingTester<LocalDateTime> tester = this.factory.createTester();
        tester.test(LocalDateTime.MAX);
        tester.test(LocalDateTime.MIN);
        tester.test(LocalDateTime.now());
        tester.test(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59)));
        tester.test(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)));
        tester.test(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 0)));
        tester.test(LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT));
    }

    @Test
    public void testLocalTime() throws IOException {
        MarshallingTester<LocalTime> tester = this.factory.createTester();
        tester.test(LocalTime.MAX);
        tester.test(LocalTime.MIN);
        tester.test(LocalTime.now());
        tester.test(LocalTime.of(23, 59, 59));
        tester.test(LocalTime.of(23, 59));
        tester.test(LocalTime.of(23, 0));
    }

    @Test
    public void testMonth() throws IOException {
        this.factory.createTester(Month.class).test();
    }

    @Test
    public void testMonthDay() throws IOException {
        MarshallingTester<MonthDay> tester = this.factory.createTester();
        tester.test(MonthDay.now());
    }

    @Test
    public void testOffsetDateTime() throws IOException {
        MarshallingTester<OffsetDateTime> tester = this.factory.createTester();
        tester.test(OffsetDateTime.MAX);
        tester.test(OffsetDateTime.MIN);
        tester.test(OffsetDateTime.now(ZoneOffset.UTC));
        tester.test(OffsetDateTime.now(ZoneOffset.MIN));
        tester.test(OffsetDateTime.now(ZoneOffset.MAX));
    }

    @Test
    public void testOffsetTime() throws IOException {
        MarshallingTester<OffsetTime> tester = this.factory.createTester();
        tester.test(OffsetTime.MAX);
        tester.test(OffsetTime.MIN);
        tester.test(OffsetTime.now(ZoneOffset.UTC));
        tester.test(OffsetTime.now(ZoneOffset.MIN));
        tester.test(OffsetTime.now(ZoneOffset.MAX));
    }

    @Test
    public void testZonedDateTime() throws IOException {
        MarshallingTester<ZonedDateTime> tester = this.factory.createTester();
        tester.test(ZonedDateTime.now(ZoneOffset.UTC));
        tester.test(ZonedDateTime.now(ZoneOffset.MIN));
        tester.test(ZonedDateTime.now(ZoneOffset.MAX));
        tester.test(ZonedDateTime.now(ZoneId.of("America/New_York")));
    }

    @Test
    public void testPeriod() throws IOException {
        MarshallingTester<Period> tester = this.factory.createTester();
        tester.test(Period.between(LocalDate.ofEpochDay(0), LocalDate.now()));
        tester.test(Period.ofMonths(100));
        tester.test(Period.ofYears(100));
        tester.test(Period.ZERO);
    }

    @Test
    public void testYear() throws IOException {
        MarshallingTester<Year> tester = this.factory.createTester();
        tester.test(Year.of(Year.MAX_VALUE));
        tester.test(Year.of(Year.MIN_VALUE));
        tester.test(Year.now());
        tester.test(Year.of(Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear()));
    }

    @Test
    public void testYearMonth() throws IOException {
        MarshallingTester<YearMonth> tester = this.factory.createTester();
        tester.test(YearMonth.of(Year.MAX_VALUE, Month.DECEMBER));
        tester.test(YearMonth.of(Year.MIN_VALUE, Month.JANUARY));
        tester.test(YearMonth.now());
        tester.test(YearMonth.of(Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear(), Instant.EPOCH.atOffset(ZoneOffset.UTC).getMonth()));
    }

    @Test
    public void testZoneId() throws IOException {
        MarshallingTester<ZoneId> tester = this.factory.createTester();
        tester.test(ZoneId.of("America/New_York"));
    }

    @Test
    public void testZoneOffset() throws IOException {
        MarshallingTester<ZoneOffset> tester = this.factory.createTester();
        tester.test(ZoneOffset.MIN);
        tester.test(ZoneOffset.MAX);
        tester.test(ZoneOffset.of("-10")); // Hawaii Standard Time
        tester.test(ZoneOffset.of("+12:45")); // New Zealand's Chatham Islands
        Random random = new Random(System.currentTimeMillis());
        tester.test(ZoneOffset.ofHoursMinutesSeconds(random.nextInt(18), random.nextInt(60), random.nextInt(60)));
        tester.test(ZoneOffset.ofHoursMinutesSeconds(0 - random.nextInt(18), 0 - random.nextInt(60), 0 - random.nextInt(60)));
        tester.test(ZoneOffset.UTC);
    }
}
