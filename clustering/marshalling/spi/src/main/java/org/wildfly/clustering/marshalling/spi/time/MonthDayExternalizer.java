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

package org.wildfly.clustering.marshalling.spi.time;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Month;
import java.time.MonthDay;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Externalizer for a {@link MonthDay}.
 * @author Paul Ferraro
 */
public class MonthDayExternalizer implements Externalizer<MonthDay> {

    @Override
    public void writeObject(ObjectOutput output, MonthDay monthDay) throws IOException {
        DefaultExternalizer.MONTH.cast(Month.class).writeObject(output, monthDay.getMonth());
        IndexSerializer.UNSIGNED_BYTE.writeInt(output, monthDay.getDayOfMonth());
    }

    @Override
    public MonthDay readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        Month month = DefaultExternalizer.MONTH.cast(Month.class).readObject(input);
        int day = IndexSerializer.UNSIGNED_BYTE.readInt(input);
        return MonthDay.of(month, day);
    }

    @Override
    public Class<MonthDay> getTargetClass() {
        return MonthDay.class;
    }
}
