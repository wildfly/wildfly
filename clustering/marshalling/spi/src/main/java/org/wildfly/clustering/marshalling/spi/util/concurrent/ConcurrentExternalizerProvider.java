/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.EnumExternalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.util.CopyOnWriteCollectionExternalizer;
import org.wildfly.clustering.marshalling.spi.util.HashMapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.HashSetExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SortedMapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SortedSetExternalizer;
import org.wildfly.clustering.marshalling.spi.util.UnboundedCollectionExternalizer;

/**
 * @author Paul Ferraro
 */
public enum ConcurrentExternalizerProvider implements ExternalizerProvider {

    CONCURRENT_HASH_MAP(new HashMapExternalizer<>(ConcurrentHashMap.class, ConcurrentHashMap::new)),
    CONCURRENT_HASH_SET(new HashSetExternalizer<>(ConcurrentHashMap.KeySetView.class, ConcurrentHashMap::newKeySet)),
    CONCURRENT_LINKED_DEQUE(new UnboundedCollectionExternalizer<>(ConcurrentLinkedDeque.class, ConcurrentLinkedDeque::new)),
    CONCURRENT_LINKED_QUEUE(new UnboundedCollectionExternalizer<>(ConcurrentLinkedQueue.class, ConcurrentLinkedQueue::new)),
    CONCURRENT_SKIP_LIST_MAP(new SortedMapExternalizer<>(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new)),
    CONCURRENT_SKIP_LIST_SET(new SortedSetExternalizer<>(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new)),
    COPY_ON_WRITE_ARRAY_LIST(new CopyOnWriteCollectionExternalizer<>(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new)),
    COPY_ON_WRITE_ARRAY_SET(new CopyOnWriteCollectionExternalizer<>(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new)),
    TIME_UNIT(new EnumExternalizer<>(TimeUnit.class)),
    ;
    private final Externalizer<?> externalizer;

    ConcurrentExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
