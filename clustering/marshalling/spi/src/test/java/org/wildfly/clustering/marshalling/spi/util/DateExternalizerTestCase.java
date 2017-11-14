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

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;
import org.wildfly.clustering.marshalling.spi.ExternalizerTestUtil;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Unit test for {@link Date} externalizers.
 * @author Paul Ferraro
 */
public class DateExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        ExternalizerTestUtil.test(DefaultExternalizer.DATE.cast(Date.class), Date.from(Instant.now()));
        ExternalizerTestUtil.test(DefaultExternalizer.SQL_DATE.cast(java.sql.Date.class), java.sql.Date.valueOf(LocalDate.now()));
        ExternalizerTestUtil.test(DefaultExternalizer.SQL_TIME.cast(java.sql.Time.class), java.sql.Time.valueOf(LocalTime.now()));
        ExternalizerTestUtil.test(DefaultExternalizer.SQL_TIMESTAMP.cast(java.sql.Timestamp.class), java.sql.Timestamp.valueOf(LocalDateTime.now()));

        // Validate default calendar
        ExternalizerTestUtil.test(DefaultExternalizer.CALENDAR.cast(Calendar.class), Calendar.getInstance());
        // Validate Gregorian calendar w/locale
        ExternalizerTestUtil.test(DefaultExternalizer.CALENDAR.cast(Calendar.class), new Calendar.Builder().setLenient(false).setLocale(Locale.FRANCE).build());
        // Validate Japanese Imperial calendar
        ExternalizerTestUtil.test(DefaultExternalizer.CALENDAR.cast(Calendar.class), Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"), Locale.JAPAN));
        // Validate Buddhist calendar
        ExternalizerTestUtil.test(DefaultExternalizer.CALENDAR.cast(Calendar.class), Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"), Locale.forLanguageTag("th_TH")));
    }
}
