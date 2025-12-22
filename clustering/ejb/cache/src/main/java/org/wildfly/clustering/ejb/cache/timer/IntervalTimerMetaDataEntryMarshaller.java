/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.ByteBufferMarshalledValue;

/**
 * @author Paul Ferraro
 */
public class IntervalTimerMetaDataEntryMarshaller implements ProtoStreamMarshaller<IntervalTimerMetaDataEntry<Object>> {

    private static final int INFO_INDEX = 1;
    private static final int START_INDEX = 2;
    private static final int LAST_TIMEOUT_INDEX = 3;
    private static final int INTERVAL_INDEX = 4;

    private static final Instant DEFAULT_START = Instant.EPOCH;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends IntervalTimerMetaDataEntry<Object>> getJavaClass() {
        return (Class<IntervalTimerMetaDataEntry<Object>>) (Class<?>) IntervalTimerMetaDataEntry.class;
    }

    @Override
    public IntervalTimerMetaDataEntry<Object> readFrom(ProtoStreamReader reader) throws IOException {
        ByteBufferMarshalledValue<Object> context = null;
        Instant start = DEFAULT_START;
        Duration lastTimeout = null;
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
                case LAST_TIMEOUT_INDEX:
                    lastTimeout = reader.readObject(Duration.class);
                    break;
                case INTERVAL_INDEX:
                    interval = reader.readObject(Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        IntervalTimerMetaDataEntry<Object> entry = new IntervalTimerMetaDataEntry<>(context, start, interval);
        entry.setLastTimeout(lastTimeout);
        return entry;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, IntervalTimerMetaDataEntry<Object> entry) throws IOException {
        Object context = entry.getContext();
        if (context != null) {
            writer.writeObject(INFO_INDEX, context);
        }
        Instant start = entry.getStart();
        if (!start.equals(DEFAULT_START)) {
            writer.writeObject(START_INDEX, start);
        }
        Duration lastTimeout = entry.getLastTimeout();
        if (lastTimeout != null) {
            writer.writeObject(LAST_TIMEOUT_INDEX, lastTimeout);
        }
        Duration interval = entry.getInterval();
        if (interval != null) {
            writer.writeObject(INTERVAL_INDEX, interval);
        }
    }
}
