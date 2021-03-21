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
import java.math.BigInteger;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for fields of a {@link BigInteger}.
 * @author Paul Ferraro
 */
public enum BigIntegerMarshaller implements FieldSetMarshaller<BigInteger, BigInteger> {
    INSTANCE;

    private static final int POSITIVE_MAGNITUDE_INDEX = 0;
    private static final int NEGATIVE_MAGNITUDE_INDEX = 1;
    private static final int FIELDS = 2;

    @Override
    public BigInteger getBuilder() {
        return BigInteger.ZERO;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public BigInteger readField(ProtoStreamReader reader, int index, BigInteger value) throws IOException {
        switch (index) {
            case POSITIVE_MAGNITUDE_INDEX:
                return new BigInteger(reader.readByteArray());
            case NEGATIVE_MAGNITUDE_INDEX:
                return new BigInteger(reader.readByteArray()).negate();
            default:
                return value;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, BigInteger value) throws IOException {
        int sigNum = value.signum();
        if (sigNum != 0) {
            writer.writeBytes(startIndex + (sigNum > 0 ? POSITIVE_MAGNITUDE_INDEX : NEGATIVE_MAGNITUDE_INDEX), value.abs().toByteArray());
        }
    }
}
