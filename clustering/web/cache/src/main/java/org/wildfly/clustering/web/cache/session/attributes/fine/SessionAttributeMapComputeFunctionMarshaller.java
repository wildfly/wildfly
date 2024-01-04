/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.fine;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeMapComputeFunctionMarshaller<V> implements ProtoStreamMarshaller<SessionAttributeMapComputeFunction<V>> {
    private static final int ENTRY_INDEX = 1;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends SessionAttributeMapComputeFunction<V>> getJavaClass() {
        return (Class<SessionAttributeMapComputeFunction<V>>) (Class<?>) SessionAttributeMapComputeFunction.class;
    }

    @Override
    public SessionAttributeMapComputeFunction<V> readFrom(ProtoStreamReader reader) throws IOException {
        Map<String, V> map = new TreeMap<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ENTRY_INDEX:
                    Map.Entry<String, V> entry = reader.readObject(SessionAttributeMapEntry.class);
                    map.put(entry.getKey(), entry.getValue());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new SessionAttributeMapComputeFunction<>(map);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SessionAttributeMapComputeFunction<V> function) throws IOException {
        for (Map.Entry<String, V> entry : function.getOperand().entrySet()) {
            writer.writeObject(ENTRY_INDEX, new SessionAttributeMapEntry<>(entry));
        }
    }
}
