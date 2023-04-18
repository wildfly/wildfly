/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
