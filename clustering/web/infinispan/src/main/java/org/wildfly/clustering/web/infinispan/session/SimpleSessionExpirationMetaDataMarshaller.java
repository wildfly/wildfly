/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.session;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link SimpleSessionExpirationMetaData}.
 * @author Paul Ferraro
 */
public class SimpleSessionExpirationMetaDataMarshaller implements ProtoStreamMarshaller<SimpleSessionExpirationMetaData> {

    private static final byte MAX_INACTIVE_INTERVAL_INDEX = 1;
    private static final byte LAST_ACCESS_END_TIME_INDEX = 2;

    // Optimize for specification default
    private static final Duration DEFAULT_MAX_INACTIVE_INTERVAL = Duration.ofMinutes(30L);
    private static final Instant DEFAULT_LAST_ACCESS_END_TIME = Instant.EPOCH;

    @Override
    public Class<? extends SimpleSessionExpirationMetaData> getJavaClass() {
        return SimpleSessionExpirationMetaData.class;
    }

    @Override
    public SimpleSessionExpirationMetaData readFrom(ProtoStreamReader reader) throws IOException {
        Duration maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;
        Instant lastAccessEndTime = DEFAULT_LAST_ACCESS_END_TIME;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case MAX_INACTIVE_INTERVAL_INDEX:
                    maxInactiveInterval = reader.readObject(Duration.class);
                    break;
                case LAST_ACCESS_END_TIME_INDEX:
                    lastAccessEndTime = reader.readObject(Instant.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SimpleSessionExpirationMetaData(maxInactiveInterval, lastAccessEndTime);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SimpleSessionExpirationMetaData metaData) throws IOException {
        Duration maxInactiveInterval = metaData.getMaxInactiveInterval();
        if (!maxInactiveInterval.equals(DEFAULT_MAX_INACTIVE_INTERVAL)) {
            writer.writeObject(MAX_INACTIVE_INTERVAL_INDEX, maxInactiveInterval);
        }
        Instant lastAccessEndTime = metaData.getLastAccessEndTime();
        if (!lastAccessEndTime.equals(DEFAULT_LAST_ACCESS_END_TIME)) {
            writer.writeObject(LAST_ACCESS_END_TIME_INDEX, lastAccessEndTime);
        }
    }
}
