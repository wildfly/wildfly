/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent.service;

import java.util.function.Supplier;

/**
 * A supplier which delegates to other supplier if it is configured.
 * @param <T> the type of objects that may be supplied by this supplier.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DelegatingSupplier<T> implements Supplier<T> {

    private volatile Supplier<T> delegate;

    /**
     * Gets delegating supplier value or <code>null</code> if supplier is not configured.
     *
     * @return delegating supplier value
     */
    @Override
    public T get() {
        final Supplier<T> delegate = this.delegate;
        return delegate != null ? delegate.get() : null;
    }

    /**
     * Sets supplier to delegate to.
     *
     * @param delegate supplier to delegate to
     */
    public void set(final Supplier<T> delegate) {
        this.delegate = delegate;
    }

}
