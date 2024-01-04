/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.ImmutableSerializationContext;

/**
 * Common interface of {@link ProtoStreamReader} and {@link ProtoStreamWriter}.
 * @author Paul Ferraro
 */
public interface ProtoStreamOperation {

    interface Context {
        /**
         * Records the specified object, so that it can be referenced later within the same stream
         * @param object an object
         */
        void record(Object object);
    }

    /**
     * Returns the context of this operation
     * @return the operation context
     */
    ProtoStreamOperation.Context getContext();

    /**
     * Returns the serialization context of the associated marshaller.
     * @return an immutable serialization context
     */
    ImmutableSerializationContext getSerializationContext();

    /**
     * Returns a marshaller suitable of marshalling an object of the specified type.
     * @param <T> the type of the associated marshaller
     * @param <V> the type of the object to be marshalled
     * @param javaClass the type of the value to be written.
     * @return a marshaller suitable for the specified type
     * @throws IllegalArgumentException if no suitable marshaller exists
     */
    @SuppressWarnings("unchecked")
    default <T, V extends T> ProtoStreamMarshaller<T> findMarshaller(Class<V> javaClass) {
        ImmutableSerializationContext context = this.getSerializationContext();
        Class<?> targetClass = javaClass;
        IllegalArgumentException exception = null;
        while (targetClass != null) {
            try {
                return (ProtoStreamMarshaller<T>) context.getMarshaller((Class<T>) targetClass);
            } catch (IllegalArgumentException e) {
                // If no marshaller was found, check super class
                if (exception == null) {
                    exception = e;
                }
                targetClass = targetClass.getSuperclass();
            }
        }
        throw exception;
    }
}
