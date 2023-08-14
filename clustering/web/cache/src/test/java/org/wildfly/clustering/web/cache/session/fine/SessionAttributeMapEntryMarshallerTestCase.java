/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.protostream.TestProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;

/**
 * Validates marshalling of {@link SessionAttributeMapEntry}.
 * @author Paul Ferraro
 */
public class SessionAttributeMapEntryMarshallerTestCase {

    @Test
    public void test() throws IOException {
        ByteBufferMarshalledValue<UUID> value = new ByteBufferMarshalledValue<>(UUID.randomUUID(), TestProtoStreamByteBufferMarshaller.INSTANCE);
        MarshallingTester<SessionAttributeMapEntry<ByteBufferMarshalledValue<UUID>>> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new SessionAttributeMapEntry<>("foo", value));
    }
}
