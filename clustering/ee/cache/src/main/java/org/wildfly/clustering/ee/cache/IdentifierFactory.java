/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee.cache;

import java.util.function.Supplier;

import org.wildfly.clustering.ee.Restartable;

/**
 * Factory for creating unique identifiers suitable for use by the local cluster member.
 * @author Paul Ferraro
 * @param <I> the identifier type
 */
public interface IdentifierFactory<I> extends Restartable, Supplier<I> {
}
