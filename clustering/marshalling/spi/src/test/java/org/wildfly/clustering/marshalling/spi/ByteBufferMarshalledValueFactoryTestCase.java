/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.clustering.marshalling.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

/**
 * Unit tests for {@link ByteBufferMarshalledValue}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledValueFactoryTestCase {

    private final ByteBufferMarshaller marshaller;
    private final ByteBufferMarshalledValueFactory factory;

    public ByteBufferMarshalledValueFactoryTestCase() {
        this(JavaByteBufferMarshaller.INSTANCE);
    }

    protected ByteBufferMarshalledValueFactoryTestCase(ByteBufferMarshaller marshaller) {
        this.marshaller = marshaller;
        this.factory = new ByteBufferMarshalledValueFactory(marshaller);
    }

    @Test
    public void get() throws Exception {
        UUID uuid = UUID.randomUUID();
        ByteBufferMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);

        assertNotNull(mv.peek());
        assertSame(uuid, mv.peek());
        assertSame(uuid, mv.get(this.marshaller));

        ByteBufferMarshalledValue<UUID> copy = replicate(mv);

        assertNull(copy.peek());

        UUID uuid2 = copy.get(this.marshaller);
        assertNotSame(uuid, uuid2);
        assertEquals(uuid, uuid2);

        copy = replicate(copy);
        uuid2 = copy.get(this.marshaller);
        assertEquals(uuid, uuid2);

        mv = this.factory.createMarshalledValue(null);
        assertNull(mv.peek());
        assertNull(mv.getBuffer());
        assertNull(mv.get(this.marshaller));
    }

    @Test
    public void equals() throws Exception {
        UUID uuid = UUID.randomUUID();
        ByteBufferMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);

        assertTrue(mv.equals(mv));
        assertFalse(mv.equals(null));

        ByteBufferMarshalledValue<UUID> dup = this.factory.createMarshalledValue(uuid);
        assertTrue(mv.equals(dup));
        assertTrue(dup.equals(mv));

        ByteBufferMarshalledValue<UUID> replica = replicate(mv);
        assertTrue(mv.equals(replica));
        assertTrue(replica.equals(mv));

        ByteBufferMarshalledValue<UUID> nulled = this.factory.createMarshalledValue(null);
        assertFalse(mv.equals(nulled));
        assertFalse(nulled.equals(mv));
        assertFalse(replica.equals(nulled));
        assertFalse(nulled.equals(replica));
        assertTrue(nulled.equals(nulled));
        assertFalse(nulled.equals(null));
        assertTrue(nulled.equals(this.factory.createMarshalledValue(null)));
    }

    @Test
    public void testHashCode() throws Exception {
        UUID uuid = UUID.randomUUID();
        ByteBufferMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);
        assertEquals(uuid.hashCode(), mv.hashCode());

        ByteBufferMarshalledValue<UUID> copy = replicate(mv);
        assertEquals(0, copy.hashCode());

        mv = this.factory.createMarshalledValue(null);
        assertEquals(0, mv.hashCode());
    }

    @SuppressWarnings("unchecked")
    <V> ByteBufferMarshalledValue<V> replicate(ByteBufferMarshalledValue<V> value) throws IOException {
        ByteBuffer buffer = this.marshaller.write(value);
        return (ByteBufferMarshalledValue<V>) this.marshaller.read(buffer);
    }
}
