/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

/**
 * A cached stateful session bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBean<K, V extends StatefulSessionBeanInstance<K>> extends AutoCloseable {
    /**
     * Returns the bean identifier.
     * @return the bean identifier.
     */
    default K getId() {
        return this.getInstance().getId();
    }

    /**
     * Returns the bean instance.
     * @return a bean instance.
     */
    V getInstance();

    /**
     * Indicates whether or not this bean was closed, i.e. {@link #close()} was invoked.
     * @return true, if this bean is valid, false otherwise.
     */
    boolean isClosed();

    /**
     * Indicates whether or not this bean was discarded, i.e. {@link #discard()} was invoked.
     * @return true, if this bean was discarded, false otherwise.
     */
    boolean isDiscarded();

    /**
     * Indicates whether or not this bean was removed, i.e. {@link #remove()} was invoked.
     * @return true, if this bean was removed, false otherwise.
     */
    boolean isRemoved();

    /**
     * Removes this bean from the cache without triggering any events.
     * A discarded bean does not need to be closed.
     */
    void discard();

    /**
     * Removes this bean from the cache, triggering requisite {@link jakarta.annotation.PreDestroy} events.
     * A removed bean does not need to be closed.
     */
    void remove();

    /**
     * Closes any resources associated with this bean.
     * If bean has an associated timeout, it will schedule its expiration.
     */
    @Override
    void close();
}
