/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.jboss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A marshalled value that is lazily serialized and deserialized on demand.
 * This implementation does not preserve the hash code of its object in serialized form.
 * @author Paul Ferraro
 */
public class SimpleMarshalledValue<T> implements MarshalledValue<T, MarshallingContext>, Serializable {
    private static final long serialVersionUID = -8852566958387608376L;

    private transient volatile MarshallingContext context;
    private transient volatile T object;
    private transient volatile ByteBuffer buffer;

    public SimpleMarshalledValue(T object, MarshallingContext context) {
        this.context = context;
        this.object = object;
    }

    SimpleMarshalledValue(ByteBuffer buffer) {
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
        int version = this.context.getCurrentVersion();
        ByteBufferOutputStream output = new ByteBufferOutputStream();
        ClassLoader loader = setThreadContextClassLoader(this.context.getClassLoader());
        try (SimpleDataOutput data = new SimpleDataOutput(Marshalling.createByteOutput(output))) {
            IndexSerializer.UNSIGNED_BYTE.writeInt(data, version);
            try (Marshaller marshaller = this.context.createMarshaller(version)) {
                marshaller.start(data);
                marshaller.writeObject(this.object);
                marshaller.finish();
                return output.getBuffer();
            }
        } finally {
            setThreadContextClassLoader(loader);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.wildfly.clustering.marshalling.spi.MarshalledValue#get(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized T get(MarshallingContext context) throws IOException, ClassNotFoundException {
        if (this.object == null) {
            this.context = context;
            if (this.buffer != null) {
                ByteArrayInputStream input = new ByteArrayInputStream(this.buffer.array(), this.buffer.arrayOffset(), this.buffer.limit() - this.buffer.arrayOffset());
                ClassLoader loader = setThreadContextClassLoader(this.context.getClassLoader());
                try (SimpleDataInput data = new SimpleDataInput(Marshalling.createByteInput(input))) {
                    int version = IndexSerializer.UNSIGNED_BYTE.readInt(data);
                    try (Unmarshaller unmarshaller = context.createUnmarshaller(version)) {
                        unmarshaller.start(data);
                        this.object = (T) unmarshaller.readObject();
                        unmarshaller.finish();
                        this.buffer = null; // Free up memory
                    }
                } finally {
                    setThreadContextClassLoader(loader);
                }
            }
        }
        return this.object;
    }

    /**
     * {@inheritDoc}
     * N.B. Calls to hashCode will return 0 if this marshalled value was deserialized but its object not yet rehydrated.
     * If consistent hashCode is required, use {@link HashableMarshalledValue instead}.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        Object object = this.object;
        return (object != null) ? object.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof SimpleMarshalledValue)) return false;
        @SuppressWarnings("unchecked")
        SimpleMarshalledValue<T> value = (SimpleMarshalledValue<T>) object;
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
        SimpleMarshalledValueExternalizer.writeBuffer(out, this.getBuffer());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.buffer = SimpleMarshalledValueExternalizer.readBuffer(in);
    }

    private static ClassLoader setThreadContextClassLoader(ClassLoader loader) {
        return (loader != null) ? WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader) : null;
    }
}
