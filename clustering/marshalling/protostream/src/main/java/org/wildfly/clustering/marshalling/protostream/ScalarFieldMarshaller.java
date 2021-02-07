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
 * A field marshaller based on a scaler marshaller.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public class ScalarFieldMarshaller<T> implements FieldMarshaller<T> {

    private final ScalarMarshaller<T> marshaller;

    public ScalarFieldMarshaller(ScalarMarshaller<T> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T result = this.marshaller.readFrom(reader);
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            reading = (tag != 0) && reader.skipField(tag);
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        this.marshaller.writeTo(writer, value);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, T value) {
        return this.marshaller.size(context, value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.marshaller.getJavaClass();
    }

    @Override
    public int getWireType() {
        return this.marshaller.getWireType();
    }
}
