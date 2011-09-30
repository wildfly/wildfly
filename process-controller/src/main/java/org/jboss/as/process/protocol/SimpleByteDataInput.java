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

import java.io.IOException;
import java.io.InputStream;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;

/**
 * Simple implementation of the {@link ByteDataInput} that delegates to a
 * {@link org.jboss.marshalling.SimpleDataInput}.
 *
 * @author John Bailey
 */
public class SimpleByteDataInput extends InputStream implements ByteDataInput {
    private final SimpleDataInput input;

    public SimpleByteDataInput(final InputStream inputStream) {
        this.input = new SimpleDataInput(Marshalling.createByteInput(inputStream));
    }

    public int read() throws IOException {
        return input.read();
    }

    public int read(byte[] b) throws IOException {
        return input.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return input.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        return input.skip(n);
    }

    public int available() throws IOException {
        return input.available();
    }

    public void readFully(byte[] b) throws IOException {
        input.readFully(b);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
        input.readFully(b, off, len);
    }

    public int skipBytes(int n) throws IOException {
        return input.skipBytes(n);
    }

    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    public byte readByte() throws IOException {
        return input.readByte();
    }

    public int readUnsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    public short readShort() throws IOException {
        return input.readShort();
    }

    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
    }

    public char readChar() throws IOException {
        return input.readChar();
    }

    public int readInt() throws IOException {
        return input.readInt();
    }

    public long readLong() throws IOException {
        return input.readLong();
    }

    public float readFloat() throws IOException {
        return input.readFloat();
    }

    public double readDouble() throws IOException {
        return input.readDouble();
    }

    public String readLine() throws IOException {
        return input.readLine();
    }

    public String readUTF() throws IOException {
        return input.readUTF();
    }

    public void close() throws IOException {
        input.close();
    }
}
