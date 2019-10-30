/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.pool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The base of all pool implementations.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public abstract class AbstractPool<T> implements Pool<T> {

    private final StatelessObjectFactory<T> factory;
    private final AtomicInteger createCount = new AtomicInteger(0);
    private final AtomicInteger removeCount = new AtomicInteger(0);

    protected AbstractPool(StatelessObjectFactory<T> factory) {
        assert factory != null : "factory is null";

        this.factory = factory;
    }

    public int getCreateCount() {
        return createCount.get();
    }

    public int getRemoveCount() {
        return removeCount.get();
    }

    public abstract void setMaxSize(int maxSize);

    protected T create() {
        T bean = factory.create();

        createCount.incrementAndGet();

        return bean;
    }

    @Deprecated
    protected void remove(T bean) {
        this.doRemove(bean);
    }

    protected void destroy(T bean) {
        doRemove(bean);
    }

    /**
     * Remove the bean context and invoke any callbacks
     * and track the remove count
     *
     * @param bean
     */
    protected void doRemove(T bean) {
        try {
            factory.destroy(bean);
        } finally {
            removeCount.incrementAndGet();
        }
    }
}
