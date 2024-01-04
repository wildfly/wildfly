/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Test;
import org.wildfly.clustering.ejb.client.SessionIDSerializer.BasicSessionIDExternalizer;
import org.wildfly.clustering.ejb.client.SessionIDSerializer.UUIDSessionIDExternalizer;
import org.wildfly.clustering.ejb.client.SessionIDSerializer.UnknownSessionIDExternalizer;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * Unit test for {@link SessionIDSerializer}.
 * @author Paul Ferraro
 */
public class SessionIDSerializerTestCase {

    @Test
    public void testProtoStream() throws IOException {
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    @Test
    public void testExternalizer() throws IOException {
        test(new ExternalizerTesterFactory(new UUIDSessionIDExternalizer(), new BasicSessionIDExternalizer(), new UnknownSessionIDExternalizer()).createTester());
    }

    private static void test(Tester<SessionID> tester) throws IOException {
        UUID uuid = UUID.randomUUID();

        tester.test(new UUIDSessionID(uuid));

        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putInt(0x07000000);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        tester.test(SessionID.createSessionID(buffer.array()));

        buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        tester.test(SessionID.createSessionID(buffer.array()));
    }
}
