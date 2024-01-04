/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for an {@link OptionalDouble}.
 * @author Paul Ferraro
 */
public class OptionalDoubleExternalizer implements Externalizer<OptionalDouble> {

    @Override
    public void writeObject(ObjectOutput output, OptionalDouble value) throws IOException {
        boolean present = value.isPresent();
        output.writeBoolean(present);
        if (present) {
            output.writeDouble(value.getAsDouble());
        }
    }

    @Override
    public OptionalDouble readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return (input.readBoolean()) ? OptionalDouble.of(input.readDouble()) : OptionalDouble.empty();
    }

    @Override
    public OptionalInt size(OptionalDouble value) {
        return OptionalInt.of(value.isPresent() ? Double.BYTES + Byte.BYTES : Byte.BYTES);
    }

    @Override
    public Class<OptionalDouble> getTargetClass() {
        return OptionalDouble.class;
    }
}
