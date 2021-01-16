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
