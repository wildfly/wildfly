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

import java.io.IOException;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public class SingletonCollectionMarshaller<T extends Collection<Object>> implements ProtoStreamMarshaller<T> {

    private final Function<Object, T> factory;

    public SingletonCollectionMarshaller(Function<Object, T> factory) {
        this.factory = factory;
    }

    @Override
    public T readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Object value = ObjectMarshaller.INSTANCE.readFrom(context, reader);
        return this.factory.apply(value);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T collection) throws IOException {
        ObjectMarshaller.INSTANCE.writeTo(context, writer, collection.iterator().next());
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T collection) {
        return ObjectMarshaller.INSTANCE.size(context, collection.iterator().next());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends T> getJavaClass() {
        return (Class<T>) this.factory.apply(null).getClass();
    }
}
