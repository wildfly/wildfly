/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.BitSet;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class BitSetExternalizer implements Externalizer<BitSet> {

    @Override
    public void writeObject(ObjectOutput output, BitSet set) throws IOException {
        byte[] bytes = set.toByteArray();
        IndexSerializer.VARIABLE.writeInt(output, bytes.length);
        output.write(bytes);
    }

    @Override
    public BitSet readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        byte[] bytes = new byte[IndexSerializer.VARIABLE.readInt(input)];
        input.readFully(bytes);
        return BitSet.valueOf(bytes);
    }

    @Override
    public OptionalInt size(BitSet set) {
        int size = set.size();
        int bytes = size / Byte.SIZE;
        if (size % Byte.SIZE > 0) {
            bytes += 1;
        }
        return OptionalInt.of(IndexSerializer.VARIABLE.size(bytes) + bytes);
    }

    @Override
    public Class<BitSet> getTargetClass() {
        return BitSet.class;
    }
}
