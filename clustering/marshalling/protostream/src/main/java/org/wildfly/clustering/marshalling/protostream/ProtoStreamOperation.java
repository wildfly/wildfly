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

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.descriptors.WireType;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * Common interface of {@link ProtoStreamReader} and {@link ProtoStreamWriter}.
 * @author Paul Ferraro
 */
public interface ProtoStreamOperation {

    interface Context {
        /**
         * Records a the specified object reference, in case it is referenced again within a stream.
         * This method is idempotent.
         * @param object an object reference
         */
        void addReference(Object object);
    }

    /**
     * Returns the serialization context of the associated marshaller.
     * @return an immutable serialization context
     */
    ImmutableSerializationContext getSerializationContext();

    /**
     * Returns a marshaller suitable of marshalling an object of the specified type.
     * @param <T> the type of the associated marshaller
     * @param <V> the type of the object to be marshalled
     * @param javaClass the type of the value to be written.
     * @return a marshaller suitable for the specified type
     * @throws IllegalArgumentException if no suitable marshaller exists
     */
    @SuppressWarnings("unchecked")
    default <T, V extends T> ProtoStreamMarshaller<T> findMarshaller(Class<V> javaClass) {
        ImmutableSerializationContext context = this.getSerializationContext();
        Class<?> targetClass = javaClass;
        IllegalArgumentException exception = null;
        while (targetClass != null) {
            try {
                return (ProtoStreamMarshaller<T>) context.getMarshaller((Class<T>) targetClass);
            } catch (IllegalArgumentException e) {
                // If no marshaller was found, check super class
                if (exception == null) {
                    exception = e;
                }
                targetClass = targetClass.getSuperclass();
            }
        }
        throw exception;
    }

    /**
     * Returns the marshalled size of the protobuf tag containing the specified field index and wire type.
     * @param index a field index
     * @param type a wire type
     * @return the marshalled size of the protobuf tag
     */
    default int tagSize(int index, WireType type) {
        return this.varIntSize(WireType.makeTag(index, type));
    }

    /**
     * Returns the marshalled size of the specified variable-width integer.
     * @param index a variable-width integer
     * @return the marshalled size of the specified variable-width integer.
     */
    default int varIntSize(int value) {
        TagWriterImpl writer = TagWriterImpl.newInstance(this.getSerializationContext());
        try {
            writer.writeVarint32(value);
            return writer.getWrittenBytes();
        } catch (IOException e) {
            return WireType.MAX_VARINT_SIZE;
        }
    }
}
