/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.lang.reflect.Array;

import org.infinispan.protostream.descriptors.WireType;

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
            while (!reader.isAtEnd()) {
                int tag = reader.readTag();
                int index = WireType.getTagFieldNumber(tag);
                if (index == ANY.getIndex()) {
                    componentClass = ScalarClass.ANY.readFrom(reader);
                } else {
                    reader.skipField(tag);
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
            writer.writeVarint32(dimensions);
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
        public WireType getWireType() {
            return WireType.VARINT;
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
        return (index > 0) && (index <= FIELDS.length) ? FIELDS[index - 1] : null;
    }
}