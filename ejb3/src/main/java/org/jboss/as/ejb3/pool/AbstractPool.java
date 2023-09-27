/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
