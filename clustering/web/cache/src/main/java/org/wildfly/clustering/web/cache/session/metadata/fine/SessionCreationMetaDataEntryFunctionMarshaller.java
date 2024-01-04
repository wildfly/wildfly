/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntryFunctionMarshaller implements ProtoStreamMarshaller<SessionAccessMetaDataEntryFunction> {

    private static final int SINCE_CREATION_OFFSET_INDEX = 1;
    private static final int LAST_ACCESS_OFFSET_INDEX = 2;

    @Override
    public Class<? extends SessionAccessMetaDataEntryFunction> getJavaClass() {
        return SessionAccessMetaDataEntryFunction.class;
    }

    @Override
    public SessionAccessMetaDataEntryFunction readFrom(ProtoStreamReader reader) throws IOException {
        AtomicReference<Offset<Duration>> sinceCreationOffset = new AtomicReference<>(Offset.forDuration(Duration.ZERO));
        AtomicReference<Offset<Duration>> lastAccessOffset = new AtomicReference<>(Offset.forDuration(Duration.ZERO));
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case SINCE_CREATION_OFFSET_INDEX:
                    sinceCreationOffset.setPlain(reader.readObject(sinceCreationOffset.getPlain().getClass()));
                    break;
                case LAST_ACCESS_OFFSET_INDEX:
                    lastAccessOffset.setPlain(reader.readObject(lastAccessOffset.getPlain().getClass()));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SessionAccessMetaDataEntryFunction(new SessionAccessMetaDataEntryOffsets() {
            @Override
            public Offset<Duration> getSinceCreationOffset() {
                return sinceCreationOffset.getPlain();
            }

            @Override
            public Offset<Duration> getLastAccessOffset() {
                return lastAccessOffset.getPlain();
            }
        });
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SessionAccessMetaDataEntryFunction function) throws IOException {
        SessionAccessMetaDataEntryOffsets offsets = function.getOperand();
        Offset<Duration> sinceCreationOffset = offsets.getSinceCreationOffset();
        if (!sinceCreationOffset.isZero()) {
            writer.writeObject(SINCE_CREATION_OFFSET_INDEX, sinceCreationOffset);
        }
        Offset<Duration> lastAccessOffset = offsets.getLastAccessOffset();
        if (!lastAccessOffset.isZero()) {
            writer.writeObject(LAST_ACCESS_OFFSET_INDEX, lastAccessOffset);
        }
    }
}
