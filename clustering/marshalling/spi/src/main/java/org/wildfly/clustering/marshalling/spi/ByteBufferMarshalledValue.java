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
import java.util.Objects;
import java.util.OptionalInt;

import org.jboss.logging.Logger;

/**
 * {@link MarshalledValue} implementation that uses a {@link ByteBufferMarshaller}.
 * @author Paul Ferraro
 * @param <T> the type wrapped by this marshalled value
 */
public class ByteBufferMarshalledValue<T> implements MarshalledValue<T, ByteBufferMarshaller>, Serializable {
    private static final long serialVersionUID = -8419893544424515905L;
    private static final Logger LOGGER = Logger.getLogger(ByteBufferMarshalledValue.class);

    private transient volatile ByteBufferMarshaller marshaller;
    private transient volatile T object;
    private transient volatile ByteBuffer buffer;

    /**
     * Constructs a marshalled value from the specified object and marshaller.
     * @param object the wrapped object
     * @param marshaller a marshaller suitable for marshalling the specified object
     */
    public ByteBufferMarshalledValue(T object, ByteBufferMarshaller marshaller) {
        this.marshaller = marshaller;
        this.object = object;
    }

    /**
     * Constructs a marshalled value from the specified byte buffer.
     * This constructor is only public to facilitate marshallers of this object (from other packages).
     * The byte buffer parameter must not be read outside the context of this object.
     * @param buffer a byte buffer
     */
    public ByteBufferMarshalledValue(ByteBuffer buffer) {
        // Normally, we would create a defensive ByteBuffer.asReadOnlyBuffer()
        // but this would preclude the use of operations on the backing array.
        this.buffer = buffer;
    }

    // Used for testing purposes only
    T peek() {
        return this.object;
    }

    public synchronized boolean isEmpty() {
        return (this.buffer == null) && (this.object == null);
    }

    public synchronized ByteBuffer getBuffer() throws IOException {
        ByteBuffer buffer = this.buffer;
        if ((buffer == null) && (this.object != null)) {
            // Since the wrapped object is likely mutable, we cannot cache the generated buffer
            buffer = this.marshaller.write(this.object);
            // N.B. Refrain from logging wrapped object
            // If wrapped object contains an EJB proxy, toString() will trigger an EJB invocation!
            LOGGER.debugf("Marshalled size of %s object = %d bytes", this.object.getClass().getCanonicalName(), buffer.limit() - buffer.arrayOffset());
        }
        return buffer;
    }

    public synchronized OptionalInt size() {
        // N.B. Buffer position is guarded by synchronization on this object
        // We invalidate buffer upon reading it, ensuring that ByteBuffer.remaining() returns the effective buffer size
        return (this.buffer != null) ? OptionalInt.of(this.buffer.remaining()) : this.marshaller.size(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized T get(ByteBufferMarshaller marshaller) throws IOException {
        if (this.object == null) {
            this.marshaller = marshaller;
            if (this.buffer != null) {
                // Invalidate buffer after reading object
                this.object = (T) this.marshaller.read(this.buffer);
                this.buffer = null;
            }
        }
        return this.object;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.object);
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
        // N.B. Refrain from logging wrapped object
        // If wrapped object contains an EJB proxy, toString() will trigger an EJB invocation!
        return String.format("%s [%s]", this.getClass().getName(), (this.object != null) ? this.object.getClass().getName() : "<serialized>");
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
