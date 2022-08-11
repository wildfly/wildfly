/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * {@link MarshalledValue} implementation suitable for map keys that uses a {@link ByteBufferMarshaller}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledKey<T> extends ByteBufferMarshalledValue<T> {
    private static final long serialVersionUID = 7317347779979133897L;

    private transient volatile int hashCode;

    public ByteBufferMarshalledKey(T object, ByteBufferMarshaller marshaller) {
        super(object, marshaller);
        this.hashCode = Objects.hashCode(object);
    }

    public ByteBufferMarshalledKey(ByteBuffer buffer, int hashCode) {
        super(buffer);
        this.hashCode = hashCode;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object object) {
        // Optimize by verifying equality of hash code first
        if ((object == null) || !(object instanceof ByteBufferMarshalledKey) || (this.hashCode != object.hashCode())) return false;
        return super.equals(object);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(this.hashCode);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.hashCode = in.readInt();
    }
}
