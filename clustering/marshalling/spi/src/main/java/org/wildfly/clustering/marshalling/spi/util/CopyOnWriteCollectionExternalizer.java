/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalInt;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for copy-on-write implementations of {@link Collection}.
 * @author Paul Ferraro
 */
public class CopyOnWriteCollectionExternalizer<T extends Collection<Object>> implements Externalizer<T> {
    @SuppressWarnings("unchecked")
    private static final Externalizer<Collection<Object>> COLLECTION_EXTERNALIZER = new BoundedCollectionExternalizer<>((Class<Collection<Object>>) (Class<?>) ArrayList.class, ArrayList::new);

    private final Class<T> targetClass;
    private final Function<Collection<Object>, T> factory;

    @SuppressWarnings("unchecked")
    public CopyOnWriteCollectionExternalizer(Class<?> targetClass, Function<Collection<Object>, T> factory) {
        this.targetClass = (Class<T>) targetClass;
        this.factory = factory;
    }

    @Override
    public OptionalInt size(T collection) {
        return COLLECTION_EXTERNALIZER.size(collection);
    }

    @Override
    public void writeObject(ObjectOutput output, T collection) throws IOException {
        COLLECTION_EXTERNALIZER.writeObject(output, collection);
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        // Collect all elements first to avoid COW costs per element.
        return this.factory.apply(COLLECTION_EXTERNALIZER.readObject(input));
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
