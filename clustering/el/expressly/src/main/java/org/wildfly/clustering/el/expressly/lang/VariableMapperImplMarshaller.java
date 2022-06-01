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

package org.wildfly.clustering.el.expressly.lang;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.glassfish.expressly.lang.VariableMapperImpl;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

import jakarta.el.ValueExpression;

/**
 * @author Paul Ferraro
 */
public class VariableMapperImplMarshaller implements ProtoStreamMarshaller<VariableMapperImpl> {

    private static final int VARIABLE_INDEX = 1;
    private static final int EXPRESSION_INDEX = 2;

    @Override
    public Class<? extends VariableMapperImpl> getJavaClass() {
        return VariableMapperImpl.class;
    }

    @Override
    public VariableMapperImpl readFrom(ProtoStreamReader reader) throws IOException {
        List<String> variables = new LinkedList<>();
        List<ValueExpression> expressions = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case VARIABLE_INDEX:
                    variables.add(reader.readString());
                    break;
                case EXPRESSION_INDEX:
                    expressions.add(reader.readAny(ValueExpression.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        VariableMapperImpl mapper = new VariableMapperImpl();
        Iterator<String> variableIterator = variables.iterator();
        Iterator<ValueExpression> expressionIterator = expressions.iterator();
        while (variableIterator.hasNext() && expressionIterator.hasNext()) {
            String variable = variableIterator.next();
            ValueExpression expression = expressionIterator.next();
            mapper.setVariable(variable, expression);
        }
        return mapper;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, VariableMapperImpl value) throws IOException {
        Object[] objects = new Object[1];
        value.writeExternal(new SimpleObjectOutput.Builder().with(objects).build());

        @SuppressWarnings("unchecked")
        Map<String, ValueExpression> expressions = (Map<String, ValueExpression>) objects[0];
        if (expressions != null) {
            for (Map.Entry<String, ValueExpression> entry : expressions.entrySet()) {
                writer.writeString(VARIABLE_INDEX, entry.getKey());
                writer.writeAny(EXPRESSION_INDEX, entry.getValue());
            }
        }
    }
}
