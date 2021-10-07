/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
