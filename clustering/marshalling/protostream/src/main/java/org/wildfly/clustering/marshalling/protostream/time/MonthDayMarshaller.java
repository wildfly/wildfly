/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Month;
import java.time.MonthDay;

import org.infinispan.protostream.descriptors.WireType;
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
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            switch (index) {
                case MONTH_INDEX:
                    result = result.with(MONTHS[reader.readEnum()]);
                    break;
                case DAY_OF_MONTH_INDEX:
                    result = result.withDayOfMonth(reader.readUInt32() + 1);
                    break;
                default:
                    reader.skipField(tag);
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
