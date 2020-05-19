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

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.marshalling.jboss.ByteBufferOutputStream;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;

/**
 * @author Paul Ferraro
 */
public class CommandDispatcherMarshaller<C, MC> implements CommandMarshaller<C> {

    private final MarshallingContext context;
    private final Object id;
    private final MarshalledValueFactory<MC> factory;

    public CommandDispatcherMarshaller(MarshallingContext context, Object id, MarshalledValueFactory<MC> factory) {
        this.context = context;
        this.id = id;
        this.factory = factory;
    }

    @Override
    public <R> ByteBuffer marshal(Command<R, ? super C> command) throws IOException {
        int version = this.context.getCurrentVersion();
        ByteBufferOutputStream buffer = new ByteBufferOutputStream();
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            IndexSerializer.VARIABLE.writeInt(output, version);
            try (Marshaller marshaller = this.context.createMarshaller(version)) {
                marshaller.start(Marshalling.createByteOutput(output));
                marshaller.writeObject(this.id);
                marshaller.writeObject(this.factory.createMarshalledValue(command));
                marshaller.flush();
            }
            return buffer.getBuffer();
        }
    }
}
