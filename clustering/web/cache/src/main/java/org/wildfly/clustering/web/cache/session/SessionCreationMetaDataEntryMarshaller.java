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
import java.time.Instant;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.AutoSizedProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.time.DurationMarshaller;

/**
 * @author Paul Ferraro
 */
public enum SessionCreationMetaDataEntryMarshaller implements AutoSizedProtoStreamMarshaller<SessionCreationMetaDataEntry<Object>> {
    INSTANCE;

    private static final Duration DEFAULT_CREATION_DURATION = Duration.ZERO;
    // Optimize for specification default
    private static final Duration DEFAULT_MAX_INACTIVE_INTERVAL = Duration.ofMinutes(30L);

    private static final int CREATION_DURATION_INDEX = 1;
    private static final int MAX_INACTIVE_INTERVAL_INDEX = 5;

    @Override
    public SessionCreationMetaDataEntry<Object> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        Duration creationDuration = DEFAULT_CREATION_DURATION;
        Duration maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case 1:
                case 2:
                case 3:
                case 4:
                    creationDuration = DurationMarshaller.INSTANCE.readField(context, reader, index - CREATION_DURATION_INDEX, creationDuration);
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    maxInactiveInterval = DurationMarshaller.INSTANCE.readField(context, reader, index - MAX_INACTIVE_INTERVAL_INDEX, maxInactiveInterval);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        SessionCreationMetaData metaData = new SimpleSessionCreationMetaData(Instant.ofEpochSecond(creationDuration.getSeconds(), creationDuration.getNano()));
        metaData.setMaxInactiveInterval(maxInactiveInterval);
        return new SessionCreationMetaDataEntry<>(metaData);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, SessionCreationMetaDataEntry<Object> entry) throws IOException {
        SessionCreationMetaData metaData = entry.getMetaData();

        Instant creationTime = metaData.getCreationTime();
        Duration creationDuration = Duration.ofSeconds(creationTime.getEpochSecond(), creationTime.getNano());
        if (!creationDuration.equals(DEFAULT_CREATION_DURATION)) {
            DurationMarshaller.INSTANCE.writeFields(context, writer, CREATION_DURATION_INDEX, creationDuration);
        }

        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.equals(DEFAULT_MAX_INACTIVE_INTERVAL)) {
            DurationMarshaller.INSTANCE.writeFields(context, writer, MAX_INACTIVE_INTERVAL_INDEX, maxInactiveInterval);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SessionCreationMetaDataEntry<Object>> getJavaClass() {
        return (Class<SessionCreationMetaDataEntry<Object>>) (Class<?>) SessionCreationMetaDataEntry.class;
    }
}
