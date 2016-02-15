/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb;

/**
 * Represents a bean and the group to which it is associated.
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean instance type
 */
public interface Bean<I, T> extends AutoCloseable {
    /**
     * Returns the identifier of this bean.
     * @return a unique identifier
     */
    I getId();

    /**
     * Returns the identifier of the group to which this bean is associated.
     * @return a unique identifier of a group
     */
    I getGroupId();

    /**
     * Acquires a reference to the bean instance.
     * Every call to {@link #acquire()} should be paired with a call to {@link #release()}.
     * @return the bean instance
     */
    T acquire();

    /**
     * Releases a reference to the bean made via {@link #acquire()}.
     * @return true, if all acquisitions have been released, false otherwise.
     */
    boolean release();

    /**
     * Indicates whether or not the specified bean is expired.
     * @return true, if this bean has expired, false otherwise.
     */
    boolean isExpired();

    /**
     * Removes this bean, notifying the specified listener.
     * @param listener a remove listener
     */
    void remove(RemoveListener<T> listener);

    /**
     * Indicates whether this bean was removed.
     * @return false, if this bean was removed, true otherwise.
     */
    boolean isValid();

    /**
     * Closes any resources used by this bean.
     * A bean should only be closed when all referenced have been released,
     * i.e. {@link #release()} returned true.
     */
    @Override
    void close();
}
