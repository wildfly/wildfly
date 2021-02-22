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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.util.LinkedHashMapExternalizer;

/**
 * Marshaller for a {@link LinkedHashMap}.
 * @author Paul Ferraro
 */
public class LinkedHashMapMarshaller extends AbstractMapMarshaller<LinkedHashMap<Object, Object>> {

    private static final int ACCESS_ORDER_INDEX = VALUE_INDEX + 1;

    @SuppressWarnings("unchecked")
    public LinkedHashMapMarshaller() {
        super((Class<LinkedHashMap<Object, Object>>) (Class<?>) LinkedHashMap.class);
    }

    @Override
    public LinkedHashMap<Object, Object> readFrom(ProtoStreamReader reader) throws IOException {
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>(16, 0.75f, false);
        List<Object> keys = new LinkedList<>();
        List<Object> values = new LinkedList<>();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case KEY_INDEX:
                    keys.add(reader.readObject(Any.class).get());
                    break;
                case VALUE_INDEX:
                    values.add(reader.readObject(Any.class).get());
                    break;
                case ACCESS_ORDER_INDEX:
                    map = new LinkedHashMap<>(16, 0.75f, reader.readBool());
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        Iterator<Object> keyIterator = keys.iterator();
        Iterator<Object> valueIterator = values.iterator();
        while (keyIterator.hasNext() || valueIterator.hasNext()) {
            map.put(keyIterator.next(), valueIterator.next());
        }
        return map;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, LinkedHashMap<Object, Object> map) throws IOException {
        super.writeTo(writer, map);
        boolean accessOrder = LinkedHashMapExternalizer.ACCESS_ORDER.apply(map);
        if (accessOrder) {
            writer.writeBool(ACCESS_ORDER_INDEX, accessOrder);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends LinkedHashMap<Object, Object>> getJavaClass() {
        return (Class<LinkedHashMap<Object, Object>>) (Class<?>) LinkedHashMap.class;
    }
}
