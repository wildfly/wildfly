/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for an {@link Instant}.
 * @author Paul Ferraro
 */
public class InstantExternalizer implements Externalizer<Instant> {

    @Override
    public void writeObject(ObjectOutput output, Instant instant) throws IOException {
        output.writeLong(instant.getEpochSecond());
        output.writeInt(instant.getNano());
    }

    @Override
    public Instant readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        long seconds = input.readLong();
        int nanos = input.readInt();
        return Instant.ofEpochSecond(seconds, nanos);
    }

    @Override
    public Class<Instant> getTargetClass() {
        return Instant.class;
    }

    @Override
    public OptionalInt size(Instant object) {
        return OptionalInt.of(Long.BYTES + Integer.BYTES);
    }
}
