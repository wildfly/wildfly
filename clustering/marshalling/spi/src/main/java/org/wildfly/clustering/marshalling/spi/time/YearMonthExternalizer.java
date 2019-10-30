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
import java.time.YearMonth;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Externalizer for a {@link YearMonth}.
 * @author Paul Ferraro
 */
public class YearMonthExternalizer implements Externalizer<YearMonth> {

    @Override
    public void writeObject(ObjectOutput output, YearMonth value) throws IOException {
        output.writeInt(value.getYear());
        DefaultExternalizer.MONTH.cast(Month.class).writeObject(output, value.getMonth());
    }

    @Override
    public YearMonth readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        int year = input.readInt();
        Month month = DefaultExternalizer.MONTH.cast(Month.class).readObject(input);
        return YearMonth.of(year, month);
    }

    @Override
    public Class<YearMonth> getTargetClass() {
        return YearMonth.class;
    }
}
