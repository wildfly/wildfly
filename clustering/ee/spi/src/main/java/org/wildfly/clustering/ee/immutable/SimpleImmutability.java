/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.immutable;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;

import org.wildfly.clustering.ee.Immutability;

/**
 * Immutability implementation based on a pre-defined set immutable classes.
 * @author Paul Ferraro
 */
public class SimpleImmutability implements Immutability {

    private final Set<Class<?>> immutableClasses;

    public SimpleImmutability(ClassLoader loader, Collection<String> immutableClassNames) {
        this(immutableClassNames, new Function<String, Class<?>>() {
            @Override
            public Class<?> apply(String className) {
                try {
                    return loader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(className, e);
                }
            }
        });
    }

    public SimpleImmutability(Collection<Class<?>> immutableClasses) {
        this(immutableClasses, Function.identity());
    }

    private <T> SimpleImmutability(Collection<T> immutables, Function<T, Class<?>> operator) {
        this.immutableClasses = !immutables.isEmpty() ? Collections.newSetFromMap(new IdentityHashMap<>(immutables.size())) : Collections.emptySet();
        for (T immutable : immutables) {
            this.immutableClasses.add(operator.apply(immutable));
        }
    }

    public SimpleImmutability(Set<Class<?>> classes) {
        this.immutableClasses = classes;
    }

    @Override
    public boolean test(Object object) {
        return (object == null) || this.immutableClasses.contains(object.getClass());
    }
}
