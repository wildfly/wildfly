/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.TreeMap;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * ProtoStream marshaller for {@link MapComputeFunction}.
 * @author Paul Ferraro
 */
public class MapComputeFunctionMarshaller implements ProtoStreamMarshaller<MapComputeFunction<Object, Object>> {
    private static final int ENTRY_INDEX = 1;

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends MapComputeFunction<Object, Object>> getJavaClass() {
        return (Class<MapComputeFunction<Object, Object>>) (Class<?>) MapComputeFunction.class;
    }

    @Override
    public MapComputeFunction<Object, Object> readFrom(ProtoStreamReader reader) throws IOException {
        Map<Object, Object> map = new TreeMap<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case ENTRY_INDEX:
                    Map.Entry<Object, Object> entry = reader.readObject(SimpleImmutableEntry.class);
                    map.put(entry.getKey(), entry.getValue());
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        return new MapComputeFunction<>(map);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, MapComputeFunction<Object, Object> function) throws IOException {
        for (Map.Entry<Object, Object> entry : function.getOperand().entrySet()) {
            writer.writeObject(ENTRY_INDEX, new SimpleImmutableEntry<>(entry));
        }
    }
}
