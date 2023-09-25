/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

/**
 * @author Paul Ferraro
 */
public interface MarshalledValueFactory<C> extends Marshallability {
    <T> MarshalledValue<T, C> createMarshalledValue(T object);

    C getMarshallingContext();
}