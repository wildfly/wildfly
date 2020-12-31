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
import java.time.LocalDate;
import java.time.Year;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshals {@link Year} instances as number of years since the epoch year.
 * @author Paul Ferraro
 */
public enum YearMarshaller implements FieldSetMarshaller<Year, Year> {
    INSTANCE;

    private static final int POST_EPOCH_YEAR = 0;
    private static final int PRE_EPOCH_YEAR = 1;
    private static final int FIELDS = 2;

    private static final Year EPOCH = Year.of(LocalDate.ofEpochDay(0).getYear());

    @Override
    public Year getBuilder() {
        return EPOCH;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public Year readField(ProtoStreamReader reader, int index, Year year) throws IOException {
        switch (index) {
            case POST_EPOCH_YEAR:
                return Year.of(EPOCH.getValue() + reader.readUInt32());
            case PRE_EPOCH_YEAR:
                return Year.of(EPOCH.getValue() - reader.readUInt32());
            default:
                return year;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, Year value) throws IOException {
        int year = value.getValue();
        if (year > EPOCH.getValue()) {
            writer.writeUInt32(startIndex + POST_EPOCH_YEAR, year - EPOCH.getValue());
        } else if (year < EPOCH.getValue()) {
            writer.writeUInt32(startIndex + PRE_EPOCH_YEAR, EPOCH.getValue() - year);
        }
    }
}
