/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for an {@link OptionalInt}.
 * @author Paul Ferraro
 */
public class OptionalIntExternalizer implements Externalizer<OptionalInt> {

    @Override
    public void writeObject(ObjectOutput output, OptionalInt value) throws IOException {
        boolean present = value.isPresent();
        output.writeBoolean(present);
        if (present) {
            output.writeInt(value.getAsInt());
        }
    }

    @Override
    public OptionalInt readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return (input.readBoolean()) ? OptionalInt.of(input.readInt()) : OptionalInt.empty();
    }

    @Override
    public OptionalInt size(OptionalInt value) {
        return OptionalInt.of(value.isPresent() ? Integer.BYTES + Byte.BYTES : Byte.BYTES);
    }

    @Override
    public Class<OptionalInt> getTargetClass() {
        return OptionalInt.class;
    }
}
