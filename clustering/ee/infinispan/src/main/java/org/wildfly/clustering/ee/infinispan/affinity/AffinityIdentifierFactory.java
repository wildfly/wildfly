/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;

/**
 * An {@link IdentifierFactory} that uses a {@link KeyAffinityService} to pre-generate locally hashing identifiers from a supplier.
 * @author Paul Ferraro
 * @param <I> the identifier type
 */
public class AffinityIdentifierFactory<I> implements IdentifierFactory<I>, KeyGenerator<Key<I>> {

    private final Supplier<I> factory;
    private final KeyAffinityService<? extends Key<I>> affinity;
    private final Address localAddress;

    public AffinityIdentifierFactory(Supplier<I> factory, Cache<? extends Key<I>, ?> cache, KeyAffinityServiceFactory affinityFactory) {
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
