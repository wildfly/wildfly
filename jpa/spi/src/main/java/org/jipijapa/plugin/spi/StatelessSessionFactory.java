/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.plugin.spi;

import java.util.function.Function;

import jakarta.persistence.EntityManagerFactory;

/**
 * Provides access to a Hibernate 'StatelessSession' to callers who have no compile-time access to that class.
 *
 * @param <T> the concrete type of the StatelessSession
 */
public interface StatelessSessionFactory<T extends AutoCloseable> {

    /**
     * Gets a 'StatelessSession' that delegates to different underlying sessions depending
     * on the active transaction (or the lack of one)
     *
     * @param supplierFactory factory to produce the supplier that determines and creates the underlying session to use for a given method call
     */
    T getTransactionScopedSession(Function<Function<EntityManagerFactory, AutoCloseable>, ScopedStatelessSessionSupplier> supplierFactory);
}
