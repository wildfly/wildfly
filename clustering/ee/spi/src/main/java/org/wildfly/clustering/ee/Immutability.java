/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee;

import java.util.function.Predicate;

/**
 * Tests for immutability.
 * @author Paul Ferraro
 */
public interface Immutability extends Predicate<Object> {

    @Override
    default Immutability and(Predicate<? super Object> immutability) {
        return new Immutability() {
            @Override
            public boolean test(Object object) {
                return Immutability.this.test(object) && immutability.test(object);
            }
        };
    }

    @Override
    default Immutability negate() {
        return new Immutability() {
            @Override
            public boolean test(Object object) {
                return !Immutability.this.test(object);
            }
        };
    }

    @Override
    default Immutability or(Predicate<? super Object> immutability) {
        return new Immutability() {
            @Override
            public boolean test(Object object) {
                return Immutability.this.test(object) || immutability.test(object);
            }
        };
    }
}
