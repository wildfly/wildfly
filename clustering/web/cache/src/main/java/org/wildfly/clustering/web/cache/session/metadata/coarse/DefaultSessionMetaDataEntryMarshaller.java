/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Protostream marshaller for a {@link DefaultSessionMetaDataEntry}.
 * @author Paul Ferraro
 */
public class DefaultSessionMetaDataEntryMarshaller implements ProtoStreamMarshaller<DefaultSessionMetaDataEntry<Object>> {

    private static final Instant DEFAULT_CREATION_TIME = Instant.EPOCH;
    // Optimize for specification default
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30L);
    // Optimize for new sessions
    private static final Offset<Instant> DEFAULT_LAST_ACCESS_START_TIME_OFFSET = Offset.forInstant(Duration.ZERO);
    // Optimize for sub-second request duration
    private static final Offset<Instant> DEFAULT_LAST_ACCESS_END_TIME_OFFSET = Offset.forInstant(ChronoUnit.SECONDS.getDuration());

    private static final int CREATION_TIME_INDEX = 1;
    private static final int TIMEOUT_INDEX = 2;
    private static final int LAST_ACCESS_START_TIME_OFFSET_INDEX = 3;
    private static final int LAST_ACCESS_END_TIME_OFFSET_INDEX = 4;

    @Override
    public DefaultSessionMetaDataEntry<Object> readFrom(ProtoStreamReader reader) throws IOException {
        Instant creationTime = DEFAULT_CREATION_TIME;
        Duration timeout = DEFAULT_TIMEOUT;
        Offset<Instant> lastAccessStartTimeOffset = DEFAULT_LAST_ACCESS_START_TIME_OFFSET;
        Offset<Instant> lastAccessEndTimeOffset = DEFAULT_LAST_ACCESS_END_TIME_OFFSET;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CREATION_TIME_INDEX:
                    creationTime = reader.readObject(Instant.class);
                    break;
                case TIMEOUT_INDEX:
                    timeout = reader.readObject(Duration.class);
                    break;
                case LAST_ACCESS_START_TIME_OFFSET_INDEX:
                    lastAccessStartTimeOffset = reader.readObject(lastAccessStartTimeOffset.getClass());
                    break;
                case LAST_ACCESS_END_TIME_OFFSET_INDEX:
                    lastAccessEndTimeOffset = reader.readObject(lastAccessEndTimeOffset.getClass());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        DefaultSessionMetaDataEntry<Object> metaData = new DefaultSessionMetaDataEntry<>(creationTime);
        metaData.setTimeout(timeout);
        metaData.getLastAccessStartTime().setOffset(lastAccessStartTimeOffset);
        metaData.getLastAccessEndTime().setOffset(lastAccessEndTimeOffset);
        return metaData;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, DefaultSessionMetaDataEntry<Object> metaData) throws IOException {

        Instant creationTime = metaData.getCreationTime();
        if (!creationTime.equals(DEFAULT_CREATION_TIME)) {
            writer.writeObject(CREATION_TIME_INDEX, creationTime);
        }

        Duration timeout = metaData.getTimeout();
        if (!timeout.equals(DEFAULT_TIMEOUT)) {
            writer.writeObject(TIMEOUT_INDEX, timeout);
        }

        Offset<Instant> lastAccessStartTimeOffset = metaData.getLastAccessStartTime().getOffset();
        if (!lastAccessStartTimeOffset.equals(DEFAULT_LAST_ACCESS_START_TIME_OFFSET)) {
            writer.writeObject(LAST_ACCESS_START_TIME_OFFSET_INDEX, lastAccessStartTimeOffset);
        }

        Offset<Instant> lastAccessEndTimeOffset = metaData.getLastAccessEndTime().getOffset();
        if (!lastAccessEndTimeOffset.equals(DEFAULT_LAST_ACCESS_END_TIME_OFFSET)) {
            writer.writeObject(LAST_ACCESS_END_TIME_OFFSET_INDEX, lastAccessEndTimeOffset);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends DefaultSessionMetaDataEntry<Object>> getJavaClass() {
        return (Class<DefaultSessionMetaDataEntry<Object>>) (Class<?>) DefaultSessionMetaDataEntry.class;
    }
}
