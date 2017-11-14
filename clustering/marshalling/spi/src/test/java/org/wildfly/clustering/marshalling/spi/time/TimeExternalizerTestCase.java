/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Test;
import org.wildfly.clustering.marshalling.spi.ExternalizerTestUtil;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Unit test for java.time.* externalizers
 * @author Paul Ferraro
 */
public class TimeExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {

        ExternalizerTestUtil.test(DefaultExternalizer.DAY_OF_WEEK.cast(DayOfWeek.class));
        ExternalizerTestUtil.test(DefaultExternalizer.MONTH.cast(Month.class));

        ExternalizerTestUtil.test(DefaultExternalizer.DURATION.cast(Duration.class), Duration.between(Instant.EPOCH, Instant.now()));
        ExternalizerTestUtil.test(DefaultExternalizer.INSTANT.cast(Instant.class), Instant.now());
        ExternalizerTestUtil.test(DefaultExternalizer.LOCAL_DATE.cast(LocalDate.class), LocalDate.now());
        ExternalizerTestUtil.test(DefaultExternalizer.LOCAL_DATE_TIME.cast(LocalDateTime.class), LocalDateTime.now());
        ExternalizerTestUtil.test(DefaultExternalizer.LOCAL_TIME.cast(LocalTime.class), LocalTime.now());
        ExternalizerTestUtil.test(DefaultExternalizer.MONTH_DAY.cast(MonthDay.class), MonthDay.now());
        ExternalizerTestUtil.test(DefaultExternalizer.PERIOD.cast(Period.class), Period.between(LocalDate.ofEpochDay(0), LocalDate.now()));
        ExternalizerTestUtil.test(DefaultExternalizer.YEAR.cast(Year.class), Year.now());
        ExternalizerTestUtil.test(DefaultExternalizer.YEAR_MONTH.cast(YearMonth.class), YearMonth.now());
        ExternalizerTestUtil.test(DefaultExternalizer.ZONE_OFFSET.cast(ZoneOffset.class), ZoneOffset.UTC);
        ExternalizerTestUtil.test(DefaultExternalizer.ZONE_ID, ZoneId.of("America/New_York"));
    }
}
