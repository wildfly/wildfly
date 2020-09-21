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

package org.wildfly.clustering.marshalling.spi.util;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.ObjectExternalizer;
import org.wildfly.clustering.marshalling.spi.StringExternalizer;
import org.wildfly.clustering.marshalling.spi.ValueExternalizer;

/**
 * Externalizers for the java.util package
 * @author Paul Ferraro
 */
public enum UtilExternalizerProvider implements ExternalizerProvider {

    ARRAY_DEQUE(new BoundedCollectionExternalizer<>(ArrayDeque.class, ArrayDeque::new)),
    ARRAY_LIST(new BoundedCollectionExternalizer<>(ArrayList.class, ArrayList::new)),
    BIT_SET(new BitSetExternalizer()),
    CALENDAR(new CalendarExternalizer()),
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
    ENUM_MAP(new EnumMapExternalizer<>()),
    ENUM_SET(new EnumSetExternalizer<>()),
    HASH_MAP(new HashMapExternalizer<>(HashMap.class, HashMap::new)),
    HASH_SET(new BoundedCollectionExternalizer<>(HashSet.class, HashSet::new)),
    LINKED_HASH_MAP(new LinkedHashMapExternalizer()),
    LINKED_HASH_SET(new BoundedCollectionExternalizer<>(LinkedHashSet.class, LinkedHashSet::new)),
    LINKED_LIST(new UnboundedCollectionExternalizer<>(LinkedList.class, LinkedList::new)),
    LOCALE(new StringExternalizer<>(Locale.class, Locale::forLanguageTag, Locale::toLanguageTag)),
    NATURAL_ORDER_COMPARATOR(new ValueExternalizer<>(Comparator.naturalOrder())),
    @SuppressWarnings({ "unchecked", "rawtypes" })
    OPTIONAL(new ObjectExternalizer<Optional>(Optional.class, Optional::ofNullable, optional -> optional.orElse(null)) {
        @Override
        public OptionalInt size(Optional optional) {
            return optional.isPresent() ? OptionalInt.empty() : OptionalInt.of(Byte.BYTES);
        }
    }),
    OPTIONAL_DOUBLE(new OptionalDoubleExternalizer()),
    OPTIONAL_INT(new OptionalIntExternalizer()),
    OPTIONAL_LONG(new OptionalLongExternalizer()),
    REVERSE_ORDER_COMPARATOR(new ValueExternalizer<>(Collections.reverseOrder())),
    SIMPLE_ENTRY(new MapEntryExternalizer<>(AbstractMap.SimpleEntry.class, AbstractMap.SimpleEntry::new)),
    SIMPLE_IMMUTABLE_ENTRY(new MapEntryExternalizer<>(AbstractMap.SimpleImmutableEntry.class, AbstractMap.SimpleImmutableEntry::new)),
    SINGLETON_LIST(new SingletonCollectionExternalizer<>(Collections::singletonList)),
    SINGLETON_MAP(new SingletonMapExternalizer()),
    SINGLETON_SET(new SingletonCollectionExternalizer<>(Collections::singleton)),
    TIME_ZONE(new StringExternalizer<>(TimeZone.class, TimeZone::getTimeZone, TimeZone::getID)),
    TREE_MAP(new SortedMapExternalizer<>(TreeMap.class, TreeMap::new)),
    TREE_SET(new SortedSetExternalizer<>(TreeSet.class, TreeSet::new)),
    UUID(new UUIDExternalizer()),
    ;
    private final Externalizer<?> externalizer;

    UtilExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
