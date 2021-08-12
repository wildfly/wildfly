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

import org.infinispan.protostream.ProtobufTagMarshaller;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * Adapts a {@link ProtobufTagMarshaller} to a {@link ProtoStreamMarshaller}.
 * @author Paul Ferraro
 */
public class ProtoStreamMarshallerAdapter<T> implements ProtoStreamMarshaller<T> {

    private final ProtobufTagMarshaller<T> marshaller;

    ProtoStreamMarshallerAdapter(ProtobufTagMarshaller<T> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.marshaller.getJavaClass();
    }

    @Override
    public String getTypeName() {
        return this.marshaller.getTypeName();
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        return this.read((ReadContext) reader);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        this.write((TagWriterImpl) ((WriteContext) writer).getWriter(), value);
    }

    @Override
    public T read(ReadContext context) throws IOException {
        return this.marshaller.read(context);
    }

    @Override
    public void write(WriteContext context, T value) throws IOException {
        this.marshaller.write(context, value);
    }
}
