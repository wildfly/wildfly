/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.protostream.TestProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapComputeFunctionMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Map<String, ByteBufferMarshalledValue<UUID>> map = new TreeMap<>();
        map.put("foo", new ByteBufferMarshalledValue<>(UUID.randomUUID(), TestProtoStreamByteBufferMarshaller.INSTANCE));
        map.put("bar", new ByteBufferMarshalledValue<>(UUID.randomUUID(), TestProtoStreamByteBufferMarshaller.INSTANCE));
        MarshallingTester<SessionAttributeMapComputeFunction<ByteBufferMarshalledValue<UUID>>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new SessionAttributeMapComputeFunction<>(map));
    }
}
