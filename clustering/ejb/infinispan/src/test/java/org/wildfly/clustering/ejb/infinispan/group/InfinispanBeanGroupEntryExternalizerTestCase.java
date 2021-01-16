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

package org.wildfly.clustering.ejb.infinispan.group;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.spi.JavaByteBufferMarshaller;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * Unit test for {@link InfinispanBeanGroupEntryExternalizer}.
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupEntryExternalizerTestCase {

    @Test
    public void test() throws IOException {
        SessionID id = new UUIDSessionID(UUID.randomUUID());
        Map<SessionID, String> beans = Collections.singletonMap(id, "bean");
        InfinispanBeanGroupEntry<SessionID, String, ByteBufferMarshaller> entry = new InfinispanBeanGroupEntry<>(new ByteBufferMarshalledValue<>(beans, JavaByteBufferMarshaller.INSTANCE));
        new ExternalizerTester<>(new InfinispanBeanGroupEntryExternalizer<SessionID, String, ByteBufferMarshaller>()).test(entry, InfinispanBeanGroupEntryExternalizerTestCase::assertEquals);
    }

    static void assertEquals(InfinispanBeanGroupEntry<SessionID, String, ByteBufferMarshaller> entry1, InfinispanBeanGroupEntry<SessionID, String, ByteBufferMarshaller> entry2) {
        try {
            Assert.assertEquals(entry1.getBeans().get(JavaByteBufferMarshaller.INSTANCE), entry2.getBeans().get(JavaByteBufferMarshaller.INSTANCE));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
