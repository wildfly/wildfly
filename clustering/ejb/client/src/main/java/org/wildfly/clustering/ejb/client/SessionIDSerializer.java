/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.client;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.IndexSerializer;
import org.wildfly.clustering.marshalling.Serializer;

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
}
