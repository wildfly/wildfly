/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.protocol.mgmt;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jboss.marshalling.SimpleDataOutput;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class FlushableDataOutputImpl implements FlushableDataOutput, Closeable {

    private final SimpleDataOutput output;

    private FlushableDataOutputImpl(SimpleDataOutput output) {
        this.output = output;
    }

    static FlushableDataOutput create(OutputStream output) {
        return new FlushableDataOutputImpl2(new DataOutputStream(output));
        // return new FlushableDataOutputImpl(new SimpleDataOutput(Marshalling.createByteOutput(output)));
    }

    /**
     * @return
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return output.hashCode();
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#write(int)
     */
    public void write(int v) throws IOException {
        output.write(v);
    }

    /**
     * @param bytes
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#write(byte[])
     */
    public void write(byte[] bytes) throws IOException {
        output.write(bytes);
    }

    /**
     * @param bytes
     * @param off
     * @param len
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#write(byte[], int, int)
     */
    public void write(byte[] bytes, int off, int len) throws IOException {
        output.write(bytes, off, len);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeBoolean(boolean)
     */
    public void writeBoolean(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeByte(int)
     */
    public void writeByte(int v) throws IOException {
        output.writeByte(v);
    }

    /**
     * @param obj
     * @return
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        return output.equals(obj);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeShort(int)
     */
    public void writeShort(int v) throws IOException {
        output.writeShort(v);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeChar(int)
     */
    public void writeChar(int v) throws IOException {
        output.writeChar(v);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeInt(int)
     */
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeLong(long)
     */
    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeFloat(float)
     */
    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    /**
     * @param v
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeDouble(double)
     */
    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return output.toString();
    }

    /**
     * @param s
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeBytes(java.lang.String)
     */
    public void writeBytes(String s) throws IOException {
        output.writeBytes(s);
    }

    /**
     * @param s
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeChars(java.lang.String)
     */
    public void writeChars(String s) throws IOException {
        output.writeChars(s);
    }

    /**
     * @param s
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#writeUTF(java.lang.String)
     */
    public void writeUTF(String s) throws IOException {
        output.writeUTF(s);
    }

    /**
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#flush()
     */
    public void flush() throws IOException {
        output.flush();
    }

    /**
     * @throws IOException
     * @see org.jboss.marshalling.SimpleDataOutput#close()
     */
    public void close() throws IOException {
        output.close();
    }
}
