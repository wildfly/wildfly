/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Map;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for implementations of {@link Map} constructed with some context.
 * @author Paul Ferraro
 */
public class ContextualMapExternalizer<T extends Map<Object, Object>, C> extends MapExternalizer<T, C, C> {

    public ContextualMapExternalizer(Class<T> targetClass, Function<C, T> factory, Function<T, C> context, Externalizer<C> contextExternalizer) {
        super(targetClass, factory, Map.Entry::getKey, context, contextExternalizer);
    }
}
