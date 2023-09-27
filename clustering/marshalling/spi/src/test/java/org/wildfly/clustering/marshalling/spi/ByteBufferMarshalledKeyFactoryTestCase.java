/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
