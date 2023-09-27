/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * @author Paul Ferraro
 */
public interface ExternalizerProvider extends Externalizer<Object> {

    Externalizer<?> getExternalizer();

    @Override
    default void writeObject(ObjectOutput output, Object object) throws IOException {
        this.cast(Object.class).writeObject(output, object);
    }

    @Override
    default Object readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return this.getExternalizer().readObject(input);
    }

    @Override
    default Class<Object> getTargetClass() {
        return this.cast(Object.class).getTargetClass();
    }

    @Override
    default OptionalInt size(Object object) {
        return this.cast(Object.class).size(object);
    }

    @SuppressWarnings("unchecked")
    default <T> Externalizer<T> cast(Class<T> type) {
        if (!type.isAssignableFrom(this.getExternalizer().getTargetClass())) {
            throw new IllegalArgumentException(type.getName());
        }
        return (Externalizer<T>) this.getExternalizer();
    }
}
