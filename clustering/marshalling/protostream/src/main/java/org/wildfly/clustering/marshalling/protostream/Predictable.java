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

package org.wildfly.clustering.marshalling.protostream;

import java.util.OptionalInt;

import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.ImmutableSerializationContext;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Implemented by objects that can predict their marshalling sizes
 * @author Paul Ferraro
 */
public interface Predictable<T> {

    /**
     * Returns the size of the buffer to use for marshalling the specified object, if known.
     * @param context the serialization context
     * @param value the value to be marshalled.
     * @return the buffer size (in bytes), or empty if unknown.
     */
    default OptionalInt size(ImmutableSerializationContext context, T value) {
        return OptionalInt.empty();
    }

    static OptionalInt computeSizeNoTag(ImmutableSerializationContext context, Object value) {
        BaseMarshaller<?> marshaller = context.getMarshaller(value.getClass());
        @SuppressWarnings("unchecked")
        OptionalInt size = (marshaller instanceof Predictable) ? ((Predictable<Object>) marshaller).size(context, value) : OptionalInt.empty();
        return size.isPresent() ? OptionalInt.of(CodedOutputStream.computeUInt32SizeNoTag(size.getAsInt()) + size.getAsInt()) : size;
    }

    static OptionalInt computeSize(ImmutableSerializationContext context, int fieldNumber, Object value) {
        OptionalInt size = computeSizeNoTag(context, value);
        return size.isPresent() ? OptionalInt.of(CodedOutputStream.computeTagSize(fieldNumber) + size.getAsInt()) : size;
    }
}
