/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.immutable;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.wildfly.clustering.ee.Immutability;

/**
 * Test for immutability using object identity.
 * @author Paul Ferraro
 */
public class IdentityImmutability implements Immutability {

    private final Set<Object> immutableObjects;

    public IdentityImmutability(Collection<Object> objects) {
        this.immutableObjects = !objects.isEmpty() ? Collections.newSetFromMap(new IdentityHashMap<>(objects.size())) : Collections.emptySet();
        for (Object object : objects) {
            this.immutableObjects.add(object);
        }
    }

    @Override
    public boolean test(Object object) {
        return (object == null) || this.immutableObjects.contains(object);
    }
}
