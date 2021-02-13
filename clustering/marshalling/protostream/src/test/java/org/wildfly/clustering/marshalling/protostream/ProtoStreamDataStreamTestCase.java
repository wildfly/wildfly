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

package org.wildfly.clustering.marshalling.protostream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.common.function.ExceptionObjIntConsumer;
import org.wildfly.common.function.ExceptionToIntFunction;

/**
 * Unit tests for non-trivial methods of ProtoStreamDataInput/ProtoStreamDataOutput.
 * @author Paul Ferraro
 */
public class ProtoStreamDataStreamTestCase {

    @Test
    public void testByte() throws IOException {
        test(Byte.SIZE, true, DataOutput::writeByte, DataInput::readByte);
    }

    @Test
    public void testUnsignedByte() throws IOException {
        test(Byte.SIZE, false, DataOutput::writeByte, DataInput::readUnsignedByte);
    }

    @Test
    public void testShort() throws IOException {
        test(Short.SIZE, true, DataOutput::writeShort, DataInput::readShort);
    }

    @Test
    public void testUnsignedShort() throws IOException {
        test(Short.SIZE, false, DataOutput::writeShort, DataInput::readUnsignedShort);
    }

    @Test
    public void testInt() throws IOException {
        test(Integer.SIZE, true, DataOutput::writeInt, DataInput::readInt);
    }

    private static void test(int bits, boolean signed, ExceptionObjIntConsumer<DataOutput, IOException> write, ExceptionToIntFunction<DataInput, IOException> read) throws IOException {
        for (int i = 0; i <= bits - (signed ? 1 : 0); ++i) {
            testInt((1 << i) - 1, write, read);
            if (signed) {
                testInt(-1 << i, write, read);
            }
        }
    }

    private static void testInt(int i, ExceptionObjIntConsumer<DataOutput, IOException> write, ExceptionToIntFunction<DataInput, IOException> read) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Integer.SIZE);
        RawProtoStreamWriter writer = RawProtoStreamWriterImpl.newInstance(bytes);
        DataOutput output = new ProtoStreamDataOutput(null, writer);
        write.accept(output, i);
        writer.flush();
        DataInput input = new ProtoStreamDataInput(null, RawProtoStreamReaderImpl.newInstance(new ByteArrayInputStream(bytes.toByteArray())));
        Assert.assertEquals(i, read.apply(input));
    }

    @Test
    public void testByteArray() throws IOException {
        UUID id = UUID.randomUUID();
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * 2);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        byte[] source = buffer.array();
        byte[] test = new byte[source.length + 2];
        System.arraycopy(source, 0, test, 1, source.length);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        RawProtoStreamWriter writer = RawProtoStreamWriterImpl.newInstance(bytes);
        DataOutput output = new ProtoStreamDataOutput(null, writer);
        output.write(test, 1, source.length);
        writer.flush();
        DataInput input = new ProtoStreamDataInput(null, RawProtoStreamReaderImpl.newInstance(new ByteArrayInputStream(bytes.toByteArray())));
        byte[] result = new byte[source.length];
        input.readFully(result);
        Assert.assertArrayEquals(source, result);
    }
}
