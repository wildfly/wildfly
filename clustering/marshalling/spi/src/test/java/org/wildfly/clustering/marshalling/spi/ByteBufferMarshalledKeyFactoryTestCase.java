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

import java.util.UUID;

/**
 * Unit tests for {@link ByteBufferMarshalledValue}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class ByteBufferMarshalledKeyFactoryTestCase extends ByteBufferMarshalledValueFactoryTestCase {

    private final ByteBufferMarshalledKeyFactory factory;

    public ByteBufferMarshalledKeyFactoryTestCase() {
        this(JavaByteBufferMarshaller.INSTANCE);
    }

    protected ByteBufferMarshalledKeyFactoryTestCase(ByteBufferMarshaller marshaller) {
        this(marshaller, new ByteBufferMarshalledKeyFactory(marshaller));
    }

    private ByteBufferMarshalledKeyFactoryTestCase(ByteBufferMarshaller marshaller, ByteBufferMarshalledKeyFactory factory) {
        super(marshaller, factory);
        this.factory = factory;
    }

    @Override
    public void testHashCode() throws Exception {
        UUID uuid = UUID.randomUUID();
        int expected = uuid.hashCode();
        ByteBufferMarshalledValue<UUID> mv = this.factory.createMarshalledValue(uuid);
        assertEquals(expected, mv.hashCode());

        ByteBufferMarshalledValue<UUID> copy = replicate(mv);
        assertEquals(expected, copy.hashCode());

        mv = this.factory.createMarshalledValue(null);
        assertEquals(0, mv.hashCode());
    }
}
