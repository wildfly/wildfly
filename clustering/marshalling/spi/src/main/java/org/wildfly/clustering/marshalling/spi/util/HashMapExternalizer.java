/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Map;
import java.util.function.IntFunction;

import org.wildfly.clustering.marshalling.spi.ValueFunction;
import org.wildfly.clustering.marshalling.spi.util.HashSetExternalizer.CapacityFactory;
import org.wildfly.clustering.marshalling.spi.ValueExternalizer;

/**
 * @author Paul Ferraro
 */
public class HashMapExternalizer<T extends Map<Object, Object>> extends MapExternalizer<T, Void, Integer> {

    public HashMapExternalizer(Class<T> targetClass, IntFunction<T> factory) {
        super(targetClass, new CapacityFactory<>(factory), Map.Entry::getValue, ValueFunction.voidFunction(), ValueExternalizer.VOID);
    }
}
