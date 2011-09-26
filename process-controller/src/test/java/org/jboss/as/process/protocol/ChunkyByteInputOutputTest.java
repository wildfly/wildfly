/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.process.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshalling;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ChunkyByteInputOutputTest {

    @Test
    public void testEqualBuffer() throws Exception {
        final byte[] content = "1234567890".getBytes();

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ByteOutput byteOutput = new ChunkyByteOutput(Marshalling.createByteOutput(byteArrayOutputStream), 10);

        byteOutput.write(content);
        byteOutput.flush();

        final byte[] chunked = byteArrayOutputStream.toByteArray();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunked);
        final ByteInput byteInput = new ChunkyByteInput(Marshalling.createByteInput(byteArrayInputStream));
        byte[] result = new byte[content.length];
        byteInput.read(result);
        byteInput.close();

        Assert.assertArrayEquals(content, result);
        Assert.assertEquals(-1, byteInput.read());
    }

    @Test
    public void testMultiChunk() throws Exception {
        final byte[] content = "12345678901234567890123456789012345678901234567890".getBytes();

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ByteOutput byteOutput = new ChunkyByteOutput(Marshalling.createByteOutput(byteArrayOutputStream), 10);

        byteOutput.write(content);
        byteOutput.flush();

        final byte[] chunked = byteArrayOutputStream.toByteArray();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunked);
        final ByteInput byteInput = new ChunkyByteInput(Marshalling.createByteInput(byteArrayInputStream));
        byte[] result = new byte[content.length];
        byteInput.read(result);
        byteInput.close();

        Assert.assertArrayEquals(content, result);
        Assert.assertEquals(-1, byteInput.read());
    }

    @Test
    public void testRemainingBytes() throws Exception {
        final byte[] content = "1234567890123456789012345678901234567890123".getBytes();

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ByteOutput byteOutput = new ChunkyByteOutput(Marshalling.createByteOutput(byteArrayOutputStream), 10);

        byteOutput.write(content);
        byteOutput.flush();

        final byte[] chunked = byteArrayOutputStream.toByteArray();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunked);
        final ByteInput byteInput = new ChunkyByteInput(Marshalling.createByteInput(byteArrayInputStream));
        byte[] result = new byte[content.length];
        byteInput.read(result);
        byteInput.close();

        Assert.assertArrayEquals(content, result);
        Assert.assertEquals(-1, byteInput.read());
    }

    @Test
    public void testIncompleteRead() throws Exception {
        final byte[] content = "1234567890123456789012345678901234567890123".getBytes();

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ByteOutput byteOutput = new ChunkyByteOutput(Marshalling.createByteOutput(byteArrayOutputStream), 10);

        byteOutput.write(content);
        byteOutput.flush();

        final byte[] chunked = byteArrayOutputStream.toByteArray();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunked);
        final ByteInput byteInput = new ChunkyByteInput(Marshalling.createByteInput(byteArrayInputStream));
        int readLength = content.length - 15;
        byte[] result = new byte[readLength];
        byteInput.read(result);
        byteInput.close();

        byte[] expected = new byte[readLength];
        System.arraycopy(content, 0, expected, 0, readLength);

        Assert.assertArrayEquals(expected, result);
        Assert.assertEquals(-1, byteInput.read());
    }

    @Test
    public void testOffsetRead() throws Exception {
        final byte[] content = "1234567890123456789012345678901234567890123".getBytes();

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final ByteOutput byteOutput = new ChunkyByteOutput(Marshalling.createByteOutput(byteArrayOutputStream), 10);

        byteOutput.write(content);
        byteOutput.flush();

        final byte[] chunked = byteArrayOutputStream.toByteArray();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(chunked);
        final ByteInput byteInput = new ChunkyByteInput(Marshalling.createByteInput(byteArrayInputStream));

        int readLength = 5;
        byte[] result = new byte[content.length];
        byteInput.read(result, content.length - 6, readLength);
        byteInput.close();

        byte[] expected = new byte[content.length];
        System.arraycopy(content, 0, expected, content.length - 6, readLength);

        Assert.assertArrayEquals(expected, result);
        Assert.assertEquals(-1, byteInput.read());
    }
}
