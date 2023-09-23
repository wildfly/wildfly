/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.util.OptionalInt;

/**
 * Interface inherited by marshallable components.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public interface Marshallable<T> extends Readable<T>, Writable<T> {

    /**
     * Computes the size of the specified object.
     * @param context the marshalling operation
     * @param value the value whose size is to be calculated
     * @return an optional buffer size, only present if the buffer size could be computed
     */
    default OptionalInt size(ProtoStreamSizeOperation operation, T value) {
        return operation.computeSize(this::writeTo, value);
    }

    /**
     * Returns the type of object handled by this marshallable instance.
     * @return the type of object handled by this marshallable instance.
     */
    Class<? extends T> getJavaClass();
}
