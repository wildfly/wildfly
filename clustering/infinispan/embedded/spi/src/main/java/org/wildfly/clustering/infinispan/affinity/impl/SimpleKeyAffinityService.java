/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.affinity.impl;

import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyGenerator;
import org.infinispan.remoting.transport.Address;

/**
 * Simple {@link KeyAffinityService} implementation for use when co-location is not a requirement.
 * @author Paul Ferraro
 */
public class SimpleKeyAffinityService<K> implements KeyAffinityService<K> {

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
