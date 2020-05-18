/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.dispatcher;

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
