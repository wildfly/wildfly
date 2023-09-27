/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
