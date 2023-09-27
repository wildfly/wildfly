/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.util.function.Function;

import org.infinispan.protostream.descriptors.WireType;

/**
 * Marshaller for an object whose fields are marshalled via a {@link FieldSetMarshaller}.
 * @param <T> the type of this marshaller
 * @param <B> the builder type for reading embedded fields
 */
public class FunctionalFieldSetMarshaller<T, B> implements ProtoStreamMarshaller<T> {
    private static final int START_INDEX = 1;

    private final Class<? extends T> targetClass;
    private final FieldSetMarshaller<T, B> marshaller;
    private final Function<B, T> build;

    public FunctionalFieldSetMarshaller(Class<? extends T> targetClass, FieldSetMarshaller<T, B> marshaller, Function<B, T> factory) {
        this.targetClass = targetClass;
        this.marshaller = marshaller;
        this.build = factory;
    }

    @Override
    public T readFrom(ProtoStreamReader reader) throws IOException {
        B builder = this.marshaller.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if ((index >= START_INDEX) && (index < START_INDEX + this.marshaller.getFields())) {
                builder = this.marshaller.readField(reader, index - START_INDEX, builder);
            } else {
                reader.skipField(tag);
            }
        }
        return this.build.apply(builder);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        this.marshaller.writeFields(writer, START_INDEX, value);
    }

    @Override
    public Class<? extends T> getJavaClass() {
        return this.targetClass;
    }
}
