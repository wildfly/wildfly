/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
