/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.math;

import java.io.IOException;
import java.math.MathContext;
import java.math.RoundingMode;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link MathContext}.
 * @author Paul Ferraro
 */
public class MathContextMarshaller implements ProtoStreamMarshaller<MathContext> {

    private static final int PRECISION_INDEX = 1;
    private static final int ROUNDING_MODE_INDEX = 2;

    private static final int DEFAULT_PRECISION = 0;
    private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public MathContext readFrom(ProtoStreamReader reader) throws IOException {
        int precision = DEFAULT_PRECISION;
        RoundingMode mode = DEFAULT_ROUNDING_MODE;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case PRECISION_INDEX:
                    precision = reader.readUInt32();
                    break;
                case ROUNDING_MODE_INDEX:
                    mode = reader.readEnum(RoundingMode.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new MathContext(precision, mode);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, MathContext context) throws IOException {
        int precision = context.getPrecision();
        if (precision != DEFAULT_PRECISION) {
            writer.writeUInt32(PRECISION_INDEX, precision);
        }
        RoundingMode mode = context.getRoundingMode();
        if (mode != DEFAULT_ROUNDING_MODE) {
            writer.writeEnum(ROUNDING_MODE_INDEX, mode);
        }
    }

    @Override
    public Class<? extends MathContext> getJavaClass() {
        return MathContext.class;
    }
}
