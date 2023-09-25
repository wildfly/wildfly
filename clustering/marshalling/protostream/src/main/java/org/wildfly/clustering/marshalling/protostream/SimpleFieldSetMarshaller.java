/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.function.Function;

/**
 * Marshaller for an object whose fields are marshalled via a {@link FieldSetMarshaller} whose construction is sufficiently simple as to not require a separate builder.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public class SimpleFieldSetMarshaller<T> extends FunctionalFieldSetMarshaller<T, T> {

    @SuppressWarnings("unchecked")
    public SimpleFieldSetMarshaller(FieldSetMarshaller<T, T> marshaller) {
        this((Class<T>) marshaller.getBuilder().getClass(), marshaller);
    }

    public SimpleFieldSetMarshaller(Class<? extends T> targetClass, FieldSetMarshaller<T, T> marshaller) {
        super(targetClass, marshaller, Function.identity());
    }
}
