/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferTestMarshaller;

/**
 * @author Paul Ferraro
 */
public enum ProtoStreamTesterFactory implements MarshallingTesterFactory {
    INSTANCE;

    @Override
    public <T> MarshallingTester<T> createTester() {
        return new MarshallingTester<>(new ByteBufferTestMarshaller<>(TestProtoStreamByteBufferMarshaller.INSTANCE));
    }
}
