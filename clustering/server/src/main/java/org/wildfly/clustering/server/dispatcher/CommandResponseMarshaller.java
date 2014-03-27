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
import java.io.InputStream;

import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Buffer;

/**
 * Marshalling strategy for the command response.
 * @author Paul Ferraro
 *
 * @param <C> command execution context
 */
public class CommandResponseMarshaller implements RpcDispatcher.Marshaller {
    private final MarshallingContext context;

    CommandResponseMarshaller(MarshallingContext context) {
        this.context = context;
    }

    @Override
    public Buffer objectToBuffer(Object object) throws Exception {
        int version = this.context.getCurrentVersion();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(version);
            try (Marshaller marshaller = this.context.createMarshaller(version)) {
                marshaller.start(Marshalling.createByteOutput(output));
                marshaller.writeObject(object);
                marshaller.flush();
            }
            return new Buffer(output.toByteArray());
        }
    }

    @Override
    public Object objectFromBuffer(byte[] buffer, int offset, int length) throws Exception {
        try (InputStream input = new ByteArrayInputStream(buffer, offset, length)) {
            int version = input.read();
            try (Unmarshaller unmarshaller = this.context.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                return unmarshaller.readObject();
            }
        }
    }
}
