/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;

/**
 * A field marshaller based on a scaler marshaller.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public class ScalarFieldMarshaller<T> implements FieldMarshaller<T> {

    private final ScalarMarshaller<T> marshaller;

    public ScalarFieldMarshaller(ScalarMarshaller<T> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        T result = this.marshaller.readFrom(reader);
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            reader.skipField(tag);
        }
        return result;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        this.marshaller.writeTo(writer, value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.marshaller.getJavaClass();
    }

    @Override
    public WireType getWireType() {
        return this.marshaller.getWireType();
    }
}
