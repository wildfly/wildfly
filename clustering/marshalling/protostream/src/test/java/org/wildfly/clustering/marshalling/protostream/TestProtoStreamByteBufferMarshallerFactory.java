/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.List;
import java.util.function.Supplier;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public class TestProtoStreamByteBufferMarshallerFactory implements Supplier<ByteBufferMarshaller> {

    private final ByteBufferMarshaller marshaller;

    public TestProtoStreamByteBufferMarshallerFactory() {
        this(List.of());
    }

    public TestProtoStreamByteBufferMarshallerFactory(Iterable<SerializationContextInitializer> initializers) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ImmutableSerializationContext context = new SerializationContextBuilder(new SimpleClassLoaderMarshaller(loader)).load(loader).register(initializers).build();
        this.marshaller = new ProtoStreamByteBufferMarshaller(context);
    }

    @Override
    public ByteBufferMarshaller get() {
        return this.marshaller;
    }
}
