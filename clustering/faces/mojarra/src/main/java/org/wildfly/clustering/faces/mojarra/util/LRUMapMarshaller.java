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

package org.wildfly.clustering.faces.mojarra.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.Any;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.util.AbstractMapMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

import com.sun.faces.util.LRUMap;

/**
 * @author Paul Ferraro
 */
public class LRUMapMarshaller extends AbstractMapMarshaller<LRUMap<Object, Object>> {

    private static final int MAX_CAPACITY_INDEX = VALUE_INDEX + 1;
    private static final int DEFAULT_MAX_CAPACITY = 15;
    private static final Field MAX_CAPACITY_FIELD = WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
        @Override
        public Field run() {
            for (Field field : LRUMap.class.getDeclaredFields()) {
                if (field.getType() == Integer.TYPE) {
                    field.setAccessible(true);
                    return field;
                }
            }
            throw new IllegalStateException();
        }
    });

    @SuppressWarnings("unchecked")
    public LRUMapMarshaller() {
        super((Class<LRUMap<Object, Object>>) (Class<?>) LRUMap.class);
    }

    @Override
    public LRUMap<Object, Object> readFrom(ProtoStreamReader reader) throws IOException {
        int maxCapacity = DEFAULT_MAX_CAPACITY;
        List<Object> keys = new LinkedList<>();
        List<Object> values = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case KEY_INDEX:
                    keys.add(reader.readObject(Any.class).get());
                    break;
                case VALUE_INDEX:
                    values.add(reader.readObject(Any.class).get());
                    break;
                case MAX_CAPACITY_INDEX:
                    maxCapacity = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        LRUMap<Object, Object> map = new LRUMap<>(maxCapacity);
        Iterator<Object> keyIterator = keys.iterator();
        Iterator<Object> valueIterator = values.iterator();
        while (keyIterator.hasNext() || valueIterator.hasNext()) {
            map.put(keyIterator.next(), valueIterator.next());
        }
        return map;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, LRUMap<Object, Object> map) throws IOException {
        super.writeTo(writer, map);
        try {
            int maxCapacity = MAX_CAPACITY_FIELD.getInt(map);
            if (maxCapacity != DEFAULT_MAX_CAPACITY) {
                writer.writeUInt32(MAX_CAPACITY_INDEX, maxCapacity);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
