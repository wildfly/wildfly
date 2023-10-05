/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso.coarse;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.ee.hotrod.RemoteCacheEntryMutator;
import org.wildfly.clustering.web.cache.sso.SessionsFactory;
import org.wildfly.clustering.web.cache.sso.coarse.CoarseSessions;
import org.wildfly.clustering.web.cache.sso.coarse.SessionFilter;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * @author Paul Ferraro
 */
public class CoarseSessionsFactory<D, S> implements SessionsFactory<Map<D, S>, D, S> {

    private final SessionsFilter<D, S> filter = new SessionsFilter<>();
    private final RemoteCache<CoarseSessionsKey, Map<D, S>> cache;
    private final Flag[] ignoreReturnFlags;

    public CoarseSessionsFactory(HotRodConfiguration configuration) {
        this.cache = configuration.getCache();
        this.ignoreReturnFlags = configuration.getIgnoreReturnFlags();
    }

    @Override
    public Sessions<D, S> createSessions(String ssoId, Map<D, S> value) {
        CoarseSessionsKey key = new CoarseSessionsKey(ssoId);
        Mutator mutator = new RemoteCacheEntryMutator<>(this.cache, this.ignoreReturnFlags, key, value);
        return new CoarseSessions<>(value, mutator);
    }

    @Override
    public Map<D, S> createValue(String id, Void context) {
        Map<D, S> sessions = new ConcurrentHashMap<>();
        this.cache.put(new CoarseSessionsKey(id), sessions);
        return sessions;
    }

    @Override
    public Map<D, S> findValue(String id) {
        return this.cache.get(new CoarseSessionsKey(id));
    }

    @Override
    public Map.Entry<String, Map<D, S>> findEntryContaining(S session) {
        SessionFilter<CoarseSessionsKey, D, S> filter = new SessionFilter<>(session);
        // Erase type to handle compilation issues with generics
        // Our filter will handle type safety and casting
        @SuppressWarnings("rawtypes")
        RemoteCache cache = this.cache;
        try (Stream<Map.Entry<?, ?>> stream = cache.entrySet().stream()) {
            Map.Entry<CoarseSessionsKey, Map<D, S>> entry = stream.filter(this.filter).map(this.filter).filter(filter).findAny().orElse(null);
            return (entry != null) ? new AbstractMap.SimpleImmutableEntry<>(entry.getKey().getId(), entry.getValue()) : null;
        }
    }

    @Override
    public boolean remove(String id) {
        this.cache.withFlags(this.ignoreReturnFlags).remove(new CoarseSessionsKey(id));
        return true;
    }
}
