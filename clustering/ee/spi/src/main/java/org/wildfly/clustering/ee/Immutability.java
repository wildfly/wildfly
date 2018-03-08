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
package org.wildfly.clustering.ee;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Determines whether a given object is immutable.
 * @author Paul Ferraro
 */
public enum Immutability implements Predicate<Object> {

    OBJECT() {
        // Singleton immutable objects detectable via reference equality test
        private final Set<Object> immutableObjects = createIdentitySet(Arrays.asList(
                null,
                Collections.emptyEnumeration(),
                Collections.emptyIterator(),
                Collections.emptyList(),
                Collections.emptyListIterator(),
                Collections.emptyMap(),
                Collections.emptyNavigableMap(),
                Collections.emptyNavigableSet(),
                Collections.emptySet(),
                Collections.emptySortedMap(),
                Collections.emptySortedSet()
        ));

        @Override
        public boolean test(Object object) {
            return this.immutableObjects.contains(object);
        }
    },
    CLASS() {
        // Concrete immutable classes detectable via reference equality test
        private final Set<Class<?>> immutableClasses = createIdentitySet(Arrays.asList(
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

        @Override
        public boolean test(Object object) {
            return this.immutableClasses.contains(object.getClass());
        }
    },
    ABSTRACT_CLASS() {
        // Interfaces and abstract classes documented to be immutable, but only detectable via instanceof tests
        private final List<Class<?>> immutableClasses = Arrays.asList(
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

        @Override
        public boolean test(Object object) {
            for (Class<?> immutableClass : this.immutableClasses) {
                if (immutableClass.isInstance(object)) return true;
            }
            return false;
        }
    },
    COLLECTION() {
        @Override
        public boolean test(Object object) {
            return COLLECTION_INSTANCE.test(object);
        }
    },
    ANNOTATION() {
        @Override
        public boolean test(Object object) {
            return object.getClass().isAnnotationPresent(net.jcip.annotations.Immutable.class);
        }
    },
    ;

    public static final Predicate<Object> INSTANCE = object -> {
        // These are not an expensive predicates, so there is little to gain from parallel computation
        for (Predicate<Object> predicate : EnumSet.allOf(Immutability.class)) {
            if (predicate.test(object)) return true;
        }
        return false;
    };
    static final Predicate<Object> COLLECTION_INSTANCE = new CollectionImmutability(INSTANCE);

    static <T> Set<T> createIdentitySet(Collection<T> list) {
        Set<T> result = Collections.newSetFromMap(new IdentityHashMap<>(list.size()));
        result.addAll(list);
        return Collections.unmodifiableSet(result);
    }
}