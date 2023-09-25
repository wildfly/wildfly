/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for a {@link ScheduleWithMetaDataCommand}.
 * @author Paul Ferraro
 */
public class ScheduleWithMetaDataCommandMarshaller<I, M> implements ProtoStreamMarshaller<ScheduleWithMetaDataCommand<I, M>> {

    private static final byte ID_INDEX = 1;
    private static final byte META_DATA_INDEX = 2;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ScheduleWithMetaDataCommand<I, M>> getJavaClass() {
        return (Class<ScheduleWithMetaDataCommand<I, M>>) (Class<?>) ScheduleWithMetaDataCommand.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ScheduleWithMetaDataCommand<I, M> readFrom(ProtoStreamReader reader) throws IOException {
        I id = null;
        M metaData = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ID_INDEX:
                    id = (I) reader.readAny();
                    break;
                case META_DATA_INDEX:
                    metaData = (M) reader.readAny();
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new ScheduleWithMetaDataCommand<>(id, metaData);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ScheduleWithMetaDataCommand<I, M> command) throws IOException {
        I id = command.getId();
        if (id != null) {
            writer.writeAny(ID_INDEX, id);
        }
        M metaData = command.getMetaData();
        if (metaData != null) {
            writer.writeAny(META_DATA_INDEX, metaData);
        }
    }
}
