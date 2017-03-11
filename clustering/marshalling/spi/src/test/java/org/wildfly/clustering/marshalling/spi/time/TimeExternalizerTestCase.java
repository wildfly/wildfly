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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerTestUtil;

/**
 * Unit test for java.time.* externalizers
 * @author Paul Ferraro
 */
public class TimeExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {

        test(new DayOfWeekExternalizer());
        test(new MonthExternalizer());

        ExternalizerTestUtil.test(new DurationExternalizer(), Duration.between(Instant.EPOCH, Instant.now()));
        ExternalizerTestUtil.test(new InstantExternalizer(), Instant.now());
        ExternalizerTestUtil.test(new LocalDateExternalizer(), LocalDate.now());
        ExternalizerTestUtil.test(new LocalDateTimeExternalizer(), LocalDateTime.now());
        ExternalizerTestUtil.test(new LocalTimeExternalizer(), LocalTime.now());
        ExternalizerTestUtil.test(new MonthDayExternalizer(), MonthDay.now());
        ExternalizerTestUtil.test(new PeriodExternalizer(), Period.between(LocalDate.ofEpochDay(0), LocalDate.now()));
        ExternalizerTestUtil.test(new YearExternalizer(), Year.now());
        ExternalizerTestUtil.test(new YearMonthExternalizer(), YearMonth.now());
        ExternalizerTestUtil.test(new ZoneOffsetExternalizer(), ZoneOffset.UTC);
    }

    private static <E extends Enum<E>> void test(Externalizer<E> externalizer) throws ClassNotFoundException, IOException {
        for (E value : externalizer.getTargetClass().getEnumConstants()) {
            ExternalizerTestUtil.test(externalizer, value);
        }
    }
}
