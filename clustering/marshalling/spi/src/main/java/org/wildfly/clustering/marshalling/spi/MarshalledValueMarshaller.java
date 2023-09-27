/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;

/**
 * Marshaller that stores attribute values using marshalled values.
 * @author Paul Ferraro
 */
public class MarshalledValueMarshaller<V, C> implements Marshaller<V, MarshalledValue<V, C>> {
    private final MarshalledValueFactory<C> factory;

    public MarshalledValueMarshaller(MarshalledValueFactory<C> factory) {
        this.factory = factory;
    }

    @Override
    public V read(MarshalledValue<V, C> value) throws IOException {
        if (value == null) return null;
        return value.get(this.factory.getMarshallingContext());
    }

    @Override
    public MarshalledValue<V, C> write(V object) {
        if (object == null) return null;
        return this.factory.createMarshalledValue(object);
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.factory.isMarshallable(object);
    }
}
