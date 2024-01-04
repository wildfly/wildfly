/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.dispatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Map;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public class CommandDispatcherMarshaller<C, MC> implements CommandMarshaller<C> {

    private final ByteBufferMarshaller marshaller;
    private final Object id;
    private final MarshalledValueFactory<MC> factory;

    public CommandDispatcherMarshaller(ByteBufferMarshaller marshaller, Object id, MarshalledValueFactory<MC> factory) {
        this.marshaller = marshaller;
        this.id = id;
        this.factory = factory;
    }

    @Override
    public <R> ByteBuffer marshal(Command<R, ? super C> command) throws IOException {
        MarshalledValue<Command<R, ? super C>, MC> value = this.factory.createMarshalledValue(command);
        Map.Entry<Object, MarshalledValue<Command<R, ? super C>, MC>> entry = new AbstractMap.SimpleImmutableEntry<>(this.id, value);
        return this.marshaller.write(entry);
    }
}
