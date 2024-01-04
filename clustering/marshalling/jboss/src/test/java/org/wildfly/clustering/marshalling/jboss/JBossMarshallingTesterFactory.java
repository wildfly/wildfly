/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferTestMarshaller;

/**
 * @author Paul Ferraro
 */
public enum JBossMarshallingTesterFactory implements MarshallingTesterFactory {
    INSTANCE;

    @Override
    public <T> MarshallingTester<T> createTester() {
        return new MarshallingTester<>(new ByteBufferTestMarshaller<>(TestJBossByteBufferMarshaller.INSTANCE));
    }
}
