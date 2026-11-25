/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import org.jboss.ejb.server.ListenerHandle;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A ListenerHandle implementation which accepts a delegate ListenerHandle
 * to permit changing the ListenerHandle delegate without affecting the
 * ListenerHandle object instance.
 *
 * This feature is required when updating an Association instance and the
 * ListenerHandles for ClusterTopologListeners and ModuleAvailabilityListeners
 * it has previously registered.
 *
 * @author Richard Achmatowicz
 */
public class DelegatingListenerHandle implements ListenerHandle {

    private final AtomicReference<ListenerHandle> delegate ;

    public DelegatingListenerHandle(ListenerHandle handle) {
        delegate = new AtomicReference<>(handle);
    }

    public ListenerHandle getDelegate() {
        return delegate.get();
    }

    // atomically swap old -> new; returns false if already closed
    public boolean compareAndSetDelegate(ListenerHandle expected, ListenerHandle update) {
        return this.delegate.compareAndSet(expected, update);
    }

    boolean isHandleClosedByChannel() {
        return delegate.get() == null;
    }

    /**
     * Close can be called concurrently either by:
     * (1) a channel to close its handle or
     * (2) the refresh handler when implementations are swapped.
     */
    @Override
    public void close() {
        ListenerHandle handle = this.delegate.getAndSet(null);
        if (handle != null)
            handle.close();
    }
}
