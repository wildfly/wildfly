/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.fine;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValue;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapEntryMarshaller implements ProtoStreamMarshaller<SessionAttributeMapEntry<ByteBufferMarshalledValue<Object>>> {

    private static final int NAME_INDEX = 1;
    private static final int VALUE_INDEX = 2;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SessionAttributeMapEntry<ByteBufferMarshalledValue<Object>>> getJavaClass() {
        return (Class<SessionAttributeMapEntry<ByteBufferMarshalledValue<Object>>>) (Class<?>) SessionAttributeMapEntry.class;
    }

    @Override
    public SessionAttributeMapEntry<ByteBufferMarshalledValue<Object>> readFrom(ProtoStreamReader reader) throws IOException {
        String name = null;
        ByteBufferMarshalledValue<Object> value = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case NAME_INDEX:
                    name = reader.readString();
                    break;
                case VALUE_INDEX:
                    value = reader.readObject(ByteBufferMarshalledValue.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SessionAttributeMapEntry<>(name, value);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SessionAttributeMapEntry<ByteBufferMarshalledValue<Object>> entry) throws IOException {
        String name = entry.getKey();
        if (name != null) {
            writer.writeString(NAME_INDEX, name);
        }
        ByteBufferMarshalledValue<Object> value = entry.getValue();
        if (value != null) {
            writer.writeObject(VALUE_INDEX, value);
        }
    }
}
