/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledValue<T> implements MarshalledValue<T, ByteBufferMarshaller>, Serializable {
    private static final long serialVersionUID = -8419893544424515905L;

    private transient volatile ByteBufferMarshaller marshaller;
    private transient volatile T object;
    private transient volatile ByteBuffer buffer;

    public ByteBufferMarshalledValue(T object, ByteBufferMarshaller marshaller) {
        this.marshaller = marshaller;
        this.object = object;
    }

    ByteBufferMarshalledValue(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    // Used for testing purposes only
    T peek() {
        return this.object;
    }

    synchronized ByteBuffer getBuffer() throws IOException {
        ByteBuffer buffer = this.buffer;
        if (buffer != null) return buffer;
        if (this.object == null) return null;
        return this.marshaller.write(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized T get(ByteBufferMarshaller marshaller) throws IOException {
        if (this.object == null) {
            this.marshaller = marshaller;
            if (this.buffer != null) {
                this.object = (T) this.marshaller.read(this.buffer);
                this.buffer = null;
            }
        }
        return this.object;
    }

    @Override
    public int hashCode() {
        Object object = this.object;
        return (object != null) ? object.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof ByteBufferMarshalledValue)) return false;
        @SuppressWarnings("unchecked")
        ByteBufferMarshalledValue<T> value = (ByteBufferMarshalledValue<T>) object;
        Object ourObject = this.object;
        Object theirObject = value.object;
        if ((ourObject != null) && (theirObject != null)) {
            return ourObject.equals(theirObject);
        }
        try {
            ByteBuffer us = this.getBuffer();
            ByteBuffer them = value.getBuffer();
            return ((us != null) && (them != null)) ? us.equals(them) : (us == them);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        Object object = this.object;
        if (object != null) return object.toString();
        ByteBuffer buffer = this.buffer;
        return (buffer != null) ? buffer.toString() : null;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ByteBufferMarshalledValueExternalizer.writeBuffer(out, this.getBuffer());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.buffer = ByteBufferMarshalledValueExternalizer.readBuffer(in);
    }
}
