/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.IdentityFunction;
import org.wildfly.clustering.marshalling.spi.ObjectExternalizer;

/**
 * Externalizers for implementations of {@link SortedMap}.
 * Requires additional serialization of the comparator.
 * @author Paul Ferraro
 */
public class SortedMapExternalizer<T extends SortedMap<Object, Object>> extends ContextualMapExternalizer<T, Comparator<Object>> {
    @SuppressWarnings("unchecked")
    private static final Externalizer<Comparator<Object>> COMPARATOR_EXTERNALIZER = (Externalizer<Comparator<Object>>) (Externalizer<?>) new ObjectExternalizer<>(Comparator.class, Comparator.class::cast, IdentityFunction.getInstance());

    public SortedMapExternalizer(Class<T> targetClass, Function<Comparator<Object>, T> factory) {
        super(targetClass, factory, SortedMap::comparator, COMPARATOR_EXTERNALIZER);
    }
}
