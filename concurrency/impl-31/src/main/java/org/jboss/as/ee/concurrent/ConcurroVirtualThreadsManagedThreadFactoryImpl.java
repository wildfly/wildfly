/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.concurro.ContextServiceImpl;
import org.glassfish.concurro.spi.ContextHandle;

/**
 * WildFly's extension of {@link org.glassfish.concurro.virtualthreads.VirtualThreadsManagedThreadFactory}.
 * @author Eduardo Martins
 */
public class ConcurroVirtualThreadsManagedThreadFactoryImpl extends org.glassfish.concurro.virtualthreads.VirtualThreadsManagedThreadFactory implements WildFlyManagedThreadFactory {

    public ConcurroVirtualThreadsManagedThreadFactoryImpl(String name, WildFlyContextService contextService) {
        super(name, (ContextServiceImpl) contextService);
    }

    @Override
    public int getPriority() {
        return Thread.NORM_PRIORITY;
    }

    @Override
    protected Thread createThread(Runnable r, final ContextHandle contextHandleForSetup) {
        if (contextHandleForSetup != null) {
            // app thread, do identity wrap
            r = SecurityIdentityUtils.doIdentityWrap(r);
        }
        final Thread t = super.createThread(r, contextHandleForSetup);
        // reset thread classloader to prevent leaks
        t.setContextClassLoader(null);
        return t;
    }
}
