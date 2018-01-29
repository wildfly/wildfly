/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.IntSerializer;

/**
 * Marshalling strategy for the command response.
 * @author Paul Ferraro
 *
 * @param <C> command execution context
 */
public class CommandResponseMarshaller implements org.jgroups.blocks.Marshaller {
    private final MarshallingContext context;
    private final ChannelFactory factory;
    private final IntSerializer versionSerializer;

    CommandResponseMarshaller(ChannelCommandDispatcherFactoryConfiguration config) {
        this.context = config.getMarshallingContext();
        this.factory = config.getChannelFactory();
        this.versionSerializer = IndexSerializer.select(this.context.getCurrentVersion());
    }

    @Override
    public void objectToStream(Object object, DataOutput stream) throws Exception {
        int version = this.context.getCurrentVersion();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            this.versionSerializer.writeInt(output, version);
            try (Marshaller marshaller = this.context.createMarshaller(version)) {
                marshaller.start(Marshalling.createByteOutput(output));
                marshaller.writeObject(object);
                marshaller.flush();
            }
        }
        byte[] buffer = bytes.toByteArray();
        IndexSerializer.VARIABLE.writeInt(stream, buffer.length);
        stream.write(buffer);
    }

    @Override
    public Object objectFromStream(DataInput stream) throws Exception {
        int size = IndexSerializer.VARIABLE.readInt(stream);
        byte[] buffer = new byte[size];
        stream.readFully(buffer);
        if (this.factory.isUnknownForkResponse(ByteBuffer.wrap(buffer))) return NoSuchService.INSTANCE;
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer))) {
            int version = this.versionSerializer.readInt(input);
            try (Unmarshaller unmarshaller = this.context.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                return unmarshaller.readObject();
            }
        }
    }
}
