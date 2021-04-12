/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.affinity.impl.KeyAffinityServiceImpl;

/**
 * Factory for a {@link KeyAffinityService} whose implementation varies depending on cache mode.
 * @author Paul Ferraro
 */
public class DefaultKeyAffinityServiceFactory implements KeyAffinityServiceFactory {

    private final ExecutorService executor;
    private final int bufferSize;

    public DefaultKeyAffinityServiceFactory(ExecutorService executor, int bufferSize) {
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public <K> KeyAffinityService<K> createService(Cache<K, ?> cache, KeyGenerator<K> generator) {
        return cache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new KeyAffinityServiceImpl<>(this.executor, cache, generator, this.bufferSize, Collections.singleton(cache.getCacheManager().getAddress()), false) : new SimpleKeyAffinityService<>(generator);
    }
}
