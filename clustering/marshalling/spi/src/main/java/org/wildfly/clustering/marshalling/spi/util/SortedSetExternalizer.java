/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IdentityFunction;
import org.wildfly.clustering.marshalling.spi.ObjectExternalizer;

/**
 * Externalizers for implementations of {@link SortedSet}.
 * Requires additional serialization of the comparator.
 * @author Paul Ferraro
 */
public class SortedSetExternalizer<T extends SortedSet<Object>> extends ContextualCollectionExternalizer<T, Comparator<Object>> {
    @SuppressWarnings("unchecked")
    private static final Externalizer<Comparator<Object>> COMPARATOR_EXTERNALIZER = (Externalizer<Comparator<Object>>) (Externalizer<?>) new ObjectExternalizer<>(Comparator.class, Comparator.class::cast, IdentityFunction.getInstance());

    public SortedSetExternalizer(Class<T> targetClass, Function<Comparator<Object>, T> factory) {
        super(targetClass, factory, SortedSet::comparator, COMPARATOR_EXTERNALIZER);
    }
}
