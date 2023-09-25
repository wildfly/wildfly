/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.immutable;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.wildfly.clustering.ee.Immutability;

/**
 * Decorates a series of immutability predicates to additionally test for collection immutability.
 * @author Paul Ferraro
 */
public class CompositeImmutability implements Immutability {

    private final Iterable<? extends Immutability> immutabilities;
    private final Immutability collectionImmutability;

    public CompositeImmutability(Immutability... predicates) {
        this(Arrays.asList(predicates));
    }

    public CompositeImmutability(Iterable<? extends Immutability> immutabilities) {
        this.immutabilities = immutabilities;
        this.collectionImmutability = new CollectionImmutability(this);
    }

    @Override
    public boolean test(Object object) {
        if (object == null) return true;
        // Short-circuit test if object is an array
        if (object.getClass().isArray()) {
            return Array.getLength(object) == 0;
        }
        for (Immutability immutability : this.immutabilities) {
            if (immutability.test(object)) {
                return true;
            }
        }
        return this.collectionImmutability.test(object);
    }
}
