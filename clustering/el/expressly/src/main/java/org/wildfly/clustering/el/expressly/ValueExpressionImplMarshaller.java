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

import org.glassfish.expressly.ValueExpressionImpl;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;

/**
 * {@link ProtoStreamMarshaller} for a {@link ValueExpressionImpl}.
 * @author Paul Ferraro
 */
public class ValueExpressionImplMarshaller implements ProtoStreamMarshaller<ValueExpressionImpl> {

    private static final int EXPRESSION_INDEX = 1;
    private static final int EXPECTED_TYPE_INDEX = 2;
    private static final int FUNCTION_MAPPER_INDEX = 3;
    private static final int VARIABLE_MAPPER_INDEX = 4;

    @Override
    public Class<? extends ValueExpressionImpl> getJavaClass() {
        return ValueExpressionImpl.class;
    }

    @Override
    public ValueExpressionImpl readFrom(ProtoStreamReader reader) throws IOException {
        String expression = null;
        Class<?> expectedType = null;
        FunctionMapper functionMapper = null;
        VariableMapper variableMapper = null;
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
                default:
                    reader.skipField(tag);
            }
        }
        return new ValueExpressionImpl(expression, null, functionMapper, variableMapper, expectedType);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ValueExpressionImpl value) throws IOException {
        String[] strings = new String[2];
        Object[] objects = new Object[2];
        value.writeExternal(new SimpleObjectOutput.Builder().with(strings).with(objects).build());

        String expression = strings[0];
        if (expression != null) {
            writer.writeString(EXPRESSION_INDEX, expression);
        }
        Class<?> expectedType = value.getExpectedType();
        if (expectedType != null) {
            writer.writeAny(EXPECTED_TYPE_INDEX, expectedType);
        }
        Object functionMapper = objects[0];
        if (functionMapper != null) {
            writer.writeAny(FUNCTION_MAPPER_INDEX, functionMapper);
        }
        Object variableMapper = objects[1];
        if (variableMapper != null) {
            writer.writeAny(VARIABLE_MAPPER_INDEX, variableMapper);
        }
    }
}
