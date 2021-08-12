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

import org.infinispan.protostream.descriptors.WireType;

/**
 * Provider for a {@link ScalarMarshaller}.
 * @author Paul Ferraro
 */
public interface ScalarMarshallerProvider extends ScalarMarshaller<Object> {
    ScalarMarshaller<?> getMarshaller();

    @Override
    default Class<? extends Object> getJavaClass() {
        return this.getMarshaller().getJavaClass();
    }

    @Override
    default WireType getWireType() {
        return this.getMarshaller().getWireType();
    }

    @Override
    default Object readFrom(ProtoStreamReader reader) throws IOException {
        return this.getMarshaller().readFrom(reader);
    }

    @Override
    default void writeTo(ProtoStreamWriter writer, Object value) throws IOException {
        this.cast(Object.class).writeTo(writer, value);
    }

    @SuppressWarnings("unchecked")
    default <T> ScalarMarshaller<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.getJavaClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (ScalarMarshaller<T>) this.getMarshaller();
    }
}
