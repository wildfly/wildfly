/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ee.infinispan;

/**
 * Exposes a cache configuration as simple high-level properties.
 * @author Paul Ferraro
 */
public interface CacheProperties {
    /**
     * Indicates whether the associated cache requires eager locking for cache reads.
     * @return true, if the cache client should perform reads under lock, false otherwise
     */
    boolean isLockOnRead();

    /**
     * Indicates whether the associated cache uses eager locking for cache writes.
     * @return true, if the cache client should perform writes under lock, false otherwise
     */
    boolean isLockOnWrite();

    /**
     * Indicates whether the mode of this cache requires marshalling of cache values
     * @return true, if cache values need to be marshallable, false otherwise.
     */
    boolean isMarshalling();

    /**
     * Indicates whether cache operations should assume immediate marshalling/unmarshalling of the value.
     * @return true, if the cache client will need to handle passivation/activation notifications on read/write, false otherwise
     */
    boolean isPersistent();

    /**
     * Indicates whether the cache is transactional.
     * @return true, if this cache is transactional, false otherwise.
     */
    boolean isTransactional();
}
