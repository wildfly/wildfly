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
import java.io.OutputStream;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataOutput;

/**
 * Simple implementation of the {@link ByteDataOutput} that delegates to a
 * {@link org.jboss.marshalling.SimpleDataOutput}.
 *
 * @author John Bailey
 */
public class SimpleByteDataOutput extends OutputStream implements ByteDataOutput {
    private final SimpleDataOutput output;

    public SimpleByteDataOutput(final OutputStream outputStream) {
        this.output = new SimpleDataOutput(Marshalling.createByteOutput(outputStream));
    }

    public void write(int b) throws IOException {
        output.write(b);
    }

    public void write(byte[] b) throws IOException {
        output.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    public void writeBoolean(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    public void writeByte(int v) throws IOException {
        output.writeByte(v);
    }

    public void writeShort(int v) throws IOException {
        output.writeShort(v);
    }

    public void writeChar(int v) throws IOException {
        output.writeChar(v);
    }

    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    public void writeBytes(String s) throws IOException {
        output.writeBytes(s);
    }

    public void writeChars(String s) throws IOException {
        output.writeChars(s);
    }

    public void writeUTF(String s) throws IOException {
        output.writeUTF(s);
    }

    public void close() throws IOException {
        output.close();
    }

    public void flush() throws IOException {
        output.flush();
    }
}
