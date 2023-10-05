/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.hotrod.sso;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.cache.sso.AuthenticationEntry;
import org.wildfly.clustering.web.cache.sso.CompositeSSO;
import org.wildfly.clustering.web.cache.sso.SSOFactory;
import org.wildfly.clustering.web.cache.sso.SessionsFactory;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * @author Paul Ferraro
 */
public class HotRodSSOFactory<AV, SV, A, D, S, L> implements SSOFactory<Map.Entry<A, AtomicReference<L>>, SV, A, D, S, L> {

    private final SessionsFactory<SV, D, S> sessionsFactory;
    private final RemoteCache<AuthenticationKey, AuthenticationEntry<AV, L>> cache;
    private final Flag[] ignoreReturnFlags;
    private final Marshaller<A, AV> marshaller;
    private final Supplier<L> localContextFactory;

    public HotRodSSOFactory(HotRodConfiguration configuration, Marshaller<A, AV> marshaller, Supplier<L> localContextFactory, SessionsFactory<SV, D, S> sessionsFactory) {
        this.cache = configuration.getCache();
        this.ignoreReturnFlags = configuration.getIgnoreReturnFlags();
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
        this.sessionsFactory = sessionsFactory;
    }

    @Override
    public SSO<A, D, S, L> createSSO(String id, Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> value) {
        Map.Entry<A, AtomicReference<L>> authenticationEntry = value.getKey();
        Sessions<D, S> sessions = this.sessionsFactory.createSessions(id, value.getValue());
        return new CompositeSSO<>(id, authenticationEntry.getKey(), sessions, authenticationEntry.getValue(), this.localContextFactory, this);
    }

    @Override
    public Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> createValue(String id, A authentication) {
        try {
            AuthenticationEntry<AV, L> entry = new AuthenticationEntry<>(this.marshaller.write(authentication));
            this.cache.withFlags(this.ignoreReturnFlags).put(new AuthenticationKey(id), entry);
            SV sessions = this.sessionsFactory.createValue(id, null);
            return new AbstractMap.SimpleImmutableEntry<>(new AbstractMap.SimpleImmutableEntry<>(authentication, entry.getLocalContext()), sessions);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> findValue(String id) {
        AuthenticationEntry<AV, L> entry = this.cache.get(new AuthenticationKey(id));
        if (entry != null) {
            SV sessions = this.sessionsFactory.findValue(id);
            if (sessions != null) {
                try {
                    A authentication = this.marshaller.read(entry.getAuthentication());
                    return new AbstractMap.SimpleImmutableEntry<>(new AbstractMap.SimpleImmutableEntry<>(authentication, entry.getLocalContext()), sessions);
                } catch (IOException e) {
                    Logger.ROOT_LOGGER.failedToActivateAuthentication(e, id);
                    this.remove(id);
                }
            }
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        this.cache.withFlags(this.ignoreReturnFlags).remove(new AuthenticationKey(id));
        this.sessionsFactory.remove(id);
        return true;
    }

    @Override
    public SessionsFactory<SV, D, S> getSessionsFactory() {
        return this.sessionsFactory;
    }
}
