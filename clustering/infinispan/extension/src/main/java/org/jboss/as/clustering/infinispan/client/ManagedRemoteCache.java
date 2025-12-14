/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.client;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
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
    private final RemoteCacheContainer container;
    private final RemoteCacheManager manager;

    public ManagedRemoteCache(RemoteCacheContainer container, RemoteCacheManager manager, RemoteCache<K, V> cache, Registrar<String> registrar) {
        this(container, manager, (InternalRemoteCache<K, V>) cache, registrar, new AtomicReference<>());
    }

    private ManagedRemoteCache(RemoteCacheContainer container, RemoteCacheManager manager, InternalRemoteCache<K, V> cache, Registrar<String> registrar, AtomicReference<Registration> registration) {
        super(cache, decorated -> new ManagedRemoteCache<>(container, manager, decorated, registrar, registration));
        this.container = container;
        this.manager = manager;
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

    @Override
    public RemoteCacheContainer getRemoteCacheContainer() {
        return this.container;
    }

    @Deprecated
    @Override
    public RemoteCacheManager getRemoteCacheManager() {
        return this.manager;
    }
}
