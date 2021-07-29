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

import org.infinispan.protostream.TagReader;

/**
 * A {@link TagReader} with the additional ability to read an arbitrary embedded object.
 * @author Paul Ferraro
 */
public interface ProtoStreamReader extends ProtoStreamOperation, TagReader {

    /**
     * Reads an object of an arbitrary type from this reader.
     * @return a supplier of the unmarshalled object
     * @throws IOException if the object could not be read with the associated marshaller.
     */
    default Object readAny() throws IOException {
        return this.readObject(Any.class).get();
    }

    /**
     * Reads an object of an arbitrary type from this reader, cast to the specified type.
     * @param the expected type
     * @return a supplier of the unmarshalled object
     * @throws IOException if the object could not be read with the associated marshaller.
     */
    default <T> T readAny(Class<T> targetClass) throws IOException {
        return targetClass.cast(this.readAny());
    }

    /**
     * Reads an object of the specified type from this reader.
     * @param <T> the type of the associated marshaller
     * @param targetClass the class of the associated marshaller
     * @return the unmarshalled object
     * @throws IOException if no marshaller is associated with the specified class, or if the object could not be read with the associated marshaller.
     */
    <T> T readObject(Class<T> targetClass) throws IOException;

    /**
     * Reads an num of the specified type from this reader.
     * @param <T> the type of the associated marshaller
     * @param targetClass the class of the associated marshaller
     * @return the unmarshalled enum
     * @throws IOException if no marshaller is associated with the specified enum class, or if the enum could not be read with the associated marshaller.
     */
    default <E extends Enum<E>> E readEnum(Class<E> enumClass) throws IOException {
        EnumMarshaller<E> marshaller = (EnumMarshaller<E>) this.getSerializationContext().getMarshaller(enumClass);
        int code = this.readEnum();
        return marshaller.decode(code);
    }

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readUInt32()} or {@link #readSInt32()}
     */
    @Deprecated
    @Override
    int readInt32() throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readSFixed32()} instead.
     */
    @Deprecated
    @Override
    int readFixed32() throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readUInt64()} or {@link #readSInt64()}
     */
    @Deprecated
    @Override
    long readInt64() throws IOException;

    /**
     * Deprecated to discourage use.
     * @deprecated Use {@link #readSFixed64()} instead.
     */
    @Deprecated
    @Override
    long readFixed64() throws IOException;
}
