/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.glassfish.expressly.MethodExpressionLiteral;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * {@link ProtoStreamMarshaller} for a {@link MethodExpressionLiteral}.
 * @author Paul Ferraro
 */
public class MethodExpressionLiteralMarshaller implements ProtoStreamMarshaller<MethodExpressionLiteral> {

    private static final int EXPRESSION_INDEX = 1;
    private static final int EXPECTED_TYPE_INDEX = 2;
    private static final int PARAMETER_TYPE_INDEX = 3;

    private static final Function<MethodExpressionLiteral, Class<?>> EXPECTED_TYPE_HANDLE = Function.invoke(findHandle(Class.class));
    private static final Function<MethodExpressionLiteral, Class<?>[]> PARAMETER_TYPES_HANDLE = Function.invoke(findHandle(Class[].class));

    private static MethodHandle findHandle(Class<?> targetType) {
        for (Field field : MethodExpressionLiteral.class.getDeclaredFields()) {
            if (field.getType() == targetType) {
                try {
                    return MethodHandles.privateLookupIn(MethodExpressionLiteral.class, MethodHandles.lookup()).findGetter(field.getDeclaringClass(), field.getName(), field.getType());
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException(targetType.getCanonicalName());
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
        Class<?> expectedType = EXPECTED_TYPE_HANDLE.apply(value);
        if (expectedType != null) {
            writer.writeAny(EXPECTED_TYPE_INDEX, expectedType);
        }
        Class<?>[] parameterTypes = PARAMETER_TYPES_HANDLE.apply(value);
        if (parameterTypes.length > 0) {
            for (Class<?> parameterType : parameterTypes) {
                writer.writeAny(PARAMETER_TYPE_INDEX, parameterType);
            }
        }
    }
}
