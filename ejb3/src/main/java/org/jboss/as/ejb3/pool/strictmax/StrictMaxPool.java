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

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.pool.AbstractPool;
import org.jboss.as.ejb3.pool.StatelessObjectFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final Queue<T> pool = new ConcurrentLinkedQueue<T>();

    public StrictMaxPool(StatelessObjectFactory<T> factory, int maxSize, long timeout, TimeUnit timeUnit) {
        super(factory);
        this.maxSize = maxSize;
        this.semaphore = new Semaphore(maxSize, false);
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    public void discard(T ctx) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Discard instance %s#%s", this, ctx);
        }

        // If we block when maxSize instances are in use, invoke release on strictMaxSize
        semaphore.release();

        // Let the super do any other remove stuff
        super.doRemove(ctx);
    }

    public int getCurrentSize() {
        return getCreateCount() - getRemoveCount();
    }

    public int getAvailableCount() {
        return semaphore.availablePermits();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        throw EjbLogger.ROOT_LOGGER.methodNotImplemented();
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
                throw EjbLogger.ROOT_LOGGER.failedToAcquirePermit(timeout, timeUnit);
        } catch (InterruptedException e) {
            throw EjbLogger.ROOT_LOGGER.acquireSemaphoreInterrupted();
        }

        T bean = pool.poll();

        if( bean !=null) {
            //we found a bean instance in the pool, return it
            return bean;
        }

        try {
            // Pool is empty, create an instance
            bean = create();
        } finally {
            if (bean == null) {
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

        pool.add(obj);

        semaphore.release();
    }

    @Override
    @Deprecated
    public void remove(T ctx) {
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.tracef("Removing instance: %s#%s", this, ctx);
        }

        semaphore.release();
        // let the super do the other remove stuff
        super.doRemove(ctx);
    }

    public void start() {
        // TODO Auto-generated method stub

    }

    public void stop() {
        for (T obj = pool.poll(); obj != null; obj = pool.poll()) {
            destroy(obj);
        }
    }
}
