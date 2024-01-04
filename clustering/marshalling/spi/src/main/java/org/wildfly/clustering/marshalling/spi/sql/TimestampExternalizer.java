/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.sql;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Timestamp;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.spi.util.DateExternalizer;

/**
 * Externalizer for a {@link Timestamp}.
 * @author Radoslav Husar
 */
public class TimestampExternalizer extends DateExternalizer<Timestamp> {
    public TimestampExternalizer() {
        super(Timestamp.class, Timestamp::new);
    }

    @Override
    public void writeObject(ObjectOutput output, Timestamp timestamp) throws IOException {
        super.writeObject(output, timestamp);
        output.writeInt(timestamp.getNanos());
    }

    @Override
    public Timestamp readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Timestamp timestamp = super.readObject(input);
        timestamp.setNanos(input.readInt());
        return timestamp;
    }

    @Override
    public OptionalInt size(Timestamp object) {
        return OptionalInt.of(Long.BYTES + Integer.BYTES);
    }
}