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
import java.time.Month;
import java.time.MonthDay;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshals {@link MonthDay} instances.
 * @author Paul Ferraro
 */
public class MonthDayMarshaller implements ProtoStreamMarshaller<MonthDay> {

    private static final Month[] MONTHS = Month.values();
    private static final MonthDay DEFAULT = MonthDay.of(Month.JANUARY, 1);

    private static final int MONTH_INDEX = 1;
    private static final int DAY_OF_MONTH_INDEX = 2;

    @Override
    public MonthDay readFrom(ProtoStreamReader reader) throws IOException {
        MonthDay result = DEFAULT;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case MONTH_INDEX:
                    result = result.with(MONTHS[reader.readEnum()]);
                    break;
                case DAY_OF_MONTH_INDEX:
                    result = result.withDayOfMonth(reader.readUInt32() + 1);
                    break;
                default:
                    reading = reader.ignoreField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, MonthDay value) throws IOException {
        Month month = value.getMonth();
        if (month != DEFAULT.getMonth()) {
            writer.writeEnum(MONTH_INDEX, month.ordinal());
        }
        int dayOfMonth = value.getDayOfMonth();
        if (dayOfMonth != DEFAULT.getDayOfMonth()) {
            writer.writeUInt32(DAY_OF_MONTH_INDEX, dayOfMonth - 1);
        }
    }

    @Override
    public Class<? extends MonthDay> getJavaClass() {
        return MonthDay.class;
    }
}
