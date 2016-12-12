/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.security.AllPermission;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.time.zone.ZoneRules;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.junit.Test;

/**
 * Unit test for {@link Immutability}
 *
 * @author Paul Ferraro
 */
public class ImmutabilityTestCase {

    @Test
    public void test() throws Exception {
        this.test(Immutability.INSTANCE);
    }

    protected void test(Predicate<Object> immutability) throws Exception {
        assertFalse(Immutability.INSTANCE.test(new Object()));
        assertFalse(Immutability.INSTANCE.test(new Date()));
        assertFalse(Immutability.INSTANCE.test(new AtomicInteger()));
        assertFalse(Immutability.INSTANCE.test(new AtomicLong()));
        assertTrue(Immutability.INSTANCE.test(null));
        assertTrue(Immutability.INSTANCE.test(Collections.EMPTY_LIST));
        assertTrue(Immutability.INSTANCE.test(Collections.EMPTY_MAP));
        assertTrue(Immutability.INSTANCE.test(Collections.EMPTY_SET));
        assertTrue(Immutability.INSTANCE.test(Boolean.TRUE));
        assertTrue(Immutability.INSTANCE.test(Character.valueOf('a')));
        assertTrue(Immutability.INSTANCE.test(this.getClass()));
        assertTrue(Immutability.INSTANCE.test(Currency.getInstance(Locale.US)));
        assertTrue(Immutability.INSTANCE.test(Locale.getDefault()));
        assertTrue(Immutability.INSTANCE.test(Byte.valueOf(Integer.valueOf(1).byteValue())));
        assertTrue(Immutability.INSTANCE.test(Short.valueOf(Integer.valueOf(1).shortValue())));
        assertTrue(Immutability.INSTANCE.test(Integer.valueOf(1)));
        assertTrue(Immutability.INSTANCE.test(Long.valueOf(1)));
        assertTrue(Immutability.INSTANCE.test(Float.valueOf(1)));
        assertTrue(Immutability.INSTANCE.test(Double.valueOf(1)));
        assertTrue(Immutability.INSTANCE.test(BigInteger.valueOf(1)));
        assertTrue(Immutability.INSTANCE.test(BigDecimal.valueOf(1)));
        assertTrue(Immutability.INSTANCE.test(InetAddress.getLocalHost()));
        assertTrue(Immutability.INSTANCE.test(new InetSocketAddress(InetAddress.getLocalHost(), 80)));
        assertTrue(Immutability.INSTANCE.test(MathContext.UNLIMITED));
        assertTrue(Immutability.INSTANCE.test("test"));
        assertTrue(Immutability.INSTANCE.test(TimeZone.getDefault()));
        assertTrue(Immutability.INSTANCE.test(UUID.randomUUID()));
        assertTrue(Immutability.INSTANCE.test(TimeUnit.DAYS));
        File file = new File(System.getProperty("user.home"));
        assertTrue(Immutability.INSTANCE.test(file));
        assertTrue(Immutability.INSTANCE.test(file.toURI()));
        assertTrue(Immutability.INSTANCE.test(file.toURI().toURL()));
        assertTrue(Immutability.INSTANCE.test(FileSystems.getDefault().getRootDirectories().iterator().next()));
        assertTrue(Immutability.INSTANCE.test(new AllPermission()));

        assertTrue(Immutability.INSTANCE.test(DateTimeFormatter.BASIC_ISO_DATE));
        assertTrue(Immutability.INSTANCE.test(DecimalStyle.STANDARD));
        assertTrue(Immutability.INSTANCE.test(Duration.ZERO));
        assertTrue(Immutability.INSTANCE.test(Instant.now()));
        assertTrue(Immutability.INSTANCE.test(LocalDate.now()));
        assertTrue(Immutability.INSTANCE.test(LocalDateTime.now()));
        assertTrue(Immutability.INSTANCE.test(LocalTime.now()));
        assertTrue(Immutability.INSTANCE.test(MonthDay.now()));
        assertTrue(Immutability.INSTANCE.test(Period.ZERO));
        assertTrue(Immutability.INSTANCE.test(ValueRange.of(0L, 10L)));
        assertTrue(Immutability.INSTANCE.test(WeekFields.ISO));
        assertTrue(Immutability.INSTANCE.test(Year.now()));
        assertTrue(Immutability.INSTANCE.test(YearMonth.now()));
        assertTrue(Immutability.INSTANCE.test(ZoneOffset.UTC));
        assertTrue(Immutability.INSTANCE.test(ZoneRules.of(ZoneOffset.UTC).nextTransition(Instant.now())));
        assertTrue(Immutability.INSTANCE.test(ZoneOffsetTransitionRule.of(Month.JANUARY, 1, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.STANDARD, ZoneOffset.UTC, ZoneOffset.ofHours(1), ZoneOffset.ofHours(2))));
        assertTrue(Immutability.INSTANCE.test(ZoneRules.of(ZoneOffset.UTC)));
        assertTrue(Immutability.INSTANCE.test(ZonedDateTime.now()));
        assertTrue(Immutability.INSTANCE.test(new JCIPImmutableObject()));
    }

    @net.jcip.annotations.Immutable
    static class JCIPImmutableObject {
    }
}
