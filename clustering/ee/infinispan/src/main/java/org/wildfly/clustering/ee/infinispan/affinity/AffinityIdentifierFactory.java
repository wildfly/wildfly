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
package org.wildfly.clustering.ee.infinispan.affinity;

import java.util.function.Supplier;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.cache.IdentifierFactory;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.infinispan.spi.affinity.KeyAffinityServiceFactory;

/**
 * {@link IdentifierFactory} that uses a {@link KeyAffinityService} to generate identifiers.
 * @author Paul Ferraro
 * @param <I> the identifier type
 */
public class AffinityIdentifierFactory<I> implements IdentifierFactory<I>, KeyGenerator<Key<I>> {

    private final Supplier<I> factory;
    private final KeyAffinityService<? extends Key<I>> affinity;
    private final Address localAddress;

    public AffinityIdentifierFactory(Supplier<I> factory, Cache<Key<I>, ?> cache, KeyAffinityServiceFactory affinityFactory) {
        this.factory = factory;
        this.affinity = affinityFactory.createService(cache, this);
        this.localAddress = cache.getCacheManager().getAddress();
    }

    @Override
    public I get() {
        return this.affinity.getKeyForAddress(this.localAddress).getId();
    }

    @Override
    public Key<I> getKey() {
        return new GroupedKey<>(this.factory.get());
    }

    @Override
    public void start() {
        this.affinity.start();
    }

    @Override
    public void stop() {
        this.affinity.stop();
    }
}
