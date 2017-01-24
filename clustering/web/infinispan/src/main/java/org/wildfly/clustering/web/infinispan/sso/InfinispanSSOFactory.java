/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.infinispan.sso;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.CacheProperties;
import org.wildfly.clustering.marshalling.spi.InvalidSerializedFormException;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.logging.InfinispanWebLogger;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

/**
 * @author Paul Ferraro
 */
public class InfinispanSSOFactory<AV, SV, A, D, S, L> implements SSOFactory<Map.Entry<A, AtomicReference<L>>, SV, A, D, S, L> {

    private final SessionsFactory<SV, D, S> sessionsFactory;
    private final Cache<AuthenticationKey, AuthenticationEntry<AV, L>> cache;
    private final Cache<AuthenticationKey, AuthenticationEntry<AV, L>> findCache;
    private final Marshaller<A, AV> marshaller;
    private final LocalContextFactory<L> localContextFactory;

    public InfinispanSSOFactory(Cache<AuthenticationKey, AuthenticationEntry<AV, L>> cache, CacheProperties properties, Marshaller<A, AV> marshaller, LocalContextFactory<L> localContextFactory, SessionsFactory<SV, D, S> sessionsFactory) {
        this.cache = cache;
        this.findCache = properties.isLockOnRead() ? cache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK) : cache;
        this.marshaller = marshaller;
        this.localContextFactory = localContextFactory;
        this.sessionsFactory = sessionsFactory;
    }

    @Override
    public SSO<A, D, S, L> createSSO(String id, Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> value) {
        Map.Entry<A, AtomicReference<L>> authenticationEntry = value.getKey();
        Sessions<D, S> sessions = this.sessionsFactory.createSessions(id, value.getValue());
        return new InfinispanSSO<>(id, authenticationEntry.getKey(), sessions, authenticationEntry.getValue(), this.localContextFactory, this);
    }

    @Override
    public Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> createValue(String id, A authentication) {
        AuthenticationEntry<AV, L> entry = new AuthenticationEntry<>(this.marshaller.write(authentication));
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).put(new AuthenticationKey(id), entry);
        SV sessions = this.sessionsFactory.createValue(id, null);
        return new AbstractMap.SimpleImmutableEntry<>(new AbstractMap.SimpleImmutableEntry<>(authentication, entry.getLocalContext()), sessions);
    }

    @Override
    public Map.Entry<Map.Entry<A, AtomicReference<L>>, SV> findValue(String id) {
        AuthenticationEntry<AV, L> entry = this.findCache.get(new AuthenticationKey(id));
        if (entry != null) {
            SV sessions = this.sessionsFactory.findValue(id);
            if (sessions != null) {
                try {
                    A authentication = this.marshaller.read(entry.getAuthentication());
                    return new AbstractMap.SimpleImmutableEntry<>(new AbstractMap.SimpleImmutableEntry<>(authentication, entry.getLocalContext()), sessions);
                } catch (InvalidSerializedFormException e) {
                    InfinispanWebLogger.ROOT_LOGGER.failedToActivateAuthentication(e, id);
                    this.remove(id);
                }
            }
        }
        return null;
    }

    @Override
    public boolean remove(String id) {
        this.cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES).remove(new AuthenticationKey(id));
        this.sessionsFactory.remove(id);
        return true;
    }

    @Override
    public SessionsFactory<SV, D, S> getSessionsFactory() {
        return this.sessionsFactory;
    }
}
