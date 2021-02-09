/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * Interface inherited by marshallable components.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public interface Marshallable<T> {

    /**
     * Reads an object from the specified reader.
     * @param reader a ProtoStream reader
     * @return the read object
     * @throws IOException if the object could not be read
     */
    T readFrom(ProtoStreamReader reader) throws IOException;

    /**
     * Writes the specified object to the specified writer.
     * @param writer a ProtoStream writer
     * @param value the object to be written
     * @throws IOException if the object could not be written
     */
    void writeTo(ProtoStreamWriter writer, T value) throws IOException;

    /**
     * Predicts the size of the specified object that would be written via the {@link #writeTo(ProtoStreamWriter, Object)} method.
     * @param context a serialization context
     * @param value the object whose size is to be determined
     * @return the computed size of the specified object, or empty if the size could not be determined.
     */
    default OptionalInt size(ImmutableSerializationContext context, T value) {
        SizeComputingProtoStreamWriter writer = new SizeComputingProtoStreamWriter(context);
        try {
            this.writeTo(writer, value);
            return writer.get();
        } catch (IOException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the type of object handled by this marshallable instance.
     * @return the type of object handled by this marshallable instance.
     */
    Class<? extends T> getJavaClass();
}
