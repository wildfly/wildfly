/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

/**
 * Marshaller for an object whose fields are marshalled via a {@link FieldSetMarshaller} and constructed via a {@link ProtoStreamBuilder}.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 * @param <B> the builder type used by this marshaller
 */
public class ProtoStreamBuilderFieldSetMarshaller<T, B extends ProtoStreamBuilder<T>> extends FunctionalFieldSetMarshaller<T, B> {

    @SuppressWarnings("unchecked")
    public ProtoStreamBuilderFieldSetMarshaller(FieldSetMarshaller<T, B> marshaller) {
        super((Class<T>) marshaller.getBuilder().build().getClass(), marshaller, B::build);
    }

    public ProtoStreamBuilderFieldSetMarshaller(Class<? extends T> targetClass, FieldSetMarshaller<T, B> marshaller) {
        super(targetClass, marshaller, B::build);
    }
}
