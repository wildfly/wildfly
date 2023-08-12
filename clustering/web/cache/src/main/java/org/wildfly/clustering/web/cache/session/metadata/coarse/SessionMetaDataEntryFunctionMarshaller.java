/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link SimpleSessionMetaDataDelta}.
 * @author Paul Ferraro
 */
public class SessionMetaDataEntryFunctionMarshaller implements ProtoStreamMarshaller<SessionMetaDataEntryFunction<Object>> {

    private static final int TIMEOUT_OFFSET_INDEX = 1;
    private static final int LAST_ACCESS_START_TIME_OFFSET_INDEX = 2;
    private static final int LAST_ACCESS_END_TIME_OFFSET_INDEX = 3;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SessionMetaDataEntryFunction<Object>> getJavaClass() {
        return (Class<SessionMetaDataEntryFunction<Object>>) (Class<?>) SessionMetaDataEntryFunction.class;
    }

    @Override
    public SessionMetaDataEntryFunction<Object> readFrom(ProtoStreamReader reader) throws IOException {
        AtomicReference<Offset<Duration>> timeoutOffset = new AtomicReference<>(Offset.forDuration(Duration.ZERO));
        AtomicReference<Offset<Instant>> lastAccessStartTimeOffset = new AtomicReference<>(Offset.forInstant(Duration.ZERO));
        AtomicReference<Offset<Instant>> lastAccessEndTimeOffset = new AtomicReference<>(Offset.forInstant(Duration.ZERO));
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case TIMEOUT_OFFSET_INDEX:
                    timeoutOffset.setPlain(reader.readObject(timeoutOffset.getPlain().getClass()));
                    break;
                case LAST_ACCESS_START_TIME_OFFSET_INDEX:
                    lastAccessStartTimeOffset.setPlain(reader.readObject(lastAccessStartTimeOffset.getPlain().getClass()));
                    break;
                case LAST_ACCESS_END_TIME_OFFSET_INDEX:
                    lastAccessEndTimeOffset.setPlain(reader.readObject(lastAccessEndTimeOffset.getPlain().getClass()));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SessionMetaDataEntryFunction<>(new SessionMetaDataEntryOffsets() {
            @Override
            public Offset<Duration> getTimeoutOffset() {
                return timeoutOffset.getPlain();
            }

            @Override
            public Offset<Instant> getLastAccessStartTimeOffset() {
                return lastAccessStartTimeOffset.getPlain();
            }

            @Override
            public Offset<Instant> getLastAccessEndTimeOffset() {
                return lastAccessEndTimeOffset.getPlain();
            }
        });
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SessionMetaDataEntryFunction<Object> function) throws IOException {
        SessionMetaDataEntryOffsets operand = function.getOperand();

        Offset<Duration> timeoutOffset = operand.getTimeoutOffset();
        if (!timeoutOffset.isZero()) {
            writer.writeObject(TIMEOUT_OFFSET_INDEX, timeoutOffset);
        }
        Offset<Instant> lastAccessStartTimeOffset = operand.getLastAccessStartTimeOffset();
        if (!lastAccessStartTimeOffset.isZero()) {
            writer.writeObject(LAST_ACCESS_START_TIME_OFFSET_INDEX, lastAccessStartTimeOffset);
        }
        Offset<Instant> lastAccessEndTimeOffset = operand.getLastAccessEndTimeOffset();
        if (!lastAccessEndTimeOffset.isZero()) {
            writer.writeObject(LAST_ACCESS_END_TIME_OFFSET_INDEX, lastAccessEndTimeOffset);
        }
    }
}
