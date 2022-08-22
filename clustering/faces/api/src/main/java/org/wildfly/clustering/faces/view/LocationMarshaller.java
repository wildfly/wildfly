/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
