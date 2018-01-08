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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Base64;

import org.jboss.ejb.client.BasicSessionID;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.ejb.client.UnknownSessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.SimpleKeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class SessionIDExternalizer extends SimpleKeyFormat<SessionID> implements Externalizer<SessionID> {

    public static final SessionIDExternalizer INSTANCE = new SessionIDExternalizer(SessionID.class);

    SessionIDExternalizer(Class<SessionID> targetClass) {
        super(targetClass, value -> SessionID.createSessionID(Base64.getDecoder().decode(value)), id -> Base64.getEncoder().encodeToString(id.getEncodedForm()));
    }

    @Override
    public void writeObject(ObjectOutput output, SessionID id) throws IOException {
        byte[] encoded = id.getEncodedForm();
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, encoded.length);
        output.write(encoded);
    }

    @Override
    public SessionID readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        byte[] encoded = new byte[IndexSerializer.UNSIGNED_BYTE.readInt(input)];
        input.readFully(encoded);
        return SessionID.createSessionID(encoded);
    }

    @MetaInfServices(Externalizer.class)
    public static class BasicSessionIDExternalizer extends SessionIDExternalizer {
        @SuppressWarnings("unchecked")
        public BasicSessionIDExternalizer() {
            super((Class<SessionID>) (Class<?>) BasicSessionID.class);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class UnknownSessionIDExternalizer extends SessionIDExternalizer {
        @SuppressWarnings("unchecked")
        public UnknownSessionIDExternalizer() {
            super((Class<SessionID>) (Class<?>) UnknownSessionID.class);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class UUIDSessionIDExternalizer extends SessionIDExternalizer {
        @SuppressWarnings("unchecked")
        public UUIDSessionIDExternalizer() {
            super((Class<SessionID>) (Class<?>) UUIDSessionID.class);
        }
    }
}
