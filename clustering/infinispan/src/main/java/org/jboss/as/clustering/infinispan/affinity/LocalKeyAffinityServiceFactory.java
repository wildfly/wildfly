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

package org.jboss.as.clustering.infinispan.affinity;

import java.util.Collections;
import java.util.concurrent.Executor;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceImpl;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;

/**
 * Key affinity service that only generates keys for use by the local node.
 * Returns a trivial implementation if the specified cache is not distributed.
 * @author Paul Ferraro
 */
public class LocalKeyAffinityServiceFactory implements KeyAffinityServiceFactory {
    private final Executor executor;
    private final int bufferSize;

    public LocalKeyAffinityServiceFactory(Executor executor, int bufferSize) {
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public <K> KeyAffinityService<K> createService(Cache<K, ?> cache, KeyGenerator<K> generator) {
        boolean distributed = cache.getCacheConfiguration().clustering().cacheMode().isDistributed();
        return distributed ? new KeyAffinityServiceImpl<K>(this.executor, cache, generator, this.bufferSize, Collections.singleton(cache.getCacheManager().getAddress()), false) : new SimpleKeyAffinityService<K>(generator);
    }

    private static class SimpleKeyAffinityService<K> implements KeyAffinityService<K> {
        private final KeyGenerator<K> generator;
        private volatile boolean started = false;

        SimpleKeyAffinityService(KeyGenerator<K> generator) {
            this.generator = generator;
        }

        @Override
        public void start() {
            this.started = true;
        }

        @Override
        public void stop() {
            this.started = false;
        }

        @Override
        public K getKeyForAddress(Address address) {
            return this.generator.getKey();
        }

        @Override
        public K getCollocatedKey(K otherKey) {
            return this.generator.getKey();
        }

        @Override
        public boolean isStarted() {
            return this.started;
        }
    }
}
