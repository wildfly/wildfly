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

package org.wildfly.clustering.ejb.infinispan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Test;
import org.wildfly.clustering.ejb.infinispan.SessionIDSerializer.BasicSessionIDExternalizer;
import org.wildfly.clustering.ejb.infinispan.SessionIDSerializer.UUIDSessionIDExternalizer;
import org.wildfly.clustering.ejb.infinispan.SessionIDSerializer.UnknownSessionIDExternalizer;
import org.wildfly.clustering.marshalling.ExternalizerTester;

/**
 * Unit test for {@link SessionIDSerializer}.
 * @author Paul Ferraro
 */
public class SessionIDSerializerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        UUID uuid = UUID.randomUUID();

        new ExternalizerTester<>(new UUIDSessionIDExternalizer()).test(new UUIDSessionID(uuid));

        ByteBuffer buffer = ByteBuffer.wrap(new byte[20]);
        buffer.putInt(0x07000000);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        new ExternalizerTester<>(new BasicSessionIDExternalizer()).test(SessionID.createSessionID(buffer.array()));

        buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        new ExternalizerTester<>(new UnknownSessionIDExternalizer()).test(SessionID.createSessionID(buffer.array()));
    }
}
