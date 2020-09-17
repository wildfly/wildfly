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
import java.util.Comparator;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ObjectMarshaller;

/**
 * @author Paul Ferraro
 */
public class SortedMapMarshaller<T extends SortedMap<Object, Object>> extends MapMarshaller<T, Comparator<Object>> {

    public SortedMapMarshaller(Class<?> targetClass, Function<Comparator<Object>, T> factory) {
        super(targetClass, factory);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Comparator<Object> readContext(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return (Comparator<Object>) ObjectMarshaller.INSTANCE.readFrom(context, reader);
    }

    @Override
    protected void writeContext(ImmutableSerializationContext context, RawProtoStreamWriter writer, T map) throws IOException {
        ObjectMarshaller.INSTANCE.writeTo(context, writer, map.comparator());
    }

    @Override
    protected OptionalInt sizeContext(ImmutableSerializationContext context, T map) {
        return ObjectMarshaller.INSTANCE.size(context, map.comparator());
    }
}
