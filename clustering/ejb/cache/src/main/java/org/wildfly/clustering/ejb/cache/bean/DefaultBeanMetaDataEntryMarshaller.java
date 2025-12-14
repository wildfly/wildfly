/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.server.offset.OffsetValue;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link DefaultBeanMetaDataEntry}.
 * @author Paul Ferraro
 */
public class DefaultBeanMetaDataEntryMarshaller implements ProtoStreamMarshaller<DefaultBeanMetaDataEntry<SessionID>> {

    private static final int NAME_INDEX = 1;
    private static final int GROUP_IDENTIFIER_INDEX = 2;
    private static final int CREATION_TIME_INDEX = 3;
    private static final int LAST_ACCESS_OFFSET_INDEX = 4;
    private static final Instant DEFAULT_CREATION_TIME = Instant.EPOCH;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends DefaultBeanMetaDataEntry<SessionID>> getJavaClass() {
        return (Class<DefaultBeanMetaDataEntry<SessionID>>) (Class<?>) DefaultBeanMetaDataEntry.class;
    }

    @Override
    public DefaultBeanMetaDataEntry<SessionID> readFrom(ProtoStreamReader reader) throws IOException {
        String name = null;
        SessionID groupId = null;
        Instant creationTime = Instant.EPOCH;
        Offset<Instant> lastAccessOffset = Offset.forInstant(Duration.ZERO);
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case NAME_INDEX:
                    name = reader.readString();
                    break;
                case GROUP_IDENTIFIER_INDEX:
                    groupId = reader.readObject(SessionID.class);
                    break;
                case CREATION_TIME_INDEX:
                    creationTime = reader.readObject(Instant.class);
                    break;
                case LAST_ACCESS_OFFSET_INDEX:
                    lastAccessOffset = reader.readObject(lastAccessOffset.getClass().asSubclass(Offset.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        DefaultBeanMetaDataEntry<SessionID> entry = new DefaultBeanMetaDataEntry<>(name, groupId, creationTime);
        entry.getLastAccessTime().setOffset(lastAccessOffset);
        return entry;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, DefaultBeanMetaDataEntry<SessionID> metaData) throws IOException {
        String name = metaData.getName();
        if (name != null) {
            writer.writeString(NAME_INDEX, name);
        }
        SessionID groupId = metaData.getGroupId();
        if (groupId != null) {
            writer.writeObject(GROUP_IDENTIFIER_INDEX, groupId);
        }
        OffsetValue<Instant> lastAccess = metaData.getLastAccessTime();
        Instant creationTime = lastAccess.getBasis();
        if (!DEFAULT_CREATION_TIME.equals(creationTime)) {
            writer.writeObject(CREATION_TIME_INDEX, creationTime);
        }
        Offset<Instant> lastAccessOffset = lastAccess.getOffset();
        if (!lastAccessOffset.isZero()) {
            writer.writeObject(LAST_ACCESS_OFFSET_INDEX, lastAccessOffset);
        }
    }
}
