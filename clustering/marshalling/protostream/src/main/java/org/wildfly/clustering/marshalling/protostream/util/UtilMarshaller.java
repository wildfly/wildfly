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

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.protostream.ExternalizerMarshaller;
import org.wildfly.clustering.marshalling.protostream.MarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.spi.util.UtilExternalizerProvider;

/**
 * @author Paul Ferraro
 */
public enum UtilMarshaller implements MarshallerProvider {
    ARRAY_DEQUE(new BoundedCollectionMarshaller<>(ArrayDeque.class, ArrayDeque::new)),
    ARRAY_LIST(new BoundedCollectionMarshaller<>(ArrayList.class, ArrayList::new)),
    BIT_SET(UtilExternalizerProvider.BIT_SET),
    CALENDAR(UtilExternalizerProvider.CALENDAR),
    CURRENCY(UtilExternalizerProvider.CURRENCY),
    DATE(UtilExternalizerProvider.DATE),
    EMPTY_ENUMERATION(UtilExternalizerProvider.EMPTY_ENUMERATION),
    EMPTY_ITERATOR(UtilExternalizerProvider.EMPTY_ITERATOR),
    EMPTY_LIST(UtilExternalizerProvider.EMPTY_LIST),
    EMPTY_LIST_ITERATOR(UtilExternalizerProvider.EMPTY_LIST_ITERATOR),
    EMPTY_MAP(UtilExternalizerProvider.EMPTY_MAP),
    EMPTY_NAVIGABLE_MAP(UtilExternalizerProvider.EMPTY_NAVIGABLE_MAP),
    EMPTY_NAVIGABLE_SET(UtilExternalizerProvider.EMPTY_NAVIGABLE_SET),
    EMPTY_SET(UtilExternalizerProvider.EMPTY_SET),
    EMPTY_SORTED_MAP(UtilExternalizerProvider.EMPTY_SORTED_MAP),
    EMPTY_SORTED_SET(UtilExternalizerProvider.EMPTY_SORTED_SET),
    ENUM_MAP(new EnumMapMarshaller<>()),
    ENUM_SET(new EnumSetMarshaller<>()),
    HASH_MAP(new HashMapMarshaller<>(HashMap.class, HashMap::new)),
    HASH_SET(new HashSetMarshaller<>(HashSet.class, HashSet::new)),
    LINKED_HASH_MAP(new LinkedHashMapMarshaller()),
    LINKED_HASH_SET(new HashSetMarshaller<>(LinkedHashSet.class, LinkedHashSet::new)),
    LINKED_LIST(new UnboundedCollectionMarshaller<>(LinkedList.class, LinkedList::new)),
    LOCALE(UtilExternalizerProvider.LOCALE),
    OPTIONAL(OptionalMarshaller.OBJECT),
    OPTIONAL_DOUBLE(OptionalMarshaller.DOUBLE),
    OPTIONAL_INT(OptionalMarshaller.INT),
    OPTIONAL_LONG(OptionalMarshaller.LONG),
    SIMPLE_ENTRY(new MapEntryMarshaller<>(AbstractMap.SimpleEntry.class, AbstractMap.SimpleEntry::new)),
    SIMPLE_IMMUTABLE_ENTRY(new MapEntryMarshaller<>(AbstractMap.SimpleImmutableEntry.class, AbstractMap.SimpleImmutableEntry::new)),
    SINGLETON_LIST(new SingletonCollectionMarshaller<>(Collections::singletonList)),
    SINGLETON_MAP(new SingletonMapMarshaller<>(Collections::singletonMap)),
    SINGLETON_SET(new SingletonCollectionMarshaller<>(Collections::singleton)),
    TIME_ZONE(UtilExternalizerProvider.TIME_ZONE),
    TREE_MAP(new SortedMapMarshaller<>(TreeMap.class, TreeMap::new)),
    TREE_SET(new SortedSetMarshaller<>(TreeSet.class, TreeSet::new)),
    UUID(UUIDMarshaller.INSTANCE),
    ;
    private final ProtoStreamMarshaller<Object> marshaller;

    @SuppressWarnings("unchecked")
    UtilMarshaller(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = (ProtoStreamMarshaller<Object>) marshaller;
    }

    UtilMarshaller(Externalizer<?> externalizer) {
        this(new ExternalizerMarshaller<>(externalizer));
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
