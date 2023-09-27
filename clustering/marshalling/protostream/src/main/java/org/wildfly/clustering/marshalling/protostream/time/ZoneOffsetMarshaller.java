/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.ZoneOffset;

import org.wildfly.clustering.marshalling.protostream.FieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshalling for {@link ZoneOffset} instances using the following strategy:
 * <ol>
 * <li>Marshal {@link ZoneOffset#UTC} as zero bytes</li>
 * <li>If offset is of form &plusmn;HH, marshal as signed integer of hours</li>
 * <li>If offset is of form &plusmn;HH:MM, marshal as signed integer of total minutes<li>
 * <li>If offset is of form &plusmn;HH:MM:SS, marshal as signed integer of total seconds<li>
 * </ol>
 * @author Paul Ferraro
 */
public enum ZoneOffsetMarshaller implements FieldSetMarshaller<ZoneOffset, ZoneOffset> {
    INSTANCE;

    private static final int HOURS_INDEX = 0;
    private static final int MINUTES_INDEX = 1;
    private static final int SECONDS_INDEX = 2;
    private static final int FIELDS = 3;

    @Override
    public ZoneOffset getBuilder() {
        return ZoneOffset.UTC;
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public ZoneOffset readField(ProtoStreamReader reader, int index, ZoneOffset offset) throws IOException {
        switch (index) {
            case HOURS_INDEX:
                return ZoneOffset.ofHours(reader.readSInt32());
            case MINUTES_INDEX:
                return ZoneOffset.ofTotalSeconds(reader.readSInt32() * 60);
            case SECONDS_INDEX:
                return ZoneOffset.ofTotalSeconds(reader.readSInt32());
            default:
                return offset;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, ZoneOffset offset) throws IOException {
        int seconds = offset.getTotalSeconds();
        if (seconds != 0) {
            if (seconds % 60 == 0) {
                int minutes = seconds / 60;
                if (minutes % 60 == 0) {
                    int hours = minutes / 60;
                    // Typical offsets
                    writer.writeSInt32(startIndex + HOURS_INDEX, hours);
                } else {
                    // Uncommon fractional hour offsets
                    writer.writeSInt32(startIndex + MINUTES_INDEX, minutes);
                }
            } else {
                // Synthetic offsets
                writer.writeSInt32(startIndex + SECONDS_INDEX, seconds);
            }
        }
    }
}
