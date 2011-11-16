/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.protocol.mgmt;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Emanuel Muckenhuber
 */
public class FlushableDataOutputImpl2 implements FlushableDataOutput {

    private final DataOutputStream delegate;

    public FlushableDataOutputImpl2(DataOutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        delegate.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        delegate.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        delegate.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        delegate.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        delegate.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        delegate.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        delegate.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        delegate.writeDouble(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        delegate.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        delegate.writeChars(s);
    }

    @Override
    public void writeUTF(String str) throws IOException {
        delegate.writeUTF(str);
    }

    public int size() {
        return delegate.size();
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
