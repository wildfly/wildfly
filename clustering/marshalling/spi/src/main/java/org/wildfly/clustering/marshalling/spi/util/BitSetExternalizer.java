/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
