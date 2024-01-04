/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.function.Supplier;

/**
 * Marshaller for a set of fields, to be shared between multiple marshallers.
 * @author Paul Ferraro
 * @param <T> the writer type
 * @param <V> the reader type
 */
public interface FieldSetMarshaller<T, V> extends FieldReadable<V>, Writable<T> {

    /**
     * Returns a builder for use with {@link #readField(ProtoStreamReader, Object)}.
     * May return a shared instance, if the builder type is immutable, or a new instance, if the builder is mutable.
     * @return a builder.
     */
    V createInitialValue();

    /**
     * Builds the target object from the read value.
     * @param value a read value
     * @return the target object
     */
    T build(V value);

    /**
     * A simple field set marshaller whose reader and writer types are the same
     * @param <T> the marshaller type
     */
    interface Simple<T> extends FieldSetMarshaller<T, T> {

        @Override
        default T build(T value) {
            return value;
        }
    }

    /**
     * A field set marshaller whose reader type supplies the writer type.
     * @param <T> the writer type
     * @param <V> the reader type
     */
    interface Supplied<T, V extends Supplier<T>> extends FieldSetMarshaller<T, V> {

        @Override
        default T build(V value) {
            return value.get();
        }
    }
}
