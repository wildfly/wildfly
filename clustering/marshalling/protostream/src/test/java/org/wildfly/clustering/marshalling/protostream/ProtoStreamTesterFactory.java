/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.List;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferTestMarshaller;

/**
 * @author Paul Ferraro
 */
public enum ProtoStreamTesterFactory implements MarshallingTesterFactory {
    INSTANCE;

    public static <T> MarshallingTester<T> createTester(Iterable<SerializationContextInitializer> initializers) {
        return new MarshallingTester<>(new ByteBufferTestMarshaller<>(new TestProtoStreamByteBufferMarshallerFactory(initializers).get()));
    }

    @Override
    public <T> MarshallingTester<T> createTester() {
        return createTester(List.of());
    }
}
