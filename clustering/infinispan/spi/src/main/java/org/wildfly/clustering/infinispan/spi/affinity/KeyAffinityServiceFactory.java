/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.affinity;

import java.util.function.Predicate;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;

/**
 * Factory for creating a key affinity service.
 * @author Paul Ferraro
 */
public interface KeyAffinityServiceFactory {
    /**
     * Creates a key affinity service for use with the specified cache, that generates local key using the specified generator.
     * @param cache
     * @param generator
     * @return a key affinity service
     */
    @SuppressWarnings("resource")
    default <K> KeyAffinityService<K> createService(Cache<? extends K, ?> cache, KeyGenerator<K> generator) {
        return this.createService(cache, generator, cache.getCacheManager().getAddress()::equals);
    }

    /**
     * Creates a key affinity service for use with the specified cache, that generates key for members matching the specified filter, using the specified generator.
     * @param cache
     * @param generator
     * @return a key affinity service
     */
    <K> KeyAffinityService<K> createService(Cache<? extends K, ?> cache, KeyGenerator<K> generator, Predicate<Address> filter);
}
