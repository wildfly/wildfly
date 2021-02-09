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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.lang.reflect.Array;

import org.infinispan.protostream.impl.WireFormat;

/**
 * Various strategies for marshalling a Class.
 * @author Paul Ferraro
 */
public enum ClassField implements Field<Class<?>> {
    ANY(ScalarClass.ANY),
    ID(ScalarClass.ID),
    NAME(ScalarClass.NAME),
    FIELD(ScalarClass.FIELD),
    ARRAY(new FieldMarshaller<Class<?>>() {
        @Override
        public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
            int dimensions = reader.readUInt32();
            Class<?> componentClass = Object.class;
            boolean reading = true;
            while (reading) {
                int tag = reader.readTag();
                int index = WireFormat.getTagFieldNumber(tag);
                if (index == ANY.getIndex()) {
                    componentClass = ScalarClass.ANY.readFrom(reader);
                } else {
                    reading = (tag != 0) && reader.skipField(tag);
                }
            }
            for (int i = 0; i < dimensions; ++i) {
                componentClass = Array.newInstance(componentClass, 0).getClass();
            }
            return componentClass;
        }

        @Override
        public void writeTo(ProtoStreamWriter writer, Class<?> targetClass) throws IOException {
            int dimensions = 0;
            Class<?> componentClass = targetClass;
            while (componentClass.isArray() && !componentClass.getComponentType().isPrimitive()) {
                componentClass = componentClass.getComponentType();
                dimensions += 1;
            }
            writer.writeUInt32NoTag(dimensions);
            if (componentClass != Object.class) {
                writer.writeTag(ANY.getIndex(), ANY.getMarshaller().getWireType());
                ScalarClass.ANY.writeTo(writer, componentClass);
            }
        }

        @Override
        public Class<? extends Class<?>> getJavaClass() {
            return ScalarClass.ANY.getJavaClass();
        }

        @Override
        public int getWireType() {
            return WireFormat.WIRETYPE_VARINT;
        }
    }),
    ;
    private final FieldMarshaller<Class<?>> marshaller;

    ClassField(ScalarMarshaller<Class<?>> value) {
        this(new ScalarFieldMarshaller<>(value));
    }

    ClassField(FieldMarshaller<Class<?>> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public int getIndex() {
        return this.ordinal() + 1;
    }

    @Override
    public FieldMarshaller<Class<?>> getMarshaller() {
        return this.marshaller;
    }

    private static final ClassField[] FIELDS = values();

    static ClassField fromIndex(int index) {
        return (index > 0) ? FIELDS[index - 1] : null;
    }
}