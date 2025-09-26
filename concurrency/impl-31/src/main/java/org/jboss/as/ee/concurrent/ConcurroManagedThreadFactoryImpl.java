/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.concurrent;

import org.glassfish.concurro.AbstractManagedThread;
import org.glassfish.concurro.ContextServiceImpl;
import org.glassfish.concurro.internal.ManagedFutureTask;
import org.glassfish.concurro.spi.ContextHandle;
import org.wildfly.security.auth.server.SecurityIdentity;

import jakarta.enterprise.concurrent.ManagedThreadFactory;

/**
 * {@link ManagedThreadFactory} implementation ensuring {@link SecurityIdentity} propagation into new threads.
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 * @author emmartins
 */
public class ConcurroManagedThreadFactoryImpl extends org.glassfish.concurro.ManagedThreadFactoryImpl implements WildFlyManagedThreadFactory {

    /**
     * the priority set on new threads
     */
    private final int priority;

    public ConcurroManagedThreadFactoryImpl(String name, WildFlyContextService contextService, int priority) {
        super(name, (ContextServiceImpl) contextService, priority);
        this.priority = priority;
    }

    /**
     *
     * @return the priority set on new threads
     */
    public int getPriority() {
        return priority;
    }

    protected AbstractManagedThread createThread(Runnable r, final ContextHandle contextHandleForSetup) {
        if (contextHandleForSetup != null) {
            // app thread, do identity wrap
            r = SecurityIdentityUtils.doIdentityWrap(r);
        }
        final ManagedThread t = new ManagedThread(r, contextHandleForSetup);
        t.setPriority(priority);
        // reset thread classloader to prevent leaks
        t.setContextClassLoader(null);
        return t;
    }

    @Override
    public void taskStarting(Thread t, ManagedFutureTask task) {
        super.taskStarting(t, task);
        if (t instanceof ManagedThread) {
            ((ManagedThread)t).task = task;
        }
    }

    @Override
    public void taskDone(Thread t) {
        super.taskDone(t);
        if (t instanceof ManagedThread) {
            ((ManagedThread)t).task = null;
        }
    }

    /**
     * Managed thread extension, to allow canceling the task running in the thread.
     * @author emmartins
     */
    public class ManagedThread extends org.glassfish.concurro.ManagedThreadFactoryImpl.ManagedThread {
        volatile ManagedFutureTask task = null;
        /**
         *
         * @param target
         * @param contextHandleForSetup
         */
        public ManagedThread(Runnable target, ContextHandle contextHandleForSetup) {
            super(target, contextHandleForSetup);
        }
        /**
         * Cancel the task running in the thread.
         * @return
         */
        boolean cancelTask() {
            final ManagedFutureTask task = this.task;
            if (task != null) {
                return task.cancel(true);
            }
            return false;
        }
    }
}
