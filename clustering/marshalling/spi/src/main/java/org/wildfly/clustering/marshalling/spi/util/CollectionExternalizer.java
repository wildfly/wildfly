/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * Generic externalizer for implementations of {@link Collection}.
 * @author Paul Ferraro
 */
public class CollectionExternalizer<T extends Collection<Object>, C, CC> implements Externalizer<T> {

    private final Class<T> targetClass;
    private final Function<CC, T> factory;
    private final Function<Map.Entry<C, Integer>, CC> constructorContext;
    private final Function<T, C> context;
    private final Externalizer<C> contextExternalizer;

    public CollectionExternalizer(Class<T> targetClass, Function<CC, T> factory, Function<Map.Entry<C, Integer>, CC> constructorContext, Function<T, C> context, Externalizer<C> contextExternalizer) {
        this.targetClass = targetClass;
        this.factory = factory;
        this.constructorContext = constructorContext;
        this.context = context;
        this.contextExternalizer = contextExternalizer;
    }

    @Override
    public void writeObject(ObjectOutput output, T collection) throws IOException {
        synchronized (collection) { // Avoid ConcurrentModificationException
            C context = this.context.apply(collection);
            this.contextExternalizer.writeObject(output, context);
            IndexSerializer.VARIABLE.writeInt(output, collection.size());
            for (Object element : collection) {
                output.writeObject(element);
            }
        }
    }

    @Override
    public T readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        C context = this.contextExternalizer.readObject(input);
        int size = IndexSerializer.VARIABLE.readInt(input);
        CC constructorContext = this.constructorContext.apply(new AbstractMap.SimpleImmutableEntry<>(context, size));
        T collection = this.factory.apply(constructorContext);
        for (int i = 0; i < size; ++i) {
            collection.add(input.readObject());
        }
        return collection;
    }

    @Override
    public OptionalInt size(T collection) {
        if (!collection.isEmpty()) return OptionalInt.empty();
        synchronized (collection) { // Avoid ConcurrentModificationException
            C context = this.context.apply(collection);
            OptionalInt contextSize = this.contextExternalizer.size(context);
            return contextSize.isPresent() ? OptionalInt.of(contextSize.getAsInt() + IndexSerializer.VARIABLE.size(collection.size())) : OptionalInt.empty();
        }
    }

    @Override
    public Class<T> getTargetClass() {
        return this.targetClass;
    }
}
