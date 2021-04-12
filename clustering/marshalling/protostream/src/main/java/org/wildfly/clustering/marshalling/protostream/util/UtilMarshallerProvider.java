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

package org.wildfly.clustering.marshalling.protostream.util;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamBuilderFieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;
import org.wildfly.common.function.Functions;

/**
 * Enumeration of java.util marshallers.
 * @author Paul Ferraro
 */
public enum UtilMarshallerProvider implements ProtoStreamMarshallerProvider {
    ARRAY_DEQUE(new CollectionMarshaller<>(ArrayDeque::new)),
    ARRAY_LIST(new CollectionMarshaller<>(ArrayList::new)),
    BIT_SET(new FunctionalScalarMarshaller<>(Scalar.BYTE_ARRAY.cast(byte[].class), BitSet::new, BitSet::isEmpty, BitSet::toByteArray, BitSet::valueOf)),
    CALENDAR(new CalendarMarshaller()),
    CURRENCY(new FunctionalScalarMarshaller<>(Currency.class, Scalar.STRING.cast(String.class), Functions.constantSupplier(getDefaultCurrency()), Currency::getCurrencyCode, Currency::getInstance)),
    DATE(new FunctionalMarshaller<>(Date.class, Instant.class, Date::toInstant, Date::from)),
    EMPTY_ENUMERATION(new ValueMarshaller<>(Collections.emptyEnumeration())),
    EMPTY_ITERATOR(new ValueMarshaller<>(Collections.emptyIterator())),
    EMPTY_LIST(new ValueMarshaller<>(Collections.emptyList())),
    EMPTY_LIST_ITERATOR(new ValueMarshaller<>(Collections.emptyListIterator())),
    EMPTY_MAP(new ValueMarshaller<>(Collections.emptyMap())),
    EMPTY_NAVIGABLE_MAP(new ValueMarshaller<>(Collections.emptyNavigableMap())),
    EMPTY_NAVIGABLE_SET(new ValueMarshaller<>(Collections.emptyNavigableSet())),
    EMPTY_SET(new ValueMarshaller<>(Collections.emptySet())),
    EMPTY_SORTED_MAP(new ValueMarshaller<>(Collections.emptySortedMap())),
    EMPTY_SORTED_SET(new ValueMarshaller<>(Collections.emptySortedSet())),
    ENUM_MAP(new EnumMapMarshaller<>()),
    ENUM_SET(new EnumSetMarshaller<>()),
    HASH_MAP(new MapMarshaller<>(HashMap::new)),
    HASH_SET(new CollectionMarshaller<>(HashSet::new)),
    LINKED_HASH_MAP(new LinkedHashMapMarshaller()),
    LINKED_HASH_SET(new CollectionMarshaller<>(LinkedHashSet::new)),
    LINKED_LIST(new CollectionMarshaller<>(LinkedList::new)),
    LOCALE(new FunctionalScalarMarshaller<>(Scalar.STRING.cast(String.class), Functions.constantSupplier(Locale.getDefault()), Locale::toLanguageTag, Locale::forLanguageTag)),
    OPTIONAL(OptionalMarshaller.OBJECT),
    OPTIONAL_DOUBLE(OptionalMarshaller.DOUBLE),
    OPTIONAL_INT(OptionalMarshaller.INT),
    OPTIONAL_LONG(OptionalMarshaller.LONG),
    SIMPLE_ENTRY(new MapEntryMarshaller<>(AbstractMap.SimpleEntry::new)),
    SIMPLE_IMMUTABLE_ENTRY(new MapEntryMarshaller<>(AbstractMap.SimpleImmutableEntry::new)),
    SINGLETON_LIST(new SingletonCollectionMarshaller<>(Collections::singletonList)),
    SINGLETON_MAP(new SingletonMapMarshaller(Collections::singletonMap)),
    SINGLETON_SET(new SingletonCollectionMarshaller<>(Collections::singleton)),
    TIME_ZONE(new FunctionalScalarMarshaller<>(TimeZone.class, Scalar.STRING.cast(String.class), Functions.constantSupplier(TimeZone.getDefault()), TimeZone::getID, TimeZone::getTimeZone)),
    TREE_MAP(new SortedMapMarshaller<>(TreeMap::new)),
    TREE_SET(new SortedSetMarshaller<>(TreeSet::new)),
    UUID(new ProtoStreamBuilderFieldSetMarshaller<>(UUIDMarshaller.INSTANCE)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    UtilMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }

    private static Currency getDefaultCurrency() {
        try {
            return Currency.getInstance(Locale.getDefault());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
