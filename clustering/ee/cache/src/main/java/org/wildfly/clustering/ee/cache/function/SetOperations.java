/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Defines Set creation and cloning strategies.
 * @author Paul Ferraro
 */
public enum SetOperations implements CollectionOperations<Object, Set<Object>> {

    TREE(TreeSet::new, TreeSet::new),
    HASH(HashSet::new, HashSet::new),
    ;

    static <T> Operations<Set<T>> forOperand(T value) {
        // Prefer TreeSet for its minimal heap requirements
        SetOperations result = (value instanceof Comparable) ? TREE : HASH;
        return result.cast();
    }

    private final Supplier<Set<Object>> factory;
    private final UnaryOperator<Set<Object>> copier;

    SetOperations(Supplier<Set<Object>> factory, UnaryOperator<Set<Object>> copier) {
        this.factory = factory;
        this.copier = copier;
    }

    @Override
    public UnaryOperator<Set<Object>> getCopier() {
        return this.copier;
    }

    @Override
    public Supplier<Set<Object>> getFactory() {
        return this.factory;
    }

    @SuppressWarnings("unchecked")
    <T> Operations<Set<T>> cast() {
        return (Operations<Set<T>>) (Operations<?>) this;
    }
}
