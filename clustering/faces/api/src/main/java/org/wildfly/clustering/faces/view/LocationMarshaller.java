/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.faces.view;

import java.io.IOException;

import jakarta.faces.view.Location;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link Location}.
 * @author Paul Ferraro
 */
public class LocationMarshaller implements ProtoStreamMarshaller<Location> {

    private static final int PATH_INDEX = 1;
    private static final int LINE_INDEX = 2;
    private static final int COLUMN_INDEX = 3;

    @Override
    public Class<? extends Location> getJavaClass() {
        return Location.class;
    }

    @Override
    public Location readFrom(ProtoStreamReader reader) throws IOException {
        String path = null;
        int line = -1;
        int column = -1;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case PATH_INDEX:
                    path = reader.readString();
                    break;
                case LINE_INDEX:
                    line = reader.readUInt32();
                    break;
                case COLUMN_INDEX:
                    column = reader.readUInt32();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new Location(path, line, column);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Location location) throws IOException {
        String path = location.getPath();
        if (path != null) {
            writer.writeString(PATH_INDEX, path);
        }
        int line = location.getLine();
        if (line >= 0) {
            writer.writeUInt32(LINE_INDEX, line);
        }
        int column = location.getColumn();
        if (column >= 0) {
            writer.writeUInt32(COLUMN_INDEX, column);
        }
    }
}
