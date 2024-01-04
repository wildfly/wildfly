/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.util.CollectionMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.SortedSetMarshaller;

/**
 * @author Paul Ferraro
 */
public enum ConcurrentMarshallerProvider implements ProtoStreamMarshallerProvider {
    CONCURRENT_HASH_MAP(new ConcurrentMapMarshaller<>(ConcurrentHashMap::new)),
    CONCURRENT_HASH_SET(new CollectionMarshaller<>(ConcurrentHashMap::newKeySet)),
    CONCURRENT_LINKED_DEQUE(new CollectionMarshaller<>(ConcurrentLinkedDeque::new)),
    CONCURRENT_LINKED_QUEUE(new CollectionMarshaller<>(ConcurrentLinkedQueue::new)),
    CONCURRENT_SKIP_LIST_MAP(new ConcurrentSortedMapMarshaller<>(ConcurrentSkipListMap::new)),
    CONCURRENT_SKIP_LIST_SET(new SortedSetMarshaller<>(ConcurrentSkipListSet::new)),
    COPY_ON_WRITE_ARRAY_LIST(new CopyOnWriteCollectionMarshaller<>(CopyOnWriteArrayList::new)),
    COPY_ON_WRITE_ARRAY_SET(new CopyOnWriteCollectionMarshaller<>(CopyOnWriteArraySet::new)),
    TIME_UNIT(new EnumMarshaller<>(TimeUnit.class)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ConcurrentMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
