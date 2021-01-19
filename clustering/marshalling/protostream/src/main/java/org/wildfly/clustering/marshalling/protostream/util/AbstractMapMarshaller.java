/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import java.util.Map;
import java.util.OptionalInt;
import java.util.AbstractMap.SimpleEntry;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.Predictable;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractMapMarshaller<T extends Map<Object, Object>> implements ProtoStreamMarshaller<T> {
    protected static final int ENTRY_INDEX = 1;

    private final Class<? extends T> mapClass;

    public AbstractMapMarshaller(Class<? extends T> mapClass) {
        this.mapClass = mapClass;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, T map) throws IOException {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            writer.writeBytes(ENTRY_INDEX, ProtoStreamMarshaller.write(context, new SimpleEntry<>(entry)));
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T map) {
        int size = 0;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            OptionalInt entrySize = Predictable.computeSize(context, ENTRY_INDEX, new SimpleEntry<>(entry));
            if (entrySize.isPresent()) {
                size += entrySize.getAsInt();
            } else {
                return entrySize;
            }
        }
        return OptionalInt.of(size);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.mapClass;
    }
}
