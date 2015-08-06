/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.Permission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.format.DateTimeFormatter;
import java.time.format.DecimalStyle;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.wildfly.clustering.web.annotation.Immutable;

/**
 * Determines whether a given object is mutable.
 * @author Paul Ferraro
 */
public class MutableDetector {

    // Singleton immutable objects detectable via reference equality test
    private static final Set<Object> IMMUTABLE_OBJECTS = createIdentitySet(Arrays.asList(
            null,
            Collections.EMPTY_LIST,
            Collections.EMPTY_MAP,
            Collections.EMPTY_SET
    ));

    // Concrete immutable classes detectable via reference equality test
    private static final Set<Class<?>> IMMUTABLE_CLASSES = createIdentitySet(Arrays.asList(
            BigDecimal.class,
            BigInteger.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Class.class,
            Currency.class,
            DateTimeFormatter.class,
            DecimalStyle.class,
            Double.class,
            Duration.class,
            File.class,
            Float.class,
            Inet4Address.class,
            Inet6Address.class,
            InetSocketAddress.class,
            Instant.class,
            Integer.class,
            Locale.class,
            LocalDate.class,
            LocalDateTime.class,
            LocalTime.class,
            Long.class,
            MathContext.class,
            MonthDay.class,
            Period.class,
            Short.class,
            StackTraceElement.class,
            String.class,
            URI.class,
            URL.class,
            UUID.class,
            ValueRange.class,
            WeekFields.class,
            Year.class,
            YearMonth.class,
            ZoneOffset.class,
            ZoneOffsetTransition.class,
            ZoneOffsetTransitionRule.class,
            ZoneRules.class,
            ZonedDateTime.class
    ));

    // Interfaces and abstract classes documented to be immutable, but only detectable via instanceof tests
    private static final List<Class<?>> IMMUTABLE_ABSTRACT_CLASSES = Arrays.asList(
            Chronology.class,
            ChronoLocalDate.class,
            Clock.class,
            Enum.class, // In theory, one could implement a mutable enum, but that would just be weird.
            Era.class,
            Path.class,
            Permission.class,
            TemporalField.class,
            TemporalUnit.class,
            TimeZone.class, // Strictly speaking, this class is mutable, although in practice it is never mutated.
            ZoneId.class
    );

    private static <T> Set<T> createIdentitySet(List<T> list) {
        Map<T, Void> result = new IdentityHashMap<>(list.size());
        for (T element : list) {
            result.put(element, null);
        }
        return Collections.unmodifiableSet(result.keySet());
    }

    public static boolean isMutable(Object object) {

        if (IMMUTABLE_OBJECTS.contains(object)) return false;

        Class<?> objectClass = object.getClass();
        if (IMMUTABLE_CLASSES.contains(objectClass)) return false;

        for (Class<?> immutableClass: IMMUTABLE_ABSTRACT_CLASSES) {
            if (immutableClass.isInstance(object)) return false;
        }

        return !objectClass.isAnnotationPresent(Immutable.class) && !objectClass.isAnnotationPresent(net.jcip.annotations.Immutable.class);
    }

    private MutableDetector() {
        // Hide
    }
}