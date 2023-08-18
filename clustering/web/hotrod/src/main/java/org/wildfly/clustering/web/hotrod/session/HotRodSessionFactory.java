/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.Registrar;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.hotrod.HotRodConfiguration;
import org.wildfly.clustering.infinispan.client.listener.ClientCacheEntryExpiredEventListener;
import org.wildfly.clustering.infinispan.client.listener.ListenerRegistration;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.ImmutableSessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.ImmutableSessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SimpleSessionCreationMetaData;
import org.wildfly.clustering.web.hotrod.logging.Logger;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Creates a {@link org.wildfly.clustering.web.session.Session} from a set of remote cache entries.
 * @param <C> the ServletContext specification type
 * @param <V> the session attribute value type
 * @param <L> the local context type
 * @author Paul Ferraro
 */
public class HotRodSessionFactory<C, V, L> extends CompositeSessionFactory<C, V, L> implements Registrar<Consumer<ImmutableSession>>, BiConsumer<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> {

    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final ImmutableSessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory;
    private final ImmutableSessionAttributesFactory<V> attributesFactory;
    private final Remover<String> attributesRemover;
    private final Collection<Consumer<ImmutableSession>> listeners = new CopyOnWriteArraySet<>();
    private final ListenerRegistration listenerRegistration;

    /**
     * Constructs a new session factory
     * @param config
     * @param metaDataFactory
     * @param attributesFactory
     * @param localContextFactory
     */
    public HotRodSessionFactory(HotRodConfiguration config, SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> metaDataFactory, SessionAttributesFactory<C, V> attributesFactory, LocalContextFactory<L> localContextFactory) {
        super(metaDataFactory, attributesFactory, localContextFactory);
        this.metaDataFactory = metaDataFactory;
        this.attributesFactory = attributesFactory;
        this.attributesRemover = attributesFactory;
        this.creationMetaDataCache = config.getCache();
        this.accessMetaDataCache= config.getCache();
        this.listenerRegistration = new ClientCacheEntryExpiredEventListener<>(this.creationMetaDataCache, this).register();
    }

    @Override
    public void close() {
        this.listenerRegistration.close();
    }

    @Override
    public void accept(SessionCreationMetaDataKey key, SessionCreationMetaDataEntry<L> entry) {
        String id = key.getId();
        SessionAccessMetaData accessMetaData = this.accessMetaDataCache.withFlags(Flag.FORCE_RETURN_VALUE).remove(new SessionAccessMetaDataKey(id));
        if (accessMetaData != null) {
            V attributesValue = this.attributesFactory.findValue(id);
            if (attributesValue != null) {
                SessionCreationMetaDataEntry<L> creationMetaDataEntry = (entry != null) ? entry : new SessionCreationMetaDataEntry<>(new SimpleSessionCreationMetaData(Instant.EPOCH));
                ImmutableSessionMetaData metaData = this.metaDataFactory.createImmutableSessionMetaData(id, new CompositeSessionMetaDataEntry<>(creationMetaDataEntry , accessMetaData));
                ImmutableSessionAttributes attributes = this.attributesFactory.createImmutableSessionAttributes(id, attributesValue);
                ImmutableSession session = HotRodSessionFactory.this.createImmutableSession(id, metaData, attributes);
                Logger.ROOT_LOGGER.tracef("Session %s has expired.", id);
                for (Consumer<ImmutableSession> listener : this.listeners) {
                    listener.accept(session);
                }
                this.attributesRemover.remove(id);
            }
        }
    }

    @Override
    public Registration register(Consumer<ImmutableSession> listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }
}
