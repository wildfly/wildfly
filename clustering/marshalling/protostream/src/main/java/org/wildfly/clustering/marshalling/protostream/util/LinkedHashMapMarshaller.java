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
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;

/**
 * @author Paul Ferraro
 */
public class LinkedHashMapMarshaller extends MapMarshaller<LinkedHashMap<Object, Object>, Boolean> {

    public LinkedHashMapMarshaller() {
        super(LinkedHashMap.class, new Function<Boolean, LinkedHashMap<Object, Object>>() {
            @Override
            public LinkedHashMap<Object, Object> apply(Boolean accessOrder) {
                // Use capacity and load factor defaults
                return new LinkedHashMap<>(16, 0.75f, accessOrder);
            }
        });
    }

    @Override
    protected Boolean readContext(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return reader.readBool();
    }

    @Override
    protected void writeContext(ImmutableSerializationContext context, RawProtoStreamWriter writer, LinkedHashMap<Object, Object> map) throws IOException {
        Object insertOrder = new Object();
        Object accessOrder = new Object();
        map.put(insertOrder, null);
        map.put(accessOrder, null);
        // Access first inserted entry
        // If map uses access order, this element will move to the tail of the map
        map.get(insertOrder);
        Iterator<Object> keys = map.keySet().iterator();
        Object element = keys.next();
        while ((element != insertOrder) && (element != accessOrder)) {
            element = keys.next();
        }
        map.remove(insertOrder);
        map.remove(accessOrder);
        ((RawProtoStreamWriterImpl) writer).getDelegate().writeBoolNoTag(element == accessOrder);
    }

    @Override
    protected OptionalInt sizeContext(ImmutableSerializationContext context, LinkedHashMap<Object, Object> map) {
        return OptionalInt.of(Byte.BYTES);
    }
}
