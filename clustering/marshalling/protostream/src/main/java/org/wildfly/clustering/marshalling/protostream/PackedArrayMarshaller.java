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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.lang.reflect.Array;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for packed repeated fields, e.g. primitive arrays.
 * See https://developers.google.com/protocol-buffers/docs/encoding?hl=id#packed
 * @author Paul Ferraro
 * @param <T> the component type of this marshaller
 */
public class PackedArrayMarshaller<T> implements ScalarMarshaller<Object> {

    private final Class<T> componentType;
    private final ScalarMarshaller<T> element;
    private final Class<? extends Object> arrayClass;

    public PackedArrayMarshaller(Class<T> componentType, ScalarMarshaller<T> element) {
        this.componentType = componentType;
        this.element = element;
        this.arrayClass = Array.newInstance(this.componentType, 0).getClass();
    }

    @Override
    public Object readFrom(ProtoStreamReader reader) throws IOException {
        int length = reader.readUInt32();
        Object array = Array.newInstance(this.componentType, length);
        for (int i = 0; i < length; ++i) {
            Object element = this.element.readFrom(reader);
            Array.set(array, i, element);
        }
        return array;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Object array) throws IOException {
        int length = Array.getLength(array);
        writer.writeVarint32(length);
        for (int i = 0; i < length; ++i) {
            @SuppressWarnings("unchecked")
            T element = (T) Array.get(array, i);
            this.element.writeTo(writer, element);
        }
    }

    @Override
    public Class<? extends Object> getJavaClass() {
        return this.arrayClass;
    }

    @Override
    public WireType getWireType() {
        return WireType.LENGTH_DELIMITED;
    }
}
