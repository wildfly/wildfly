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
package org.wildfly.clustering.ejb.infinispan;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jboss.ejb.client.BasicSessionID;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.ejb.client.UnknownSessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * @author Paul Ferraro
 */
public enum SessionIDSerializer implements Serializer<SessionID> {
    INSTANCE;

    @Override
    public void write(DataOutput output, SessionID id) throws IOException {
        byte[] encoded = id.getEncodedForm();
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, encoded.length);
        output.write(encoded);
    }

    @Override
    public SessionID read(DataInput input) throws IOException {
        byte[] encoded = new byte[IndexSerializer.UNSIGNED_BYTE.readInt(input)];
        input.readFully(encoded);
        return SessionID.createSessionID(encoded);
    }

    @MetaInfServices(Externalizer.class)
    public static class BasicSessionIDExternalizer extends SerializerExternalizer<SessionID> {
        @SuppressWarnings("unchecked")
        public BasicSessionIDExternalizer() {
            super((Class<SessionID>) (Class<?>) BasicSessionID.class, INSTANCE);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class UnknownSessionIDExternalizer extends SerializerExternalizer<SessionID> {
        @SuppressWarnings("unchecked")
        public UnknownSessionIDExternalizer() {
            super((Class<SessionID>) (Class<?>) UnknownSessionID.class, INSTANCE);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class UUIDSessionIDExternalizer extends SerializerExternalizer<SessionID> {
        @SuppressWarnings("unchecked")
        public UUIDSessionIDExternalizer() {
            super((Class<SessionID>) (Class<?>) UUIDSessionID.class, INSTANCE);
        }
    }
}
