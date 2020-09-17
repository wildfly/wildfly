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

import java.io.IOException;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;

/**
 * Provides a {@link ProtoStreamMarshaller}.
 * @author Paul Ferraro
 */
public interface MarshallerProvider extends ProtoStreamMarshaller<Object> {
    ProtoStreamMarshaller<?> getMarshaller();

    @Override
    default Object readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        return this.getMarshaller().readFrom(context, reader);
    }

    @Override
    default void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Object object) throws IOException {
        this.cast(Object.class).writeTo(context, writer, object);
    }

    @Override
    default OptionalInt size(ImmutableSerializationContext context, Object value) {
        return this.cast(Object.class).size(context, value);
    }

    @Override
    default Class<? extends Object> getJavaClass() {
        return this.getMarshaller().getJavaClass();
    }

    @SuppressWarnings("unchecked")
    default <T> ProtoStreamMarshaller<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.getJavaClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (ProtoStreamMarshaller<T>) this.getMarshaller();
    }
}
