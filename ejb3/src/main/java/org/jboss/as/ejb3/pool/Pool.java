/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.pool;

/**
 * A pool of stateless objects.
 * <p/>
 * A pool is linked to an object factory. How this link is established
 * is left beyond scope.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface Pool<T> {
    /**
     * Discard an object. This will be called
     * in case of a system exception.
     *
     * @param obj the object
     */
    void discard(T obj);

    /**
     * Get an object from the pool. This will mark
     * the object as being in use.
     *
     * @return the object
     */
    T get();

    int getAvailableCount();

    int getCreateCount();

    int getCurrentSize();

    int getMaxSize();

    int getRemoveCount();

    /**
     * Release the object from use.
     *
     * @param obj the object
     */
    void release(T obj);

    void setMaxSize(int maxSize);

    /**
     * Start the pool.
     */
    void start();

    /**
     * Stop the pool.
     */
    void stop();
}
