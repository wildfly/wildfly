/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Base {@link Externalizer} for enumerations.
 * @author Paul Ferraro
 */
public class EnumExternalizer<E extends Enum<E>> implements Externalizer<E> {

    private final IntSerializer ordinalSerializer;
    private final Class<E> enumClass;
    private final E[] values;

    public EnumExternalizer(Class<E> enumClass) {
        this.ordinalSerializer = IndexSerializer.select(enumClass.getEnumConstants().length);
        this.enumClass = enumClass;
        this.values = enumClass.getEnumConstants();
    }

    @Override
    public void writeObject(ObjectOutput output, E value) throws IOException {
        this.ordinalSerializer.writeInt(output, value.ordinal());
    }

    @Override
    public E readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.values[this.ordinalSerializer.readInt(input)];
    }

    @Override
    public Class<E> getTargetClass() {
        return this.enumClass;
    }

    @Override
    public OptionalInt size(E value) {
        return OptionalInt.of(this.ordinalSerializer.size(value.ordinal()));
    }
}
