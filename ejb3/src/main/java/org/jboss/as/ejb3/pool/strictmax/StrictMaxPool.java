/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.pool.strictmax;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import org.jboss.as.ejb3.pool.AbstractPool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;

import javax.ejb.EJBException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A pool with a maximum size.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class StrictMaxPool<T> extends AbstractPool<T> {

    /**
     * A FIFO semaphore that is set when the strict max size behavior is in effect.
     * When set, only maxSize instances may be active and any attempt to get an
     * instance will block until an instance is freed.
     */
    private final Semaphore semaphore;
    /**
     * The maximum number of instances allowed in the pool
     */
    private final int maxSize;
    /**
     * The time to wait for the semaphore.
     */
    private final long timeout;
    private final TimeUnit timeUnit;
    /**
     * The pool data structure
     * Guarded by the implicit lock for "pool"
     */
    private final LinkedList<T> pool = new LinkedList<T>();

    private int inUse = 0;

    public StrictMaxPool(StatelessObjectFactory<T> factory, int maxSize, long timeout, TimeUnit timeUnit) {
        super(factory);
        this.maxSize = maxSize;
        this.semaphore = new Semaphore(maxSize, true);
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public void discard(T ctx) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Discard instance %s#%s", this, ctx);
        }

        // If we block when maxSize instances are in use, invoke release on strictMaxSize
        semaphore.release();
        --inUse;

        // Let the super do any other remove stuff
        super.doRemove(ctx);
    }

    public int getCurrentSize() {
        return getCreateCount() - getRemoveCount();
    }

    public int getAvailableCount() {
        return maxSize - inUse;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        throw MESSAGES.methodNotImplemented();
    }

    /**
     * Get an instance without identity.
     * Can be used by finders,create-methods, and activation
     *
     * @return Context /w instance
     */
    public T get() {
        try {
            boolean acquired = semaphore.tryAcquire(timeout, timeUnit);
            if (!acquired)
                throw MESSAGES.failedToAcquirePermit(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw MESSAGES.acquireSemaphoreInterrupted();
        }

        synchronized (pool) {
            if (!pool.isEmpty()) {
                return pool.removeFirst();
            }
        }

        T bean = null;
        try {
            // Pool is empty, create an instance
            ++inUse;
            bean = create();
        } finally {
            if (bean == null) {
                --inUse;
                semaphore.release();
            }
        }
        return bean;
    }

    /**
     * Return an instance after invocation.
     * <p/>
     * Called in 2 cases:
     * a) Done with finder method
     * b) Just removed
     *
     * @param obj
     */
    public void release(T obj) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("%s/%s Free instance: %s", pool.size(), maxSize, this);
        }

        boolean destroyIt = false;
        synchronized (pool) {
            // Add the unused context back into the pool
            if (pool.size() < maxSize)
                pool.add(obj);
            else
                destroyIt = true;
        }
        if (destroyIt)
            destroy(obj);
        // If we block when maxSize instances are in use, invoke release on strictMaxSize
        semaphore.release();
        --inUse;
    }

    @Override
    @Deprecated
    public void remove(T ctx) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Removing instance: %s#%s", this, ctx);
        }

        semaphore.release();
        --inUse;
        // let the super do the other remove stuff
        super.doRemove(ctx);
    }

    public void start() {
        // TODO Auto-generated method stub

    }

    public void stop() {
        synchronized (pool) {
            for (T obj : pool) {
                destroy(obj);
            }
            pool.clear();
        }
    }
}
