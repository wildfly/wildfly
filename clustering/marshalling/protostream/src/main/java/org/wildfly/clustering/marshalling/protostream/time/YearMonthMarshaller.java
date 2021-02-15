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
import java.time.Year;
import java.time.YearMonth;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for {@link YearMonth} instances, using the following strategy:
 * <ol>
 * <li>Marshal epoch year</li>
 * <li>Marshal month as enum</li>
 * </ol>
 * @author Paul Ferraro
 */
public class YearMonthMarshaller implements ProtoStreamMarshaller<YearMonth> {

    private static final Month[] MONTHS = Month.values();
    private static final YearMonth DEFAULT = YearMonth.of(YearMarshaller.INSTANCE.getBuilder().getValue(), Month.JANUARY);

    private static final int YEAR_INDEX = 1;
    private static final int MONTH_INDEX = YEAR_INDEX + YearMarshaller.INSTANCE.getFields();

    @Override
    public YearMonth readFrom(ProtoStreamReader reader) throws IOException {
        YearMonth result = DEFAULT;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index >= YEAR_INDEX && index < MONTH_INDEX) {
                result = result.withYear(YearMarshaller.INSTANCE.readField(reader, index - YEAR_INDEX, Year.of(result.getYear())).getValue());
            } else if (index == MONTH_INDEX) {
                result = result.withMonth(MONTHS[reader.readEnum()].getValue());
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, YearMonth value) throws IOException {
        int year = value.getYear();
        if (year != DEFAULT.getYear()) {
            YearMarshaller.INSTANCE.writeFields(writer, YEAR_INDEX, Year.of(year));
        }
        Month month = value.getMonth();
        if (month != DEFAULT.getMonth()) {
            writer.writeEnum(MONTH_INDEX, month.ordinal());
        }
    }

    @Override
    public Class<? extends YearMonth> getJavaClass() {
        return YearMonth.class;
    }
}
