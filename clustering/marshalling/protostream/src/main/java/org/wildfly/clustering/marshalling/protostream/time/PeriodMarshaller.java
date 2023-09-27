/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Period;

import org.infinispan.protostream.descriptors.WireType;
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
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
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
                    reader.skipField(tag);
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
