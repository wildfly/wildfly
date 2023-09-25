/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for an {@link OptionalLong}.
 * @author Paul Ferraro
 */
public class OptionalLongExternalizer implements Externalizer<OptionalLong> {

    @Override
    public void writeObject(ObjectOutput output, OptionalLong value) throws IOException {
        boolean present = value.isPresent();
        output.writeBoolean(present);
        if (present) {
            output.writeLong(value.getAsLong());
        }
    }

    @Override
    public OptionalLong readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return (input.readBoolean()) ? OptionalLong.of(input.readLong()) : OptionalLong.empty();
    }

    @Override
    public OptionalInt size(OptionalLong value) {
        return OptionalInt.of(value.isPresent() ? Long.BYTES + Byte.BYTES : Byte.BYTES);
    }

    @Override
    public Class<OptionalLong> getTargetClass() {
        return OptionalLong.class;
    }
}
