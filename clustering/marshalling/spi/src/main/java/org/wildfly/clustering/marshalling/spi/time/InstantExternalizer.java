/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;

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
}
