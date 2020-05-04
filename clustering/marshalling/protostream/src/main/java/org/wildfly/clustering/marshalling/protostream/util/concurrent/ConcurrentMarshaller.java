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

package org.wildfly.clustering.marshalling.protostream.util.concurrent;

import java.io.IOException;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.protostream.ExternalizerMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.CollectionMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.HashMapMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.SortedMapMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.SortedSetMarshaller;

/**
 * @author Paul Ferraro
 */
public enum ConcurrentMarshaller implements ProtoStreamMarshaller<Object> {
    CONCURRENT_HASH_MAP(new HashMapMarshaller<>(ConcurrentHashMap.class, ConcurrentHashMap::new)),
    CONCURRENT_HASH_SET(new CollectionMarshaller<>(ConcurrentHashMap.KeySetView.class, ConcurrentHashMap::newKeySet)),
    CONCURRENT_LINKED_DEQUE(new CollectionMarshaller<>(ConcurrentLinkedDeque.class, size -> new ConcurrentLinkedDeque<>())),
    CONCURRENT_LINKED_QUEUE(new CollectionMarshaller<>(ConcurrentLinkedQueue.class, size -> new ConcurrentLinkedQueue<>())),
    CONCURRENT_SKIP_LIST_MAP(new SortedMapMarshaller<>(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new)),
    CONCURRENT_SKIP_LIST_SET(new SortedSetMarshaller<>(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new)),
    COPY_ON_WRITE_ARRAY_LIST(new CopyOnWriteCollectionMarshaller<>(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new)),
    COPY_ON_WRITE_ARRAY_SET(new CopyOnWriteCollectionMarshaller<>(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new)),
    ;
    private final ProtoStreamMarshaller<Object> marshaller;

    @SuppressWarnings("unchecked")
    ConcurrentMarshaller(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = (ProtoStreamMarshaller<Object>) marshaller;
    }

    ConcurrentMarshaller(Externalizer<?> externalizer) {
        this(new ExternalizerMarshaller<>(externalizer));
    }

    @Override
    public Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return this.marshaller.readFrom(context, reader);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object value) throws IOException {
        this.marshaller.writeTo(context, writer, value);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Object value) {
        return this.marshaller.size(context, value);
    }

    @Override
    public String getTypeName() {
        return this.marshaller.getTypeName();
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return this.marshaller.getJavaClass();
    }
}
