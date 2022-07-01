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

import org.glassfish.expressly.ValueExpressionLiteral;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

/**
 * {@link ProtoStreamMarshaller} for a {@link ValueExpressionLiteral}.
 * @author Paul Ferraro
 */
public class ValueExpressionLiteralMarshaller implements ProtoStreamMarshaller<ValueExpressionLiteral> {

    private static final int VALUE_INDEX = 1;
    private static final int EXPECTED_TYPE_INDEX = 2;

    @Override
    public Class<? extends ValueExpressionLiteral> getJavaClass() {
        return ValueExpressionLiteral.class;
    }

    @Override
    public ValueExpressionLiteral readFrom(ProtoStreamReader reader) throws IOException {
        Object value = null;
        Class<?> expectedType = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case VALUE_INDEX:
                    value = reader.readAny();
                    break;
                case EXPECTED_TYPE_INDEX:
                    expectedType = reader.readAny(Class.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new ValueExpressionLiteral(value, expectedType);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ValueExpressionLiteral literal) throws IOException {
        Object value = getValue(literal);
        if (value != null) {
            writer.writeAny(VALUE_INDEX, value);
        }
        Class<?> expectedType = literal.getExpectedType();
        if (expectedType != null) {
            writer.writeAny(EXPECTED_TYPE_INDEX, expectedType);
        }
    }

    private static Object getValue(ValueExpressionLiteral literal) throws IOException {
        if (literal.getExpectedType() == null) {
            return literal.getValue(null);
        }
        Object[] objects = new Object[1];
        literal.writeExternal(new SimpleObjectOutput.Builder().with(objects).build());
        return objects[0];
    }
}
