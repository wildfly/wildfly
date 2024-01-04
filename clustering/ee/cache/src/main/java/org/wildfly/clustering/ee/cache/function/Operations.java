/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Defines operations for creating and copying an operable object.
 * @author Paul Ferraro
 * @param <T> the operable object type
 */
public interface Operations<T> {

    UnaryOperator<T> getCopier();

    Supplier<T> getFactory();

    Predicate<T> isEmpty();
}
