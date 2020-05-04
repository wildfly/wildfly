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

package org.wildfly.clustering.marshalling.spi.sql;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Timestamp;

import org.wildfly.clustering.marshalling.spi.util.DateExternalizer;

/**
 * Externalizer for a {@link Timestamp}.
 * @author Radoslav Husar
 */
public class TimestampExternalizer extends DateExternalizer<Timestamp> {
    public TimestampExternalizer() {
        super(Timestamp.class, Timestamp::new);
    }

    @Override
    public void writeObject(ObjectOutput output, Timestamp timestamp) throws IOException {
        super.writeObject(output, timestamp);
        output.writeInt(timestamp.getNanos());
    }

    @Override
    public Timestamp readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Timestamp timestamp = super.readObject(input);
        timestamp.setNanos(input.readInt());
        return timestamp;
    }
}