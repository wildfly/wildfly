/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
