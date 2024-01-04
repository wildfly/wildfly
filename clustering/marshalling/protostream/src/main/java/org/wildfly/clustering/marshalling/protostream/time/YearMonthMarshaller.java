/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.FieldSetReader;
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
    private static final YearMonth DEFAULT = YearMonth.of(YearMarshaller.INSTANCE.createInitialValue().getValue(), Month.JANUARY);

    private static final int YEAR_INDEX = 1;
    private static final int MONTH_INDEX = YearMarshaller.INSTANCE.nextIndex(YEAR_INDEX);

    @Override
    public YearMonth readFrom(ProtoStreamReader reader) throws IOException {
        FieldSetReader<Year> yearReader = reader.createFieldSetReader(YearMarshaller.INSTANCE, YEAR_INDEX);
        YearMonth result = DEFAULT;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (yearReader.contains(index)) {
                result = result.withYear(yearReader.readField(Year.of(result.getYear())).getValue());
            } else if (index == MONTH_INDEX) {
                result = result.withMonth(MONTHS[reader.readEnum()].getValue());
            } else {
                reader.skipField(tag);
            }
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, YearMonth value) throws IOException {
        int year = value.getYear();
        if (year != DEFAULT.getYear()) {
            writer.createFieldSetWriter(YearMarshaller.INSTANCE, YEAR_INDEX).writeFields(Year.of(year));
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
