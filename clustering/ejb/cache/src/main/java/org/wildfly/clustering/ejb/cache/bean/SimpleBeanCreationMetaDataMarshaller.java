/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.io.IOException;
import java.time.Instant;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link SimpleBeanCreationMetaData}.
 * @author Paul Ferraro
 */
public class SimpleBeanCreationMetaDataMarshaller implements ProtoStreamMarshaller<SimpleBeanCreationMetaData<SessionID>> {

    private static final int NAME_INDEX = 1;
    private static final int GROUP_IDENTIFIER_INDEX = 2;
    private static final int CREATION_TIME_INDEX = 3;
    private static final Instant DEFAULT_CREATION_TIME = Instant.EPOCH;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SimpleBeanCreationMetaData<SessionID>> getJavaClass() {
        return (Class<SimpleBeanCreationMetaData<SessionID>>) (Class<?>) SimpleBeanCreationMetaData.class;
    }

    @Override
    public SimpleBeanCreationMetaData<SessionID> readFrom(ProtoStreamReader reader) throws IOException {
        String name = null;
        SessionID groupId = null;
        Instant creationTime = Instant.EPOCH;
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
                default:
                    reader.skipField(tag);
            }
        }
        return new SimpleBeanCreationMetaData<>(name, groupId, creationTime);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SimpleBeanCreationMetaData<SessionID> metaData) throws IOException {
        String name = metaData.getName();
        if (name != null) {
            writer.writeString(NAME_INDEX, name);
        }
        SessionID groupId = metaData.getGroupId();
        if (groupId != null) {
            writer.writeObject(GROUP_IDENTIFIER_INDEX, groupId);
        }
        Instant creationTime = metaData.getCreationTime();
        if (!DEFAULT_CREATION_TIME.equals(creationTime)) {
            writer.writeObject(CREATION_TIME_INDEX, creationTime);
        }
    }
}
