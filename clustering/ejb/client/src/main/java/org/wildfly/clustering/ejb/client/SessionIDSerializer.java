/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.client;

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
