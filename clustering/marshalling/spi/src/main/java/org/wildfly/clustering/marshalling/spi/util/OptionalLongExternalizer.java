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
    public Class<OptionalLong> getTargetClass() {
        return OptionalLong.class;
    }
}
