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
import java.io.StreamCorruptedException;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.OptionalInt;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.PrimitiveMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;

/**
 * Marshals {@link Year} instances as number of years since the epoch as a signed integer.
 * @author Paul Ferraro
 */
public enum YearMarshaller implements ProtoStreamMarshaller<Year> {
    INSTANCE;

    @Override
    public Year readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        int epochYear = PrimitiveMarshaller.INTEGER.cast(Integer.class).readFrom(context, reader);
        if (reader.readTag() != 0) {
            throw new StreamCorruptedException();
        }
        return Year.of(epochYear + Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear());
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, Year year) throws IOException {
        int epochYear = year.getValue() - Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear();
        PrimitiveMarshaller.INTEGER.writeTo(context, writer, epochYear);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, Year year) {
        int epochYear = year.getValue() - Instant.EPOCH.atOffset(ZoneOffset.UTC).getYear();
        return PrimitiveMarshaller.INTEGER.size(context, epochYear);
    }

    @Override
    public Class<? extends Year> getJavaClass() {
        return Year.class;
    }
}
