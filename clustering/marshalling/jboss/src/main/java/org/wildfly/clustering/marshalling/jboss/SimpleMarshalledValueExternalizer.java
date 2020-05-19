/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(Externalizer.class)
public class SimpleMarshalledValueExternalizer<T> implements Externalizer<SimpleMarshalledValue<T>> {

    static ByteBuffer readBuffer(ObjectInput input) throws IOException {
        int size = IndexSerializer.VARIABLE.readInt(input);
        byte[] bytes = (size > 0) ? new byte[size] : null;
        if (bytes != null) {
            input.readFully(bytes);
        }
        return (bytes != null) ? ByteBuffer.wrap(bytes) : null;
    }

    static void writeBuffer(ObjectOutput output, ByteBuffer buffer) throws IOException {
        int length = (buffer != null) ? buffer.limit() - buffer.arrayOffset() : 0;
        IndexSerializer.VARIABLE.writeInt(output, length);
        if (length > 0) {
            output.write(buffer.array(), buffer.arrayOffset(), length);
        }
    }

    @Override
    public SimpleMarshalledValue<T> readObject(ObjectInput input) throws IOException {
        return new SimpleMarshalledValue<>(readBuffer(input));
    }

    @Override
    public void writeObject(ObjectOutput output, SimpleMarshalledValue<T> object) throws IOException {
        writeBuffer(output, object.getBuffer());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<SimpleMarshalledValue<T>> getTargetClass() {
        return (Class<SimpleMarshalledValue<T>>) (Class<?>) SimpleMarshalledValue.class;
    }
}
