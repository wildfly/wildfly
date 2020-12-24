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

package org.wildfly.clustering.web.cache.session;

import java.io.IOException;
import java.time.Duration;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.AutoSizedProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.time.DurationMarshaller;

/**
 * @author Paul Ferraro
 */
public enum SessionAccessMetaDataMarshaller implements AutoSizedProtoStreamMarshaller<SimpleSessionAccessMetaData> {
    INSTANCE;

    // Optimize for new sessions
    private static final Duration DEFAULT_SINCE_CREATION = Duration.ZERO;
    // Optimize for sub-second request duration
    private static final Duration DEFAULT_LAST_ACCESS = Duration.ofSeconds(1);

    private static final int SINCE_CREATION_INDEX = 1;
    private static final int LAST_ACCESS_INDEX = 5;

    @Override
    public SimpleSessionAccessMetaData readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Duration sinceCreation = DEFAULT_SINCE_CREATION;
        Duration lastAccess = DEFAULT_LAST_ACCESS;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                case 4:
                    sinceCreation = DurationMarshaller.INSTANCE.readField(context, reader, index - SINCE_CREATION_INDEX, sinceCreation);
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    lastAccess = DurationMarshaller.INSTANCE.readField(context, reader, index - LAST_ACCESS_INDEX, lastAccess);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        SimpleSessionAccessMetaData metaData = new SimpleSessionAccessMetaData();
        metaData.setLastAccessDuration(sinceCreation, lastAccess);
        return metaData;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, SimpleSessionAccessMetaData metaData) throws IOException {
        Duration sinceCreation = metaData.getSinceCreationDuration();
        if (!sinceCreation.equals(DEFAULT_SINCE_CREATION)) {
            DurationMarshaller.INSTANCE.writeFields(context, writer, SINCE_CREATION_INDEX, sinceCreation);
        }
        Duration lastAccess = metaData.getLastAccessDuration();
        if (!lastAccess.equals(DEFAULT_LAST_ACCESS)) {
            DurationMarshaller.INSTANCE.writeFields(context, writer, LAST_ACCESS_INDEX, lastAccess);
        }
    }

    @Override
    public Class<? extends SimpleSessionAccessMetaData> getJavaClass() {
        return SimpleSessionAccessMetaData.class;
    }
}
