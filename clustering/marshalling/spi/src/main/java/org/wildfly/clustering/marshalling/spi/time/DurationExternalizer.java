/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Duration;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link Duration}.
 * @author Paul Ferraro
 */
public class DurationExternalizer implements Externalizer<Duration> {

    @Override
    public void writeObject(ObjectOutput output, Duration duration) throws IOException {
        output.writeLong(duration.getSeconds());
        output.writeInt(duration.getNano());
    }

    @Override
    public Duration readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        long seconds = input.readLong();
        int nanos = input.readInt();
        return Duration.ofSeconds(seconds, nanos);
    }

    @Override
    public Class<Duration> getTargetClass() {
        return Duration.class;
    }

    @Override
    public OptionalInt size(Duration object) {
        return OptionalInt.of(Long.BYTES + Integer.BYTES);
    }
}
