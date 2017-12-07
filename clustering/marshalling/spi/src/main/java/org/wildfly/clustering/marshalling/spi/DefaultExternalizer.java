/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.net.InetAddressExternalizer;
import org.wildfly.clustering.marshalling.spi.net.InetSocketAddressExternalizer;
import org.wildfly.clustering.marshalling.spi.net.URLExternalizer;
import org.wildfly.clustering.marshalling.spi.time.DurationExternalizer;
import org.wildfly.clustering.marshalling.spi.time.InstantExternalizer;
import org.wildfly.clustering.marshalling.spi.time.LocalDateTimeExternalizer;
import org.wildfly.clustering.marshalling.spi.time.MonthDayExternalizer;
import org.wildfly.clustering.marshalling.spi.time.PeriodExternalizer;
import org.wildfly.clustering.marshalling.spi.time.YearMonthExternalizer;
import org.wildfly.clustering.marshalling.spi.util.CalendarExternalizer;
import org.wildfly.clustering.marshalling.spi.util.CollectionExternalizer;
import org.wildfly.clustering.marshalling.spi.util.CopyOnWriteCollectionExternalizer;
import org.wildfly.clustering.marshalling.spi.util.DateExternalizer;
import org.wildfly.clustering.marshalling.spi.util.MapEntryExternalizer;
import org.wildfly.clustering.marshalling.spi.util.MapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SingletonCollectionExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SingletonMapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SortedMapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SortedSetExternalizer;
import org.wildfly.clustering.marshalling.spi.util.UUIDExternalizer;

/**
 * Default externalizers for common JDK types
 * @author Paul Ferraro
 */
public enum DefaultExternalizer implements Externalizer<Object> {
    // java.net
    INET_ADDRESS(new InetAddressExternalizer<>(InetAddress.class, OptionalInt.empty())),
    INET4_ADDRESS(new InetAddressExternalizer<>(Inet4Address.class, OptionalInt.of(4))),
    INET6_ADDRESS(new InetAddressExternalizer<>(Inet6Address.class, OptionalInt.of(16))),
    INET_SOCKET_ADDRESS(new InetSocketAddressExternalizer()),
    URI(new StringExternalizer<>(java.net.URI.class, java.net.URI::create, java.net.URI::toString)),
    URL(new URLExternalizer()),
    // java.time
    DAY_OF_WEEK(new EnumExternalizer<>(DayOfWeek.class)),
    DURATION(new DurationExternalizer()),
    INSTANT(new InstantExternalizer()),
    LOCAL_DATE(new LongExternalizer<>(LocalDate.class, LocalDate::ofEpochDay, LocalDate::toEpochDay)),
    LOCAL_DATE_TIME(new LocalDateTimeExternalizer()),
    LOCAL_TIME(new LongExternalizer<>(LocalTime.class, LocalTime::ofNanoOfDay, LocalTime::toNanoOfDay)),
    MONTH(new EnumExternalizer<>(Month.class)),
    MONTH_DAY(new MonthDayExternalizer()),
    PERIOD(new PeriodExternalizer()),
    YEAR(new IntExternalizer<>(Year.class, Year::of, Year::getValue)),
    YEAR_MONTH(new YearMonthExternalizer()),
    ZONE_ID(new StringExternalizer<>(ZoneId.class, ZoneId::of, ZoneId::getId)),
    ZONE_OFFSET(new StringExternalizer<>(ZoneOffset.class, ZoneOffset::of, ZoneOffset::getId)),
    // java.util
    ARRAY_DEQUE(new CollectionExternalizer<>(ArrayDeque.class, ArrayDeque::new)),
    ARRAY_LIST(new CollectionExternalizer<>(ArrayList.class, ArrayList::new)),
    ATOMIC_BOOLEAN(new BooleanExternalizer<>(AtomicBoolean.class, AtomicBoolean::new, AtomicBoolean::get)),
    ATOMIC_INTEGER(new IntExternalizer<>(AtomicInteger.class, AtomicInteger::new, AtomicInteger::get)),
    ATOMIC_LONG(new LongExternalizer<>(AtomicLong.class, AtomicLong::new, AtomicLong::get)),
    ATOMIC_REFERENCE(new ObjectExternalizer<>(AtomicReference.class, AtomicReference::new, AtomicReference::get)),
    CALENDAR(new CalendarExternalizer()),
    CONCURRENT_HASH_MAP(new MapExternalizer<>(ConcurrentHashMap.class, ConcurrentHashMap::new)),
    CONCURRENT_HASH_SET(new CollectionExternalizer<>(ConcurrentHashMap.KeySetView.class, ConcurrentHashMap::newKeySet)),
    CONCURRENT_LINKED_DEQUE(new CollectionExternalizer<>(ConcurrentLinkedDeque.class, size -> new ConcurrentLinkedDeque<>())),
    CONCURRENT_LINKED_QUEUE(new CollectionExternalizer<>(ConcurrentLinkedQueue.class, size -> new ConcurrentLinkedQueue<>())),
    CONCURRENT_SKIP_LIST_MAP(new SortedMapExternalizer<>(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new)),
    CONCURRENT_SKIP_LIST_SET(new SortedSetExternalizer<>(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new)),
    COPY_ON_WRITE_ARRAY_LIST(new CopyOnWriteCollectionExternalizer<>(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new)),
    COPY_ON_WRITE_ARRAY_SET(new CopyOnWriteCollectionExternalizer<>(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new)),
    CURRENCY(new StringExternalizer<>(Currency.class, Currency::getInstance, Currency::getCurrencyCode)),
    DATE(new DateExternalizer<>(Date.class, Date::new)),
    EMPTY_ENUMERATION(new ValueExternalizer<>(Collections.emptyEnumeration())),
    EMPTY_ITERATOR(new ValueExternalizer<>(Collections.emptyIterator())),
    EMPTY_LIST(new ValueExternalizer<>(Collections.emptyList())),
    EMPTY_LIST_ITERATOR(new ValueExternalizer<>(Collections.emptyListIterator())),
    EMPTY_MAP(new ValueExternalizer<>(Collections.emptyMap())),
    EMPTY_NAVIGABLE_MAP(new ValueExternalizer<>(Collections.emptyNavigableMap())),
    EMPTY_NAVIGABLE_SET(new ValueExternalizer<>(Collections.emptyNavigableSet())),
    EMPTY_SET(new ValueExternalizer<>(Collections.emptySet())),
    EMPTY_SORTED_MAP(new ValueExternalizer<>(Collections.emptySortedMap())),
    EMPTY_SORTED_SET(new ValueExternalizer<>(Collections.emptySortedSet())),
    HASH_MAP(new MapExternalizer<>(HashMap.class, HashMap::new)),
    HASH_SET(new CollectionExternalizer<>(HashSet.class, HashSet::new)),
    LINKED_HASH_MAP(new MapExternalizer<>(LinkedHashMap.class, LinkedHashMap::new)),
    LINKED_HASH_SET(new CollectionExternalizer<>(LinkedHashSet.class, LinkedHashSet::new)),
    LINKED_LIST(new CollectionExternalizer<>(LinkedList.class, size -> new LinkedList<>())),
    LOCALE(new StringExternalizer<>(Locale.class, Locale::forLanguageTag, Locale::toLanguageTag)),
    NATURAL_ORDER_COMPARATOR(new ValueExternalizer<>(Comparator.naturalOrder())),
    @SuppressWarnings("unchecked")
    OPTIONAL(new ObjectExternalizer<>(Optional.class, Optional::ofNullable, optional -> optional.orElse(null))),
    REVERSE_ORDER_COMPARATOR(new ValueExternalizer<>(Collections.reverseOrder())),
    SIMPLE_ENTRY(new MapEntryExternalizer<>(AbstractMap.SimpleEntry.class, AbstractMap.SimpleEntry::new)),
    SIMPLE_IMMUTABLE_ENTRY(new MapEntryExternalizer<>(AbstractMap.SimpleImmutableEntry.class, AbstractMap.SimpleImmutableEntry::new)),
    SINGLETON_LIST(new SingletonCollectionExternalizer<>(Collections::singletonList)),
    SINGLETON_MAP(new SingletonMapExternalizer()),
    SINGLETON_SET(new SingletonCollectionExternalizer<>(Collections::singleton)),
    SQL_DATE(new DateExternalizer<>(java.sql.Date.class, java.sql.Date::new)),
    SQL_TIME(new DateExternalizer<>(java.sql.Time.class, java.sql.Time::new)),
    SQL_TIMESTAMP(new DateExternalizer.SqlTimestampExternalizer()),
    TIME_UNIT(new EnumExternalizer<>(TimeUnit.class)),
    TIME_ZONE(new StringExternalizer<>(TimeZone.class, TimeZone::getTimeZone, TimeZone::getID)),
    TREE_MAP(new SortedMapExternalizer<>(TreeMap.class, TreeMap::new)),
    TREE_SET(new SortedSetExternalizer<>(TreeSet.class, TreeSet::new)),
    UUID(new UUIDExternalizer()),
    ;

    private final Externalizer<Object> externalizer;

    @SuppressWarnings("unchecked")
    DefaultExternalizer(Externalizer<?> externalizer) {
        this.externalizer = (Externalizer<Object>) externalizer;
    }

    @Override
    public void writeObject(ObjectOutput output, Object object) throws IOException {
        this.externalizer.writeObject(output, object);
    }

    @Override
    public Object readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.externalizer.readObject(input);
    }

    @Override
    public Class<Object> getTargetClass() {
        return this.externalizer.getTargetClass();
    }

    /**
     * Cast this externalizer to its target type.
     * @param type the externalizer type
     * @return the externalizer
     */
    @SuppressWarnings("unchecked")
    public <T> Externalizer<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.externalizer.getTargetClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (Externalizer<T>) this.externalizer;
    }
}
