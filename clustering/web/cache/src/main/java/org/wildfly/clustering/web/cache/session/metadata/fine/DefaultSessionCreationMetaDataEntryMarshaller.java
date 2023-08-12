/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Marshaller for a {@link DefaultSessionCreationMetaDataEntry}.
 * @author Paul Ferraro
 */
public class DefaultSessionCreationMetaDataEntryMarshaller implements ProtoStreamMarshaller<DefaultSessionCreationMetaDataEntry<Object>> {

    private static final Instant DEFAULT_CREATION_TIME = Instant.EPOCH;
    // Optimize for specification default
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30L);

    private static final int CREATION_TIME_INDEX = 1;
    private static final int TIMEOUT_INDEX = 2;

    @Override
    public DefaultSessionCreationMetaDataEntry<Object> readFrom(ProtoStreamReader reader) throws IOException {
        Instant creationTime = DEFAULT_CREATION_TIME;
        Duration timeout = DEFAULT_TIMEOUT;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CREATION_TIME_INDEX:
                    creationTime = reader.readObject(Instant.class);
                    break;
                case TIMEOUT_INDEX:
                    timeout = reader.readObject(Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        DefaultSessionCreationMetaDataEntry<Object> result = new DefaultSessionCreationMetaDataEntry<>(creationTime);
        result.setTimeout(timeout);
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, DefaultSessionCreationMetaDataEntry<Object> metaData) throws IOException {

        Instant creationTime = metaData.getCreationTime();
        if (!creationTime.equals(DEFAULT_CREATION_TIME)) {
            writer.writeObject(CREATION_TIME_INDEX, creationTime);
        }

        Duration timeout = metaData.getTimeout();
        if (!timeout.equals(DEFAULT_TIMEOUT)) {
            writer.writeObject(TIMEOUT_INDEX, timeout);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends DefaultSessionCreationMetaDataEntry<Object>> getJavaClass() {
        return (Class<DefaultSessionCreationMetaDataEntry<Object>>) (Class<?>) DefaultSessionCreationMetaDataEntry.class;
    }
}
