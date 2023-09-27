/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.io.IOException;
import java.time.Duration;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataMarshaller implements ProtoStreamMarshaller<SimpleSessionAccessMetaData> {

    // Optimize for new sessions
    private static final Duration DEFAULT_SINCE_CREATION = Duration.ZERO;
    // Optimize for sub-second request duration
    private static final Duration DEFAULT_LAST_ACCESS = Duration.ofSeconds(1);

    private static final int SINCE_CREATION_INDEX = 1;
    private static final int LAST_ACCESS_INDEX = 2;

    @Override
    public SimpleSessionAccessMetaData readFrom(ProtoStreamReader reader) throws IOException {
        Duration sinceCreation = DEFAULT_SINCE_CREATION;
        Duration lastAccess = DEFAULT_LAST_ACCESS;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case SINCE_CREATION_INDEX:
                    sinceCreation = reader.readObject(Duration.class);
                    break;
                case LAST_ACCESS_INDEX:
                    lastAccess = reader.readObject(Duration.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        SimpleSessionAccessMetaData metaData = new SimpleSessionAccessMetaData();
        metaData.setLastAccessDuration(sinceCreation, lastAccess);
        return metaData;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SimpleSessionAccessMetaData metaData) throws IOException {
        Duration sinceCreation = metaData.getSinceCreationDuration();
        if (!sinceCreation.equals(DEFAULT_SINCE_CREATION)) {
            writer.writeObject(SINCE_CREATION_INDEX, sinceCreation);
        }

        Duration lastAccess = metaData.getLastAccessDuration();
        if (!lastAccess.equals(DEFAULT_LAST_ACCESS)) {
            writer.writeObject(LAST_ACCESS_INDEX, lastAccess);
        }
    }

    @Override
    public Class<? extends SimpleSessionAccessMetaData> getJavaClass() {
        return SimpleSessionAccessMetaData.class;
    }
}
