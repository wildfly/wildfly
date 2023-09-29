/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.cache.function;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author Paul Ferraro
 */
public interface CollectionOperations<V, C extends Collection<V>> extends Operations<C> {

    @Override
    default Predicate<C> isEmpty() {
        return Collection::isEmpty;
    }
}
