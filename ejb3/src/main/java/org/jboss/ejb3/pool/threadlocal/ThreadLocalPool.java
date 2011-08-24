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
package org.jboss.ejb3.pool.threadlocal;

import org.jboss.ejb3.pool.AbstractPool;
import org.jboss.ejb3.pool.Pool;
import org.jboss.ejb3.pool.StatelessObjectFactory;
import org.jboss.ejb3.pool.infinite.InfinitePool;
import org.jboss.logging.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pool which keeps an object ready per thread.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @version $Revision: $
 */
public class ThreadLocalPool<T> extends AbstractPool<T> implements Pool<T> {
    private static final Logger log = Logger.getLogger(ThreadLocalPool.class);

    protected final Pool<T> delegate;
    protected WeakThreadLocal<T> pool = new WeakThreadLocal<T>();
    private AtomicInteger inUse = new AtomicInteger();

    public ThreadLocalPool(StatelessObjectFactory<T> factory) {
        super(factory);
        this.delegate = new InfinitePool<T>(factory);
    }

    @Override
    protected T create() {
        return delegate.get();
    }

    public void discard(T obj) {
        delegate.discard(obj);
        inUse.decrementAndGet();
    }

    @Override
    public T get() {
        T obj = pool.get();
        if (obj != null)
            pool.set(null);
        else
            obj = delegate.get();

        inUse.incrementAndGet();

        return obj;
    }

    @Override
    public void release(T obj) {
        if (pool.get() == null) {
            pool.set(obj);
        } else {
            delegate.release(obj);
        }

        inUse.decrementAndGet();
    }

    @Override
    public int getCurrentSize() {
        int size;
        synchronized (delegate) {
            size = delegate.getCreateCount() - delegate.getRemoveCount();
        }
        return size;
    }

    @Override
    public int getAvailableCount() {
        return getMaxSize() - inUse.get();
    }

    @Override
    public int getCreateCount() {
        return delegate.getCreateCount();
    }

    @Override
    public int getMaxSize() {
        // the thread local pool dynamically grows for new threads
        // if a bean is reentrant it'll grow and shrink over the reentrant call
        return getCurrentSize();
    }

    @Override
    public int getRemoveCount() {
        return delegate.getRemoveCount();
    }

    @Override
    public void setMaxSize(int maxSize) {
        //this.maxSize = maxSize;
        log.warn("EJBTHREE-1703: setting a max size on ThreadlocalPool is bogus");
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        log.trace("destroying pool");

        delegate.stop();

        // This really serves little purpose, because we want the whole thread local map to die
        pool.remove();

        inUse.getAndSet(0);
    }
}
