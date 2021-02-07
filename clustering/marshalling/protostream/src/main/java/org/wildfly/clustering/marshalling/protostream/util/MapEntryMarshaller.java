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
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.function.Function;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link java.util.Map.Entry}
 * @author Paul Ferraro
 * @param <T> the map entry type of this marshaller
 */
public class MapEntryMarshaller<T extends Map.Entry<Object, Object>> implements ProtoStreamMarshaller<T> {

    private static final int KEY_INDEX = 1;
    private static final int VALUE_INDEX = 2;

    private final Class<? extends T> targetClass;
    private final Function<SimpleEntry<Object, Object>, T> factory;

    @SuppressWarnings("unchecked")
    public MapEntryMarshaller(Function<SimpleEntry<Object, Object>, T> factory) {
        this.targetClass = (Class<T>) factory.apply(new SimpleEntry<>(null, null)).getClass();
        this.factory = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        SimpleEntry<Object, Object> entry = new SimpleEntry<>(null, null);
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case KEY_INDEX:
                    Object key = reader.readObject(Any.class).get();
                    entry = new SimpleEntry<>(key, entry.getValue());
                    break;
                case VALUE_INDEX:
                    Object value = reader.readObject(Any.class).get();
                    entry.setValue(value);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return this.factory.apply(entry);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T entry) throws IOException {
        Object key = entry.getKey();
        if (key != null) {
            writer.writeObject(KEY_INDEX, new Any(key));
        }
        Object value = entry.getValue();
        if (key != null) {
            writer.writeObject(VALUE_INDEX, new Any(value));
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
