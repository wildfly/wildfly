/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public enum TestProtoStreamByteBufferMarshaller implements ByteBufferMarshaller {
    INSTANCE;

    private final ByteBufferMarshaller marshaller;

    TestProtoStreamByteBufferMarshaller() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ImmutableSerializationContext context = new SerializationContextBuilder(new SimpleClassLoaderMarshaller(loader)).load(loader).build();
        this.marshaller = new ProtoStreamByteBufferMarshaller(context);
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.marshaller.isMarshallable(object);
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        return this.marshaller.readFrom(input);
    }

    @Override
    public void writeTo(OutputStream output, Object object) throws IOException {
        this.marshaller.writeTo(output, object);
    }

    @Override
    public OptionalInt size(Object object) {
        return this.marshaller.size(object);
    }
}
