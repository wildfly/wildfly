/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Defines Map creation and cloning strategies.
 * @author Paul Ferraro
 */
public enum MapOperations implements Operations<Map<Object, Object>> {

    TREE(TreeMap::new, TreeMap::new),
    HASH(HashMap::new, HashMap::new),
    ;

    static <K, V> Operations<Map<K, V>> forOperandKey(K value) {
        // Prefer TreeMap for its minimal heap requirements
        MapOperations result = (value instanceof Comparable) ? TREE : HASH;
        return result.cast();
    }

    private final Supplier<Map<Object, Object>> factory;
    private final UnaryOperator<Map<Object, Object>> copier;

    MapOperations(Supplier<Map<Object, Object>> factory, UnaryOperator<Map<Object, Object>> copier) {
        this.factory = factory;
        this.copier = copier;
    }

    @Override
    public UnaryOperator<Map<Object, Object>> getCopier() {
        return this.copier;
    }

    @Override
    public Supplier<Map<Object, Object>> getFactory() {
        return this.factory;
    }

    @Override
    public Predicate<Map<Object, Object>> isEmpty() {
        return Map::isEmpty;
    }

    @SuppressWarnings("unchecked")
    <K, V> Operations<Map<K, V>> cast() {
        return (Operations<Map<K, V>>) (Operations<?>) this;
    }
}
