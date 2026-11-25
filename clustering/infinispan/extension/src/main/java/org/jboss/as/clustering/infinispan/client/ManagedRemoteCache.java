/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.client;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.wildfly.clustering.cache.infinispan.remote.RemoteCacheDecorator;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.clustering.server.Registration;

/**
 * {@link RemoteCache} decorator that handles registration on {@link #start()} and deregistration on {@link #stop()}.
 * N.B. Implements {@link InternalRemoteCache} to support casting, as required by {@link org.infinispan.client.hotrod.Search#getQueryFactory(RemoteCache)}.
 * @author Paul Ferraro
 */
public class ManagedRemoteCache<K, V> extends RemoteCacheDecorator<K, V> implements UnaryOperator<Registration> {

    private final Registrar<String> registrar;
    private final AtomicReference<Registration> registration;

    public ManagedRemoteCache(RemoteCacheContainer container, RemoteCache<K, V> cache, Registrar<String> registrar) {
        this(container, (InternalRemoteCache<K, V>) cache, registrar, new AtomicReference<>());
    }

    private ManagedRemoteCache(RemoteCacheContainer container, InternalRemoteCache<K, V> cache, Registrar<String> registrar, AtomicReference<Registration> registration) {
        super(container, cache, decorated -> new ManagedRemoteCache<>(container, decorated, registrar, registration));
        this.registrar = registrar;
        this.registration = registration;
    }

    @Override
    public void start() {
        if (this.registration.getAndUpdate(this) == null) {
            super.start();
        }
    }

    @Override
    public Registration apply(Registration registration) {
        return (registration == null) ? this.registrar.register(this.getName()) : registration;
    }

    @Override
    public void stop() {
        try (Registration registration = this.registration.getAndSet(null)) {
            if (registration != null) {
                super.stop();
            }
        }
    }
}
