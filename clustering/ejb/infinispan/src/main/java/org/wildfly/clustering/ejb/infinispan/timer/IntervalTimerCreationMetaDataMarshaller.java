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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * @author Paul Ferraro
 */
public class IntervalTimerCreationMetaDataMarshaller implements ProtoStreamMarshaller<IntervalTimerCreationMetaData<Object>> {

    private static final int INFO_INDEX = 1;
    private static final int START_INDEX = 2;
    private static final int INTERVAL_INDEX = 3;

    private static final Instant DEFAULT_START = Instant.EPOCH;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends IntervalTimerCreationMetaData<Object>> getJavaClass() {
        return (Class<IntervalTimerCreationMetaDataEntry<Object>>) (Class<?>) IntervalTimerCreationMetaDataEntry.class;
    }

    @Override
    public IntervalTimerCreationMetaData<Object> readFrom(ProtoStreamReader reader) throws IOException {
        MarshalledValue<Object, Object> context = null;
        Instant start = DEFAULT_START;
        Duration interval = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case INFO_INDEX:
                    context = reader.readObject(ByteBufferMarshalledValue.class);
                    break;
                case START_INDEX:
                    start = reader.readObject(Instant.class);
                    break;
                case INTERVAL_INDEX:
                    interval = reader.readObject(Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new IntervalTimerCreationMetaDataEntry<>(context, start, interval);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, IntervalTimerCreationMetaData<Object> metaData) throws IOException {
        Object context = metaData.getContext();
        if (context != null) {
            writer.writeObject(INFO_INDEX, context);
        }
        Instant start = metaData.getStart();
        if (!start.equals(DEFAULT_START)) {
            writer.writeObject(START_INDEX, start);
        }
        Duration interval = metaData.getInterval();
        if (interval != null) {
            writer.writeObject(INTERVAL_INDEX, interval);
        }
    }
}
