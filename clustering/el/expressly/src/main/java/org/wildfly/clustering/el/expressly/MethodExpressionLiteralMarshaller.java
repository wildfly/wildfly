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

package org.wildfly.clustering.el.expressly;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;

import org.glassfish.expressly.MethodExpressionLiteral;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link ProtoStreamMarshaller} for a {@link MethodExpressionLiteral}.
 * @author Paul Ferraro
 */
public class MethodExpressionLiteralMarshaller implements ProtoStreamMarshaller<MethodExpressionLiteral> {

    private static final int EXPRESSION_INDEX = 1;
    private static final int EXPECTED_TYPE_INDEX = 2;
    private static final int PARAMETER_TYPE_INDEX = 3;

    private static final Field EXPECTED_TYPE_FIELD = getField(Class.class);
    private static final Field PARAMETER_TYPES_FIELD = getField(Class[].class);

    private static Field getField(Class<?> targetType) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                for (Field field : MethodExpressionLiteral.class.getDeclaredFields()) {
                    if (field.getType() == targetType) {
                        field.setAccessible(true);
                        return field;
                    }
                }
                throw new IllegalStateException();
            }
        });
    }

    @Override
    public Class<? extends MethodExpressionLiteral> getJavaClass() {
        return MethodExpressionLiteral.class;
    }

    @Override
    public MethodExpressionLiteral readFrom(ProtoStreamReader reader) throws IOException {
        String expression = null;
        Class<?> expectedType = null;
        List<Class<?>> parameterTypes = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case EXPRESSION_INDEX:
                    expression = reader.readString();
                    break;
                case EXPECTED_TYPE_INDEX:
                    expectedType = reader.readAny(Class.class);
                    break;
                case PARAMETER_TYPE_INDEX:
                    parameterTypes.add(reader.readAny(Class.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new MethodExpressionLiteral(expression, expectedType, parameterTypes.toArray(new Class<?>[0]));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, MethodExpressionLiteral value) throws IOException {
        String expression = value.getExpressionString();
        if (expression != null) {
            writer.writeString(EXPRESSION_INDEX, expression);
        }
        Class<?> expectedType = getValue(value, EXPECTED_TYPE_FIELD, Class.class);
        if (expectedType != null) {
            writer.writeAny(EXPECTED_TYPE_INDEX, expectedType);
        }
        Class<?>[] parameterTypes = getValue(value, PARAMETER_TYPES_FIELD, Class[].class);
        if (parameterTypes.length > 0) {
            for (Class<?> parameterType : parameterTypes) {
                writer.writeAny(PARAMETER_TYPE_INDEX, parameterType);
            }
        }
    }

    private static <T> T getValue(MethodExpressionLiteral value, Field field, Class<T> targetClass) {
        return WildFlySecurityManager.doUnchecked(new PrivilegedAction<T>() {
            @Override
            public T run() {
                try {
                    return targetClass.cast(field.get(value));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
}
