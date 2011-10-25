/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.infinispan.invoker;

import org.infinispan.Cache;

/**
 * Encapsulates logic used to invoke an operation on a cache.
 *
 * @author Paul Ferraro
 */
public interface CacheInvoker {
    /**
     * Invokes the specified operation on the specified cache.
     *
     * @param <R> the type of the cache operation result
     * @param cache an infinispan cache
     * @param operation a cache operation
     * @return the result of the cache operation
     */
    <K, V, R> R invoke(Cache<K, V> cache, Operation<K, V, R> operation);

    /**
     * Encapsulates a cache operation.
     *
     * @param <R> the return type of the cache operation
     */
    interface Operation<K, V, R> {
        /**
         * Invoke some operation on the specified cache.
         *
         * @param cache an infinispan cache
         * @return the result of the cache operation
         */
        R invoke(Cache<K, V> cache);
    }
}
