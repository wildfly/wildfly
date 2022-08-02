/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.weld.annotated.slim;

import java.io.IOException;
import java.lang.reflect.Field;

import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedType;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Generic marshaller for an {@link AnnotatedField}.
 * @author Paul Ferraro
 */
public class AnnotatedFieldMarshaller<X, T extends SlimAnnotatedType<X>, F extends AnnotatedField<X>> implements ProtoStreamMarshaller<F> {

    private static final int TYPE_INDEX = 1;
    private static final int DECLARING_TYPE_INDEX = 2;
    private static final int FIELD_NAME_INDEX = 3;

    private final Class<F> targetClass;
    private final Class<T> typeClass;

    public AnnotatedFieldMarshaller(Class<F> targetClass, Class<T> typeClass) {
        this.targetClass = targetClass;
        this.typeClass = typeClass;
    }

    @Override
    public Class<F> getJavaClass() {
        return this.targetClass;
    }

    @Override
    public F readFrom(ProtoStreamReader reader) throws IOException {
        T type = null;
        Class<?> declaringType = null;
        String fieldName = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TYPE_INDEX:
                    type = reader.readObject(this.typeClass);
                    break;
                case DECLARING_TYPE_INDEX:
                    declaringType = reader.readObject(Class.class);
                    break;
                case FIELD_NAME_INDEX:
                    fieldName = reader.readString();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        Class<?> fieldClass = (declaringType != null) ? declaringType : type.getJavaClass();
        for (AnnotatedField<? super X> annotatedField : type.getFields()) {
            Field field = annotatedField.getJavaMember();
            if ((field.getDeclaringClass() == fieldClass) && field.getName().equals(fieldName)) {
                return this.targetClass.cast(annotatedField);
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, F field) throws IOException {
        AnnotatedType<X> type = field.getDeclaringType();
        if (type != null) {
            writer.writeObject(TYPE_INDEX, type);
        }
        Field member = field.getJavaMember();
        if (member != null) {
            Class<?> declaringClass = member.getDeclaringClass();
            if (declaringClass != type.getJavaClass()) {
                writer.writeObject(DECLARING_TYPE_INDEX, declaringClass);
            }
            writer.writeString(FIELD_NAME_INDEX, member.getName());
        }
    }
}