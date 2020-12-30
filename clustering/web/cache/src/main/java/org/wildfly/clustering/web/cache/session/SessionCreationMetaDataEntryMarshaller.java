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

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntryMarshaller implements ProtoStreamMarshaller<SessionCreationMetaDataEntry<Object>> {

    private static final Instant DEFAULT_CREATION_TIME = Instant.EPOCH;
    // Optimize for specification default
    private static final Duration DEFAULT_MAX_INACTIVE_INTERVAL = Duration.ofMinutes(30L);

    private static final int CREATION_TIME_INDEX = 1;
    private static final int MAX_INACTIVE_INTERVAL_INDEX = 2;

    @Override
    public SessionCreationMetaDataEntry<Object> readFrom(ProtoStreamReader reader) throws IOException {
        Instant creationTime = DEFAULT_CREATION_TIME;
        Duration maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CREATION_TIME_INDEX:
                    creationTime = reader.readObject(Instant.class);
                    break;
                case MAX_INACTIVE_INTERVAL_INDEX:
                    maxInactiveInterval = reader.readObject(Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        SessionCreationMetaData metaData = new SimpleSessionCreationMetaData(creationTime);
        metaData.setMaxInactiveInterval(maxInactiveInterval);
        return new SessionCreationMetaDataEntry<>(metaData);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SessionCreationMetaDataEntry<Object> entry) throws IOException {
        SessionCreationMetaData metaData = entry.getMetaData();

        Instant creationTime = metaData.getCreationTime();
        if (!creationTime.equals(DEFAULT_CREATION_TIME)) {
            writer.writeObject(CREATION_TIME_INDEX, creationTime);
        }

        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.equals(DEFAULT_MAX_INACTIVE_INTERVAL)) {
            writer.writeObject(MAX_INACTIVE_INTERVAL_INDEX, maxInactiveInterval);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SessionCreationMetaDataEntry<Object>> getJavaClass() {
        return (Class<SessionCreationMetaDataEntry<Object>>) (Class<?>) SessionCreationMetaDataEntry.class;
    }
}
