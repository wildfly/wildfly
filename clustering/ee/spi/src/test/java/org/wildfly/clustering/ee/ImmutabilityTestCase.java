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
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
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
        assertFalse(immutability.test(new Object()));
        assertFalse(immutability.test(new Date()));
        assertFalse(immutability.test(new AtomicInteger()));
        assertFalse(immutability.test(new AtomicLong()));
        assertTrue(immutability.test(null));
        assertTrue(immutability.test(Collections.emptyEnumeration()));
        assertTrue(immutability.test(Collections.emptyIterator()));
        assertTrue(immutability.test(Collections.emptyList()));
        assertTrue(immutability.test(Collections.emptyListIterator()));
        assertTrue(immutability.test(Collections.emptyMap()));
        assertTrue(immutability.test(Collections.emptyNavigableMap()));
        assertTrue(immutability.test(Collections.emptyNavigableSet()));
        assertTrue(immutability.test(Collections.emptySet()));
        assertTrue(immutability.test(Collections.emptySortedMap()));
        assertTrue(immutability.test(Collections.emptySortedSet()));
        assertTrue(immutability.test(Boolean.TRUE));
        assertTrue(immutability.test(Character.valueOf('a')));
        assertTrue(immutability.test(this.getClass()));
        assertTrue(immutability.test(Currency.getInstance(Locale.US)));
        assertTrue(immutability.test(Locale.getDefault()));
        assertTrue(immutability.test(Byte.valueOf(Integer.valueOf(1).byteValue())));
        assertTrue(immutability.test(Short.valueOf(Integer.valueOf(1).shortValue())));
        assertTrue(immutability.test(Integer.valueOf(1)));
        assertTrue(immutability.test(Long.valueOf(1)));
        assertTrue(immutability.test(Float.valueOf(1)));
        assertTrue(immutability.test(Double.valueOf(1)));
        assertTrue(immutability.test(BigInteger.valueOf(1)));
        assertTrue(immutability.test(BigDecimal.valueOf(1)));
        assertTrue(immutability.test(InetAddress.getLocalHost()));
        assertTrue(immutability.test(new InetSocketAddress(InetAddress.getLocalHost(), 80)));
        assertTrue(immutability.test(MathContext.UNLIMITED));
        assertTrue(immutability.test("test"));
        assertTrue(immutability.test(TimeZone.getDefault()));
        assertTrue(immutability.test(UUID.randomUUID()));
        assertTrue(immutability.test(TimeUnit.DAYS));
        File file = new File(System.getProperty("user.home"));
        assertTrue(immutability.test(file));
        assertTrue(immutability.test(file.toURI()));
        assertTrue(immutability.test(file.toURI().toURL()));
        assertTrue(immutability.test(FileSystems.getDefault().getRootDirectories().iterator().next()));
        assertTrue(immutability.test(new AllPermission()));

        assertTrue(immutability.test(DateTimeFormatter.BASIC_ISO_DATE));
        assertTrue(immutability.test(DecimalStyle.STANDARD));
        assertTrue(immutability.test(Duration.ZERO));
        assertTrue(immutability.test(Instant.now()));
        assertTrue(immutability.test(LocalDate.now()));
        assertTrue(immutability.test(LocalDateTime.now()));
        assertTrue(immutability.test(LocalTime.now()));
        assertTrue(immutability.test(MonthDay.now()));
        assertTrue(immutability.test(Period.ZERO));
        assertTrue(immutability.test(ValueRange.of(0L, 10L)));
        assertTrue(immutability.test(WeekFields.ISO));
        assertTrue(immutability.test(Year.now()));
        assertTrue(immutability.test(YearMonth.now()));
        assertTrue(immutability.test(ZoneOffset.UTC));
        assertTrue(immutability.test(ZoneRules.of(ZoneOffset.UTC).nextTransition(Instant.now())));
        assertTrue(immutability.test(ZoneOffsetTransitionRule.of(Month.JANUARY, 1, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.STANDARD, ZoneOffset.UTC, ZoneOffset.ofHours(1), ZoneOffset.ofHours(2))));
        assertTrue(immutability.test(ZoneRules.of(ZoneOffset.UTC)));
        assertTrue(immutability.test(ZonedDateTime.now()));
        assertTrue(immutability.test(new JCIPImmutableObject()));

        assertTrue(immutability.test(Collections.singleton("1")));
        assertTrue(immutability.test(Collections.singletonList("1")));
        assertTrue(immutability.test(Collections.singletonMap("1", "2")));

        assertTrue(immutability.test(Collections.singleton(new JCIPImmutableObject())));
        assertTrue(immutability.test(Collections.singletonList(new JCIPImmutableObject())));
        assertTrue(immutability.test(Collections.singletonMap("1", new JCIPImmutableObject())));

        assertTrue(immutability.test(Collections.unmodifiableCollection(Arrays.asList("1", "2"))));
        assertTrue(immutability.test(Collections.unmodifiableList(Arrays.asList("1", "2"))));
        assertTrue(immutability.test(Collections.unmodifiableMap(Collections.singletonMap("1", "2"))));
        assertTrue(immutability.test(Collections.unmodifiableNavigableMap(new TreeMap<>(Collections.singletonMap("1", "2")))));
        assertTrue(immutability.test(Collections.unmodifiableNavigableSet(new TreeSet<>(Collections.singleton("1")))));
        assertTrue(immutability.test(Collections.unmodifiableSet(Collections.singleton("1"))));
        assertTrue(immutability.test(Collections.unmodifiableSortedMap(new TreeMap<>(Collections.singletonMap("1", "2")))));
        assertTrue(immutability.test(Collections.unmodifiableSortedSet(new TreeSet<>(Collections.singleton("1")))));

        Object mutableObject = new AtomicInteger();
        assertFalse(immutability.test(Collections.singletonList(mutableObject)));
        assertFalse(immutability.test(Collections.singletonMap("1", mutableObject)));
    }

    @net.jcip.annotations.Immutable
    static class JCIPImmutableObject {
    }
}
