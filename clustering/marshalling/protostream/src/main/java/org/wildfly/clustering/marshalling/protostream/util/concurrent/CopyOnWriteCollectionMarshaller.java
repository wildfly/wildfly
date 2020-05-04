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
import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.CollectionMarshaller;

/**
 * ProtoStream optimized marshaller for copy-on-write implementations of {@link Collection}.
 * @author Paul Ferraro
 */
public class CopyOnWriteCollectionMarshaller<T extends Collection<Object>> implements ProtoStreamMarshaller<T> {
    @SuppressWarnings("unchecked")
    private static final ProtoStreamMarshaller<Collection<Object>> COLLECTION_MARSHALLER = new CollectionMarshaller<>((Class<Collection<Object>>) (Class<?>) Collection.class, ArrayList::new);

    private final Class<T> targetClass;
    private final Function<Collection<Object>, T> factory;

    public CopyOnWriteCollectionMarshaller(Class<T> targetClass, Function<Collection<Object>, T> factory) {
        this.targetClass = targetClass;
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Collection<Object> collection = COLLECTION_MARSHALLER.readFrom(context, reader);
        return this.factory.apply(collection);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T collection) throws IOException {
        COLLECTION_MARSHALLER.writeTo(context, writer, collection);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T collection) {
        return COLLECTION_MARSHALLER.size(context, collection);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
