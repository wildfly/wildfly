/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.remote;

import org.jboss.ejb.server.ListenerHandle;

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

    public DelegatingListenerHandle(ListenerHandle handle) {
        delegate = handle;
    }

    public ListenerHandle getDelegate() {
        return delegate;
    }

    public void setDelegate(ListenerHandle delegate) {
        this.delegate = delegate;
    }

    private ListenerHandle delegate ;

    @Override
    public void close() {
        getDelegate().close();
    }
}
