/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.IntFunction;

import org.wildfly.clustering.marshalling.spi.ValueFunction;
import org.wildfly.clustering.marshalling.spi.ValueExternalizer;

/**
 * Externalizer for bounded implementations of {@link Collection}.
 * @author Paul Ferraro
 */
public class BoundedCollectionExternalizer<T extends Collection<Object>> extends CollectionExternalizer<T, Void, Integer> {

    public BoundedCollectionExternalizer(Class<T> targetClass, IntFunction<T> factory) {
        super(targetClass, factory::apply, Map.Entry::getValue, ValueFunction.voidFunction(), ValueExternalizer.VOID);
    }
}
