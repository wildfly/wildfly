/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.infinispan.client.hotrod.RemoteCache;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ee.hotrod.RemoteCacheMutatorFactory;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaData;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.cache.session.MutableSessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.MutableSessionCreationMetaData;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionMetaDataFactory;
import org.wildfly.clustering.web.cache.session.SimpleSessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SimpleSessionCreationMetaData;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionMetaDataFactory<L> implements SessionMetaDataFactory<CompositeSessionMetaDataEntry<L>> {

    private final RemoteCache<Key<String>, Object> cache;
    private final RemoteCache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final MutatorFactory<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataMutatorFactory;
    private final RemoteCache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;
    private final MutatorFactory<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataMutatorFactory;
    private final CacheProperties properties;

    public HotRodSessionMetaDataFactory(HotRodSessionMetaDataFactoryConfiguration configuration) {
        this.cache = configuration.getCache();
        this.creationMetaDataCache = configuration.getCache();
        this.creationMetaDataMutatorFactory = new RemoteCacheMutatorFactory<>(this.creationMetaDataCache, new Function<SessionCreationMetaDataEntry<L>, Duration>() {
            @Override
            public Duration apply(SessionCreationMetaDataEntry<L> entry) {
                return entry.getMetaData().getMaxInactiveInterval();
            }
        });
        this.accessMetaDataCache = configuration.getCache();
        this.accessMetaDataMutatorFactory = new RemoteCacheMutatorFactory<>(this.accessMetaDataCache);
        this.properties = configuration.getCacheProperties();
    }

    @Override
    public CompositeSessionMetaDataEntry<L> createValue(String id, Void context) {
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = new SessionCreationMetaDataEntry<>(new SimpleSessionCreationMetaData());
        SessionAccessMetaData accessMetaData = new SimpleSessionAccessMetaData();
        this.creationMetaDataMutatorFactory.createMutator(new SessionCreationMetaDataKey(id), creationMetaDataEntry).mutate();
        this.accessMetaDataMutatorFactory.createMutator(new SessionAccessMetaDataKey(id), accessMetaData).mutate();
        return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
    }

    @Override
    public CompositeSessionMetaDataEntry<L> findValue(String id) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        Set<Key<String>> keys = new HashSet<>(3);
        keys.add(creationMetaDataKey);
        keys.add(accessMetaDataKey);
        // Use bulk read
        Map<Key<String>, Object> entries = this.cache.getAll(keys);
        @SuppressWarnings("unchecked")
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = (SessionCreationMetaDataEntry<L>) entries.get(creationMetaDataKey);
        SessionAccessMetaData accessMetaData = (SessionAccessMetaData) entries.get(accessMetaDataKey);
        if ((creationMetaDataEntry != null) && (accessMetaData != null)) {
            return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
        }
        if ((creationMetaDataEntry != null) || (accessMetaData != null)) {
            this.purge(id);
        }
        return null;
    }

    @Override
    public InvalidatableSessionMetaData createSessionMetaData(String id, CompositeSessionMetaDataEntry<L> entry) {
        boolean newSession = entry.getCreationMetaData().isNew();

        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        Mutator creationMutator = this.properties.isTransactional() && newSession ? Mutator.PASSIVE : this.creationMetaDataMutatorFactory.createMutator(creationMetaDataKey, new SessionCreationMetaDataEntry<>(entry.getCreationMetaData(), entry.getLocalContext()));
        SessionCreationMetaData creationMetaData = new MutableSessionCreationMetaData(entry.getCreationMetaData(), creationMutator);

        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        Mutator accessMutator = this.properties.isTransactional() && newSession ? Mutator.PASSIVE : this.accessMetaDataMutatorFactory.createMutator(accessMetaDataKey, entry.getAccessMetaData());
        SessionAccessMetaData accessMetaData = new MutableSessionAccessMetaData(entry.getAccessMetaData(), accessMutator);

        return new CompositeSessionMetaData(creationMetaData, accessMetaData);
    }

    @Override
    public ImmutableSessionMetaData createImmutableSessionMetaData(String id, CompositeSessionMetaDataEntry<L> entry) {
        return new CompositeSessionMetaData(entry.getCreationMetaData(), entry.getAccessMetaData());
    }

    @Override
    public boolean remove(String id) {
        this.creationMetaDataCache.remove(new SessionCreationMetaDataKey(id));
        this.accessMetaDataCache.remove(new SessionAccessMetaDataKey(id));
        return true;
    }
}
