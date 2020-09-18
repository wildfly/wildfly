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
import org.wildfly.clustering.marshalling.spi.util.CollectionExternalizer;
import org.wildfly.clustering.marshalling.spi.util.CopyOnWriteCollectionExternalizer;
import org.wildfly.clustering.marshalling.spi.util.HashMapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SortedMapExternalizer;
import org.wildfly.clustering.marshalling.spi.util.SortedSetExternalizer;

/**
 * @author Paul Ferraro
 */
public enum ConcurrentExternalizerProvider implements ExternalizerProvider {

    CONCURRENT_HASH_MAP(new HashMapExternalizer<>(ConcurrentHashMap.class, ConcurrentHashMap::new)),
    CONCURRENT_HASH_SET(new CollectionExternalizer<>(ConcurrentHashMap.KeySetView.class, ConcurrentHashMap::newKeySet)),
    CONCURRENT_LINKED_DEQUE(new CollectionExternalizer<>(ConcurrentLinkedDeque.class, size -> new ConcurrentLinkedDeque<>())),
    CONCURRENT_LINKED_QUEUE(new CollectionExternalizer<>(ConcurrentLinkedQueue.class, size -> new ConcurrentLinkedQueue<>())),
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
