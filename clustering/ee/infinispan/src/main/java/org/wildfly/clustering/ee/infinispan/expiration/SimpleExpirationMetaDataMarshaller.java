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

package org.wildfly.clustering.ee.infinispan.expiration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link SimpleExpirationMetaData}.
 * @author Paul Ferraro
 */
public class SimpleExpirationMetaDataMarshaller implements ProtoStreamMarshaller<SimpleExpirationMetaData> {

    private static final int TIMEOUT_INDEX = 1;
    private static final int LAST_ACCESS_TIME_INDEX = 2;

    // This is the default specification timeout for web sessions
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
    private static final Instant DEFAULT_LAST_ACCESS_TIME = Instant.EPOCH;

    @Override
    public Class<? extends SimpleExpirationMetaData> getJavaClass() {
        return SimpleExpirationMetaData.class;
    }

    @Override
    public SimpleExpirationMetaData readFrom(ProtoStreamReader reader) throws IOException {
        Duration timeout = DEFAULT_TIMEOUT;
        Instant lastAccessTime = DEFAULT_LAST_ACCESS_TIME;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TIMEOUT_INDEX:
                    timeout = reader.readObject(Duration.class);
                    break;
                case LAST_ACCESS_TIME_INDEX:
                    lastAccessTime = reader.readObject(Instant.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SimpleExpirationMetaData(timeout, lastAccessTime);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SimpleExpirationMetaData metaData) throws IOException {
        Duration timeout = metaData.getTimeout();
        if (!DEFAULT_TIMEOUT.equals(timeout)) {
            writer.writeObject(TIMEOUT_INDEX, timeout);
        }
        Instant lastAccessedTime = metaData.getLastAccessTime();
        if (!DEFAULT_LAST_ACCESS_TIME.equals(lastAccessedTime)) {
            writer.writeObject(LAST_ACCESS_TIME_INDEX, lastAccessedTime);
        }
    }
}
