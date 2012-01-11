/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.ejb3.cache.impl.backing;

import java.io.Serializable;

import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.PassivationManager;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryFactory;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStoreConfig;
import org.jboss.as.ejb3.cache.spi.GroupAwareBackingCache;
import org.jboss.as.ejb3.cache.spi.GroupCompatibilityChecker;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.BackingCacheEntryStore;
import org.jboss.as.ejb3.cache.spi.ReplicationPassivationManager;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.ejb.client.Affinity;
import org.jboss.logging.Logger;
import org.jboss.marshalling.MarshallingConfiguration;

/**
 * Functions as both a StatefulObjectFactory and PassivationManager for {@link SerializationGroupMember}s.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class SerializationGroupMemberContainer<K extends Serializable, V extends Cacheable<K>, G extends Serializable>
    implements BackingCacheEntryFactory<K, V, SerializationGroupMember<K, V, G>>, ReplicationPassivationManager<K, SerializationGroupMember<K, V, G>>, BackingCacheEntryStore<K, V, SerializationGroupMember<K, V, G>> {
    private static final Logger log = Logger.getLogger(SerializationGroupMemberContainer.class);

    private final PassivationManager<K, V> passivationManager;
    private final boolean passivateEventsOnReplicate;
    private volatile BackingCacheEntryStore<K, V, SerializationGroupMember<K, V, G>> store;
    private volatile GroupAwareBackingCache<K, V, G, SerializationGroupMember<K, V, G>> delegate;

    /**
     * Cache that's managing the PassivationGroup
     */
    private final PassivatingBackingCache<G, Cacheable<G>, SerializationGroup<K, V, G>> groupCache;

    public SerializationGroupMemberContainer(PassivationManager<K, V> passivationManager,
            PassivatingBackingCache<G, Cacheable<G>, SerializationGroup<K, V, G>> groupCache,
            BackingCacheEntryStoreConfig config) {
        this.passivationManager = passivationManager;
        this.groupCache = groupCache;
        this.passivateEventsOnReplicate = config.isPassivateEventsOnReplicate();
    }

    public void setBackingCache(GroupAwareBackingCache<K, V, G, SerializationGroupMember<K, V, G>> delegate) {
        this.delegate = delegate;
    }

    public void setBackingCacheEntryStore(BackingCacheEntryStore<K, V, SerializationGroupMember<K, V, G>> store) {
        this.store = store;
    }

    @Override
    public Affinity getStrictAffinity() {
        return this.store.getStrictAffinity();
    }

    @Override
    public Affinity getWeakAffinity(K key) {
        return this.store.getWeakAffinity(key);
    }

    @Override
    public boolean hasAffinity(K key) {
        return this.store.hasAffinity(key);
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration() {
        return this.passivationManager.getMarshallingConfiguration();
    }

    @Override
    public SerializationGroupMember<K, V, G> createEntry(V item) {
        return new SerializationGroupMemberImpl<K, V, G>(item, delegate);
    }

    @Override
    public void destroyEntry(SerializationGroupMember<K, V, G> entry) {
        SerializationGroup<K, V, G> group = entry.getGroup();
        if (group != null) {
            group.removeMember(entry.getId());
            if (group.size() == 0) {
                groupCache.remove(group.getId());
            }
        }
    }

    @Override
    public void postActivate(SerializationGroupMember<K, V, G> entry) {
        log.tracef("postActivate(%s)", entry);

        // Restore the entry's ref to the group and object
        SerializationGroup<K, V, G> group = entry.getGroup();
        if (group == null && entry.getGroupId() != null) {
            group = groupCache.get(entry.getGroupId());
        }

        if (group != null) {
            group.lock();
            try {
                entry.setGroup(group);
                entry.setUnderlyingItem(group.getMemberObject(entry.getId()));
                // Notify the group that this entry is active
                group.addActive(entry);
            } finally {
                group.unlock();
            }
        }

        // Invoke callbacks on the underlying object
        if (entry.isPrePassivated()) {
            passivationManager.postActivate(entry.getUnderlyingItem());
            entry.setPrePassivated(false);
        }
    }

    @Override
    public void prePassivate(SerializationGroupMember<K, V, G> entry) {
        log.tracef("prePassivate(%s)", entry);

        // entry.obj may or may not get serialized (depends on if group
        // is in use) but it's ok to invoke callbacks now. If a caller
        // requests this entry again and the obj hadn't been serialized with
        // the group, we'll just call postActivate on it then, which is OK.
        if (!entry.isPrePassivated()) {
            passivationManager.prePassivate(entry.getUnderlyingItem());
        }

        // If this call is coming via PassivatingBackingCache.passivate(),
        // entry.group will *not* be null. In that case we are the controller
        // for the group passivation. If the call is coming via entry.prePassivate(),
        // entry.group *will* be null. In that case we are not the controller
        // of the passivation and can just return.
        SerializationGroup<K, V, G> group = entry.getGroup();
        if (group != null) {
            if (!group.tryLock())
                throw new IllegalStateException("Cannot obtain lock on " + group.getId() + " to passivate " + entry);
            try {
                // Remove ourself from group's active list so we don't get
                // called again via entry.prePassivate()
                group.removeActive(entry.getId());

                // Only tell the group to passivate if no members are in use
                if (group.getInUseCount() == 0) {
                    // Go ahead and do the real passivation.
                    groupCache.passivate(group.getId());
                } else {
                    // this turns into a pretty meaningless exercise of just
                    // passivating an empty entry. TODO consider throwing
                    // ItemInUseException here, thus aborting everything. Need to
                    // be sure that doesn't lead to problems as the exception propagates
                    log.tracef("Group %s has %d in-use members; not passivating serialization group %s", group, group.getInUseCount(), entry);
                }

                // This call didn't come through entry.prePassivate() (which nulls
                // group) so we have to do it ourselves. Otherwise
                // when this call returns, delegate may serialize the entry
                // with a ref to group and obj.
                entry.setGroup(null);
            } finally {
                group.unlock();
            }
        }
    }

    @Override
    public void preReplicate(SerializationGroupMember<K, V, G> entry) {
        // This method follows the same conceptual logic as prePassivate.
        // See the detailed comments in that method.

        log.tracef("preReplicate(%s)", entry);

        if (!entry.isPreReplicated()) {
            if (this.passivateEventsOnReplicate) {
                passivationManager.prePassivate(entry.getUnderlyingItem());
            }
            entry.setPreReplicated(true);
        }

        SerializationGroup<K, V, G> group = entry.getGroup();
        if (group != null) {
            group.lock();
            try {
                // Remove ourself from group's active list so we don't get
                // called again via entry.preReplicate()
                group.removeActive(entry.getId());

                try {
                    if (group.getInUseCount() == 0) {
                        group.getGroupCache().release(group.getId());
                        group.setGroupModified(false);
                    }
                } finally {
                    // Here we differ from prePassivate!!
                    // Restore the entry as "active" so it can get
                    // passivation callbacks
                    group.addActive(entry);
                }

                entry.setGroup(null);
            } finally {
                group.unlock();
            }
        }
    }

    @Override
    public void postReplicate(SerializationGroupMember<K, V, G> entry) {
        log.tracef("postReplicate(%s)", entry);

        // Restore the entry's ref to the group and object
        SerializationGroup<K, V, G> group = entry.getGroup();
        if (group == null && entry.getGroupId() != null) {
            group = groupCache.get(entry.getGroupId());
        }

        if (group != null) {
            group.lock();
            try {
                entry.setGroup(group);
                entry.setUnderlyingItem(group.getMemberObject(entry.getId()));

                // Notify the group that this entry is active
                group.addActive(entry);
            } finally {
                group.unlock();
            }
        }

        // Invoke callbacks on the underlying object
        if (entry.isPreReplicated()) {
            if (this.passivateEventsOnReplicate) {
                passivationManager.postActivate(entry.getUnderlyingItem());
            }
            entry.setPreReplicated(false);
        }
    }

    @Override
    public void update(SerializationGroupMember<K, V, G> entry, boolean modified) {
        store.update(entry, modified);
    }

    @Override
    public boolean isClustered() {
        return groupCache.isClustered();
    }

    @Override
    public SerializationGroupMember<K, V, G> get(K key, boolean lock) {
        SerializationGroupMember<K, V, G> entry = store.get(key, lock);
        // In case it was deserialized, make sure it has a ref to us
        if (entry != null)
            entry.setPassivatingCache(delegate);
        return entry;
    }

    @Override
    public void passivate(SerializationGroupMember<K, V, G> entry) {
        store.passivate(entry);
    }

    @Override
    public void insert(SerializationGroupMember<K, V, G> entry) {
        store.insert(entry);
    }

    @Override
    public SerializationGroupMember<K, V, G> remove(K key) {
        return store.remove(key);
    }

    @Override
    public void start() {
        store.start();
    }

    @Override
    public void stop() {
        store.stop();
    }

    @Override
    public BackingCacheEntryStoreConfig getConfig() {
        return this.store.getConfig();
    }

    @Override
    public StatefulTimeoutInfo getTimeout() {
        return this.store.getTimeout();
    }

    public boolean isCompatibleWith(SerializationGroup<K, V, G> group) {
        PassivatingBackingCache<G, Cacheable<G>, SerializationGroup<K, V, G>> otherCache = group.getGroupCache();
        if (otherCache != null) {
            return store.isCompatibleWith(otherCache.getCompatibilityChecker());
        }
        return false;
    }

    @Override
    public boolean isCompatibleWith(GroupCompatibilityChecker other) {
        return store.isCompatibleWith(other);
    }
}
