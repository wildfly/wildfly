/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.Map;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * Abstract marshaller for a {@link Map} that writes the entries of the map.
 * @author Paul Ferraro
 * @param <T> the map type of this marshaller
 */
public abstract class AbstractMapMarshaller<T extends Map<Object, Object>> implements ProtoStreamMarshaller<T> {
    protected static final int KEY_INDEX = 1;
    protected static final int VALUE_INDEX = 2;

    private final Class<? extends T> mapClass;

    public AbstractMapMarshaller(Class<? extends T> mapClass) {
        this.mapClass = mapClass;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T map) throws IOException {
        synchronized (map) { // Avoid ConcurrentModificationException
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                writer.writeAny(KEY_INDEX, entry.getKey());
                writer.writeAny(VALUE_INDEX, entry.getValue());
            }
        }
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.mapClass;
    }
}
