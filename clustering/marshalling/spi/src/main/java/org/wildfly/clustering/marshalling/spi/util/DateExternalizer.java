/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
import java.sql.Timestamp;
import java.util.Date;
import java.util.function.LongFunction;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.LongExternalizer;

/**
 * Externalizers for {@link Date} implementations.
 * @author Paul Ferraro
 */
public class DateExternalizer<D extends Date> extends LongExternalizer<D> {

    DateExternalizer(LongFunction<D> factory, Class<D> targetClass) {
        super(targetClass, factory, Date::getTime);
    }

    @MetaInfServices(Externalizer.class)
    public static class UtilDateExternalizer extends DateExternalizer<Date> {
        public UtilDateExternalizer() {
            super(Date::new, Date.class);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class SqlDateExternalizer extends DateExternalizer<java.sql.Date> {
        public SqlDateExternalizer() {
            super(java.sql.Date::new, java.sql.Date.class);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class SqlTimeExternalizer extends DateExternalizer<java.sql.Time> {
        public SqlTimeExternalizer() {
            super(java.sql.Time::new, java.sql.Time.class);
        }
    }

    @MetaInfServices(Externalizer.class)
    public static class SqlTimestampExternalizer extends DateExternalizer<java.sql.Timestamp> {
        public SqlTimestampExternalizer() {
            super(java.sql.Timestamp::new, java.sql.Timestamp.class);
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
}
