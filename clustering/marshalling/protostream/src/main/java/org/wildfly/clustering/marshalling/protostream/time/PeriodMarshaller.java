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

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Period;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link Period} instances, using the following strategy:
 * <ol>
 * <li>Marshal {@link Period#ZERO} as zero bytes</li>
 * <li>Marshal number of years of period as signed integer</li>
 * <li>Marshal number of months of period as signed integer</li>
 * <li>Marshal number of days of period as signed integer</li>
 * </ol>
 * @author Paul Ferraro
 */
public class PeriodMarshaller implements ProtoStreamMarshaller<Period> {

    private static final int YEARS_INDEX = 1;
    private static final int MONTHS_INDEX = 2;
    private static final int DAYS_INDEX = 3;

    @Override
    public Period readFrom(ProtoStreamReader reader) throws IOException {
        Period period = Period.ZERO;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case YEARS_INDEX:
                    period = period.withYears(reader.readSInt32());
                    break;
                case MONTHS_INDEX:
                    period = period.withMonths(reader.readSInt32());
                    break;
                case DAYS_INDEX:
                    period = period.withDays(reader.readSInt32());
                    break;
                default:
                    reading = reader.ignoreField(tag);
            }
        }
        return period;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Period period) throws IOException {
        int years = period.getYears();
        if (years != 0) {
            writer.writeSInt32(YEARS_INDEX, years);
        }
        int months = period.getMonths();
        if (months != 0) {
            writer.writeSInt32(MONTHS_INDEX, months);
        }
        int days = period.getDays();
        if (days != 0) {
            writer.writeSInt32(DAYS_INDEX, days);
        }
    }

    @Override
    public Class<? extends Period> getJavaClass() {
        return Period.class;
    }
}
