/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for implementations of {@link Collection} constructed with some context.
 * @author Paul Ferraro
 */
public class ContextualCollectionExternalizer<T extends Collection<Object>, C> extends CollectionExternalizer<T, C, C> {

    public ContextualCollectionExternalizer(Class<T> targetClass, Function<C, T> factory, Function<T, C> context, Externalizer<C> contextExternalizer) {
        super(targetClass, factory, Map.Entry::getKey, context, contextExternalizer);
    }
}
