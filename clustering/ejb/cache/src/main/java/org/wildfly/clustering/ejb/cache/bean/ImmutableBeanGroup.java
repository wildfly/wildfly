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

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.ejb.bean.BeanInstance;

/**
 * Exposes the context of, and manages the lifecycle for, groups of beans.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface ImmutableBeanGroup<K, V extends BeanInstance<K>> extends AutoCloseable {

    /**
     * Returns the unique identifier of this bean group.
     * @return a unique identifier
     */
    K getId();

    /**
     * Indicates whether or not this bean group contains and bean instances.
     * @return true, if this bean group is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns the bean instance with the specified identifier.
     * @param id a bean instance identifier
     * @return the requested bean instance, or null, if no such bean instance exists.
     */
    V getBeanInstance(K id);

    /**
     * Indicates that the caller is finished with the bean group, and that it should close any resources.
     */
    @Override
    void close();
}
