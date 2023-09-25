/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.immutable;

import org.wildfly.clustering.ee.Immutability;

/**
 * Test for immutability using instanceof checks.
 * @author Paul Ferraro
 */
public class InstanceOfImmutability implements Immutability {

    private final Iterable<Class<?>> immutableClasses;

    public InstanceOfImmutability(Iterable<Class<?>> immutableClasses) {
        this.immutableClasses = immutableClasses;
    }

    @Override
    public boolean test(Object object) {
        for (Class<?> immutableClass : this.immutableClasses) {
            if (immutableClass.isInstance(object)) return true;
        }
        return false;
    }
}
