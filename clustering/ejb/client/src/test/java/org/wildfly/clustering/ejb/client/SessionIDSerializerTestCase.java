/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Consumer;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.jupiter.params.ParameterizedTest;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.TesterFactory;
import org.wildfly.clustering.marshalling.junit.TesterFactorySource;

/**
 * Unit test for {@link SessionIDSerializer}.
 * @author Paul Ferraro
 */
public class SessionIDSerializerTestCase {

    @ParameterizedTest
    @TesterFactorySource(MarshallingTesterFactory.class)
    public void test(TesterFactory factory) {
        UUID uuid = UUID.randomUUID();
        Consumer<SessionID> tester = factory.createTester();

        tester.accept(new UUIDSessionID(uuid));

        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.putInt(0x07000000);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        tester.accept(SessionID.createSessionID(buffer.array()));

        buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        tester.accept(SessionID.createSessionID(buffer.array()));
    }
}
