/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.OptionalInt;
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
        this(marshaller, new ByteBufferMarshalledValueFactory(marshaller));
    }

    ByteBufferMarshalledValueFactoryTestCase(ByteBufferMarshaller marshaller, ByteBufferMarshalledValueFactory factory) {
        this.marshaller = marshaller;
        this.factory = factory;
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
        OptionalInt size = this.marshaller.size(value);
        ByteBuffer buffer = this.marshaller.write(value);
        if (size.isPresent()) {
            // Verify that computed size equals actual size
            assertEquals(size.getAsInt(), buffer.remaining());
        }
        ByteBufferMarshalledValue<V> result = (ByteBufferMarshalledValue<V>) this.marshaller.read(buffer);
        OptionalInt resultSize = this.marshaller.size(result);
        if (size.isPresent() && resultSize.isPresent()) {
            // Verify that computed size equals actual size
            assertEquals(size.getAsInt(), resultSize.getAsInt());
        }
        return result;
    }
}
