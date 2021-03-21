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

package org.wildfly.clustering.marshalling.protostream.math;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link BigDecimal}.
 * @author Paul Ferraro
 */
public class BigDecimalMarshaller implements ProtoStreamMarshaller<BigDecimal> {

    private static final int BIG_INTEGER_INDEX = 1;
    private static final int SCALE_INDEX = BIG_INTEGER_INDEX + BigIntegerMarshaller.INSTANCE.getFields();

    private static final int DEFAULT_SCALE = 0;

    @Override
    public BigDecimal readFrom(ProtoStreamReader reader) throws IOException {
        BigInteger unscaledValue = BigIntegerMarshaller.INSTANCE.getBuilder();
        int scale = DEFAULT_SCALE;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= BIG_INTEGER_INDEX && index < SCALE_INDEX) {
                unscaledValue = BigIntegerMarshaller.INSTANCE.readField(reader, index - BIG_INTEGER_INDEX, unscaledValue);
            } else if (index == SCALE_INDEX) {
                scale = reader.readSInt32();
            } else {
                reading = reader.ignoreField(tag);
            }
        }
        return new BigDecimal(unscaledValue, scale);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, BigDecimal value) throws IOException {
        BigIntegerMarshaller.INSTANCE.writeFields(writer, BIG_INTEGER_INDEX, value.unscaledValue());
        int scale = value.scale();
        if (scale != DEFAULT_SCALE) {
            writer.writeSInt32(SCALE_INDEX, scale);
        }
    }

    @Override
    public Class<? extends BigDecimal> getJavaClass() {
        return BigDecimal.class;
    }
}
