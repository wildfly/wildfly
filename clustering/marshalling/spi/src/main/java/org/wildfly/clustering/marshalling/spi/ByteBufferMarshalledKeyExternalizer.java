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
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link ByteBufferMarshalledKey}.
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledKeyExternalizer implements Externalizer<ByteBufferMarshalledKey<Object>> {

    @Override
    public void writeObject(ObjectOutput output, ByteBufferMarshalledKey<Object> value) throws IOException {
        ByteBufferMarshalledValueExternalizer.writeBuffer(output, value.getBuffer());
        output.writeInt(value.hashCode());
    }

    @Override
    public ByteBufferMarshalledKey<Object> readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return new ByteBufferMarshalledKey<>(ByteBufferMarshalledValueExternalizer.readBuffer(input), input.readInt());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<ByteBufferMarshalledKey<Object>> getTargetClass() {
        return (Class<ByteBufferMarshalledKey<Object>>) (Class<?>) ByteBufferMarshalledKey.class;
    }
}
