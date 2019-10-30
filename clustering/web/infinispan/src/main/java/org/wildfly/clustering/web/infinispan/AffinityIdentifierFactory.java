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
package org.wildfly.clustering.web.infinispan;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.manager.EmbeddedCacheManager;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.infinispan.spi.distribution.Key;
import org.wildfly.clustering.web.IdentifierFactory;

/**
 * {@link IdentifierFactory} that uses a {@link KeyAffinityService} to generate identifiers.
 * @author Paul Ferraro
 * @param <K> the key type
 */
public class AffinityIdentifierFactory<K> implements IdentifierFactory<K>, KeyGenerator<Key<K>> {

    private final IdentifierFactory<K> factory;
    private final KeyAffinityService<? extends Key<K>> affinity;
    private final EmbeddedCacheManager manager;

    public AffinityIdentifierFactory(IdentifierFactory<K> factory, Cache<Key<K>, ?> cache, KeyAffinityServiceFactory affinityFactory) {
        this.factory = factory;
        this.affinity = affinityFactory.createService(cache, this);
        this.manager = cache.getCacheManager();
    }

    @Override
    public K createIdentifier() {
        return this.affinity.getKeyForAddress(this.manager.getAddress()).getValue();
    }

    @Override
    public Key<K> getKey() {
        return new Key<>(this.factory.createIdentifier());
    }

    @Override
    public void start() {
        this.factory.start();
        this.affinity.start();
    }

    @Override
    public void stop() {
        this.affinity.stop();
        this.factory.stop();
    }
}
