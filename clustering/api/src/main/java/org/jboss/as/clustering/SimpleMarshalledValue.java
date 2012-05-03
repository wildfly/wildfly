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

package org.jboss.as.clustering;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.marshalling.Unmarshaller;

/**
 * A non-hashable marshalled value, that is lazily serialized, but only deserialized on demand.
 * @author Paul Ferraro
 */
public class SimpleMarshalledValue<T> implements MarshalledValue<T, MarshallingContext>, Externalizable {
    private static final long serialVersionUID = -8852566958387608376L;

    private transient volatile MarshallingContext context;
    private transient volatile T object;
    private transient volatile byte[] bytes;

    public SimpleMarshalledValue(T object, MarshallingContext context) {
        this.context = context;
        this.object = object;
    }

    public SimpleMarshalledValue() {
        // Required for externalization
    }

    T peek() {
        return this.object;
    }

    byte[] getBytes() throws IOException {
        byte[] bytes = this.bytes;
        if (bytes != null) return bytes;
        if (this.object == null) return null;
        int version = this.context.getCurrentVersion();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SimpleDataOutput data = new SimpleDataOutput(Marshalling.createByteOutput(output));
        data.writeInt(version);
        Marshaller marshaller = this.context.createMarshaller(version);
        try {
            marshaller.start(data);

            // Workaround for AS7-2496
            ClassLoader currentLoader = null;
            ClassLoader contextLoader = context.getContextClassLoader(version);
            if (contextLoader != null) {
                currentLoader = getCurrentThreadContextClassLoader();
                setCurrentThreadContextClassLoader(contextLoader);
            }
            try {
                marshaller.writeObject(this.object);
            } finally {
                if (contextLoader != null) {
                    setCurrentThreadContextClassLoader(currentLoader);
                }
            }
            marshaller.finish();
            return output.toByteArray();
        } finally {
            marshaller.close();
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.MarshalledValue#get(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized T get(MarshallingContext context) throws IOException, ClassNotFoundException {
        if (this.object == null) {
            this.context = context;
            if (this.bytes != null) {
                ByteArrayInputStream input = new ByteArrayInputStream(this.bytes);
                SimpleDataInput data = new SimpleDataInput(Marshalling.createByteInput(input));
                int version = data.readInt();
                Unmarshaller unmarshaller = context.createUnmarshaller(version);
                try {
                    unmarshaller.start(data);
                    // Workaround for AS7-2496
                    ClassLoader currentLoader = null;
                    ClassLoader contextLoader = context.getContextClassLoader(version);
                    if (contextLoader != null) {
                        currentLoader = getCurrentThreadContextClassLoader();
                        setCurrentThreadContextClassLoader(contextLoader);
                    }
                    try {
                        this.object = (T) unmarshaller.readObject();
                    } finally {
                        if (contextLoader != null) {
                            setCurrentThreadContextClassLoader(currentLoader);
                        }
                    }
                    unmarshaller.finish();
                    this.bytes = null; // Free up memory
                } finally {
                    unmarshaller.close();
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
        return (this.object != null) ? this.object.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if ((object == null) || !(object instanceof SimpleMarshalledValue)) return false;
        @SuppressWarnings("unchecked")
        SimpleMarshalledValue<T> value = (SimpleMarshalledValue<T>) object;
        if ((this.object != null) && (value.object != null)) {
            return this.object.equals(value.object);
        }
        try {
            byte[] us = this.getBytes();
            byte[] them = value.getBytes();
            return ((us != null) && (them != null)) ? Arrays.equals(us, them) : (us == them);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        if (this.object != null) return this.object.toString();
        byte[] bytes = this.bytes;
        return (bytes != null) ? bytes.toString() : null;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        byte[] bytes = this.getBytes();
        if (bytes != null) {
            out.writeInt(bytes.length);
            out.write(bytes);
        } else {
            out.writeInt(0);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        int size = in.readInt();
        byte[] bytes = null;
        if (size > 0) {
            bytes = new byte[size];
            in.read(bytes);
        }
        this.bytes = bytes;
    }

    static ClassLoader getCurrentThreadContextClassLoader() {
        if(System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            PrivilegedAction<ClassLoader> action = new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            };
            return AccessController.doPrivileged(action);
        }
    }

    static void setCurrentThreadContextClassLoader(final ClassLoader loader) {
        if(System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(loader);
        } else {
            PrivilegedAction<Void> action = new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Thread.currentThread().setContextClassLoader(loader);
                    return null;
                }
            };
            AccessController.doPrivileged(action);
        }
    }
}
