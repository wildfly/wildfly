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

import org.glassfish.expressly.MethodExpressionImpl;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.function.Function;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;

/**
 * {@link ProtoStreamMarshaller} for a {@link MethodExpressionImpl}.
 * @author Paul Ferraro
 */
public class MethodExpressionImplMarshaller implements ProtoStreamMarshaller<MethodExpressionImpl> {

    private static final int EXPRESSION_INDEX = 1;
    private static final int EXPECTED_TYPE_INDEX = 2;
    private static final int FUNCTION_MAPPER_INDEX = 3;
    private static final int VARIABLE_MAPPER_INDEX = 4;
    private static final int PARAMETER_TYPE_INDEX = 5;

    private static final Function<MethodExpressionImpl, Class<?>> EXPECTED_TYPE_HANDLE = Function.invoke(findHandle(Class.class));
    private static final Function<MethodExpressionImpl, Class<?>[]> PARAMETER_TYPES_FIELD = Function.invoke(findHandle(Class[].class));

    private static MethodHandle findHandle(Class<?> targetType) {
        for (Field field : MethodExpressionImpl.class.getDeclaredFields()) {
            if (field.getType() == targetType) {
                try {
                    return MethodHandles.privateLookupIn(MethodExpressionImpl.class, MethodHandles.lookup()).findGetter(field.getDeclaringClass(), field.getName(), field.getType());
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException(targetType.getCanonicalName());
    }

    @Override
    public Class<? extends MethodExpressionImpl> getJavaClass() {
        return MethodExpressionImpl.class;
    }

    @Override
    public MethodExpressionImpl readFrom(ProtoStreamReader reader) throws IOException {
        String expression = null;
        Class<?> expectedType = null;
        FunctionMapper functionMapper = null;
        VariableMapper variableMapper = null;
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
                case FUNCTION_MAPPER_INDEX:
                    functionMapper = reader.readAny(FunctionMapper.class);
                    break;
                case VARIABLE_MAPPER_INDEX:
                    variableMapper = reader.readAny(VariableMapper.class);
                    break;
                case PARAMETER_TYPE_INDEX:
                    parameterTypes.add(reader.readAny(Class.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new MethodExpressionImpl(expression, null, functionMapper, variableMapper, expectedType, parameterTypes.toArray(new Class<?>[0]));
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, MethodExpressionImpl value) throws IOException {
        String[] strings = new String[2];
        Object[] objects = new Object[3];
        value.writeExternal(new SimpleObjectOutput.Builder().with(strings).with(objects).build());

        String expression = value.getExpressionString();
        if (expression != null) {
            writer.writeString(EXPRESSION_INDEX, expression);
        }
        Class<?> expectedType = EXPECTED_TYPE_HANDLE.apply(value);
        if (expectedType != null) {
            writer.writeAny(EXPECTED_TYPE_INDEX, expectedType);
        }
        Class<?>[] parameterTypes = PARAMETER_TYPES_FIELD.apply(value);
        for (Class<?> parameterType : parameterTypes) {
            writer.writeAny(PARAMETER_TYPE_INDEX, parameterType);
        }
        Object functionMapper = objects[1];
        if (functionMapper != null) {
            writer.writeAny(FUNCTION_MAPPER_INDEX, functionMapper);
        }
        Object variableMapper = objects[2];
        if (variableMapper != null) {
            writer.writeAny(VARIABLE_MAPPER_INDEX, variableMapper);
        }
    }
}
