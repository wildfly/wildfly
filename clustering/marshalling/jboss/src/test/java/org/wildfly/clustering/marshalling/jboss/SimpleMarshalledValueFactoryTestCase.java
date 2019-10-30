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
package org.wildfly.clustering.marshalling.jboss;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.junit.Test;

/**
 * Unit tests for SimpleMarshalledValue.
 *
 * @author Brian Stansberry
 */
public class SimpleMarshalledValueFactoryTestCase {
    private final MarshallingContext context;
    private final SimpleMarshalledValueFactory factory;

    public SimpleMarshalledValueFactoryTestCase() {
        MarshallingConfigurationRepository repository = new MarshallingConfigurationRepository() {
            @Override
            public int getCurrentMarshallingVersion() {
                return 0;
            }

            @Override
            public MarshallingConfiguration getMarshallingConfiguration(int version) {
                assertEquals(0, version);
                return new MarshallingConfiguration();
            }
        };
        this.context = new SimpleMarshallingContext(Marshalling.getMarshallerFactory("river", Marshalling.class.getClassLoader()), repository, Thread.currentThread().getContextClassLoader());
        this.factory = this.createFactory(this.context);
    }

    SimpleMarshalledValueFactory createFactory(MarshallingContext context) {
        return new SimpleMarshalledValueFactory(context);
    }

    /**
     * Test method for {@link org.jboss.ha.framework.server.SimpleMarshalledValue#get()}.
     */
    @Test
    public void get() throws Exception {
        UUID uuid = UUID.randomUUID();
        SimpleMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);

        assertNotNull(mv.peek());
        assertSame(uuid, mv.peek());
        assertSame(uuid, mv.get(this.context));

        SimpleMarshalledValue<UUID> copy = replicate(mv);

        assertNull(copy.peek());

        UUID uuid2 = copy.get(this.context);
        assertNotSame(uuid, uuid2);
        assertEquals(uuid, uuid2);

        copy = replicate(copy);
        uuid2 = copy.get(this.context);
        assertEquals(uuid, uuid2);

        mv = this.factory.createMarshalledValue(null);
        assertNull(mv.peek());
        assertNull(mv.getBytes());
        assertNull(mv.get(this.context));
    }

    /**
     * Test method for {@link org.jboss.ha.framework.server.SimpleMarshalledValue#equals(java.lang.Object)}.
     */
    @Test
    public void equals() throws Exception {
        UUID uuid = UUID.randomUUID();
        SimpleMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);

        assertTrue(mv.equals(mv));
        assertFalse(mv.equals(null));

        SimpleMarshalledValue<UUID> dup = this.factory.createMarshalledValue(uuid);
        assertTrue(mv.equals(dup));
        assertTrue(dup.equals(mv));

        SimpleMarshalledValue<UUID> replica = replicate(mv);
        assertTrue(mv.equals(replica));
        assertTrue(replica.equals(mv));

        SimpleMarshalledValue<UUID> nulled = this.factory.createMarshalledValue(null);
        assertFalse(mv.equals(nulled));
        assertFalse(nulled.equals(mv));
        assertFalse(replica.equals(nulled));
        assertFalse(nulled.equals(replica));
        assertTrue(nulled.equals(nulled));
        assertFalse(nulled.equals(null));
        assertTrue(nulled.equals(this.factory.createMarshalledValue(null)));
    }

    /**
     * Test method for {@link org.jboss.ha.framework.server.SimpleMarshalledValue#hashCode()}.
     */
    @Test
    public void testHashCode() throws Exception {
        UUID uuid = UUID.randomUUID();
        SimpleMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);
        assertEquals(uuid.hashCode(), mv.hashCode());

        SimpleMarshalledValue<UUID> copy = replicate(mv);
        this.validateHashCode(uuid, copy);

        mv = this.factory.createMarshalledValue(null);
        assertEquals(0, mv.hashCode());
    }

    <T> void validateHashCode(T original, SimpleMarshalledValue<T> copy) {
        assertEquals(0, copy.hashCode());
    }

    @SuppressWarnings("unchecked")
    <V> SimpleMarshalledValue<V> replicate(SimpleMarshalledValue<V> mv) throws IOException, ClassNotFoundException {
        return (SimpleMarshalledValue<V>) unmarshall(marshall(mv));
    }

    private static byte[] marshall(Object mv) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(mv);
        }
        return baos.toByteArray();
    }

    private static Object unmarshall(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
}
