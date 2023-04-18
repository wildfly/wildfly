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
package org.wildfly.clustering.ejb.bean;

import java.util.function.Consumer;

/**
 * Described the mutable and immutable properties of a cached bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface Bean<K, V extends BeanInstance<K>> extends ImmutableBean<K, V>, AutoCloseable {

    /**
     * Returns the metadata of this bean.
     * @return
     */
    @Override
    BeanMetaData<K> getMetaData();

    /**
     * Indicates whether or not this bean is valid, i.e. not closed nor removed.
     * @return true, if this bean is valid, false otherwise
     */
    boolean isValid();

    /**
     * Removes this bean from the cache, executing the specified task.
     */
    void remove(Consumer<V> removeTask);

    /**
     * Closes any resources used by this bean.
     */
    @Override
    void close();
}
