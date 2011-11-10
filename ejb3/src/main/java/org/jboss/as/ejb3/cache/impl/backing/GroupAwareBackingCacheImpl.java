/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.cache.Cacheable;
import org.jboss.as.ejb3.cache.StatefulObjectFactory;
import org.jboss.as.ejb3.cache.spi.GroupAwareBackingCache;
import org.jboss.as.ejb3.cache.spi.PassivatingBackingCache;
import org.jboss.as.ejb3.cache.spi.SerializationGroup;
import org.jboss.as.ejb3.cache.spi.SerializationGroupMember;

/**
 * Group-aware version of {@link PassivatingBackingCacheImpl}.
 *
 * @author Brian Stansberry
 * @author Paul Ferraro
 */
public class GroupAwareBackingCacheImpl<K extends Serializable, V extends Cacheable<K>, G extends Serializable>
    extends PassivatingBackingCacheImpl<K, V, SerializationGroupMember<K, V, G>>
    implements GroupAwareBackingCache<K, V, G, SerializationGroupMember<K, V, G>> {
    /**
     * Cache that's managing the SerializationGroup
     */
    private final PassivatingBackingCache<G, Cacheable<G>, SerializationGroup<K, V, G>> groupCache;

    /**
     * Container for the group members.
     */
    private final SerializationGroupMemberContainer<K, V, G> memberContainer;

    /**
     * Creates a new GroupAwareCacheImpl.
     *
     * @param memberContainer the factory for the underlying Cacheables
     * @param groupCache cache for the group
     */
    public GroupAwareBackingCacheImpl(StatefulObjectFactory<V> factory, SerializationGroupMemberContainer<K, V, G> memberContainer,
            PassivatingBackingCache<G, Cacheable<G>, SerializationGroup<K, V, G>> groupCache, ThreadFactory threadFactory) {
        super(factory, memberContainer, memberContainer, memberContainer, threadFactory);

        this.groupCache = groupCache;
        this.memberContainer = memberContainer;
        this.memberContainer.setBackingCache(this);
    }

    public GroupAwareBackingCacheImpl(StatefulObjectFactory<V> factory, SerializationGroupMemberContainer<K, V, G> memberContainer,
            PassivatingBackingCache<G, Cacheable<G>, SerializationGroup<K, V, G>> groupCache, ScheduledExecutorService executor) {
        super(factory, memberContainer, memberContainer, memberContainer, executor);

        this.groupCache = groupCache;
        this.memberContainer = memberContainer;
        this.memberContainer.setBackingCache(this);
    }

    @Override
    public SerializationGroup<K, V, G> createGroup() {
        return groupCache.create();
    }

    @Override
    public void setGroup(V object, SerializationGroup<K, V, G> group) {
        K key = object.getId();
        SerializationGroupMember<K, V, G> entry = peek(key);
        entry.lock();
        try {
            if (entry.getGroup() != null) {
                throw EjbMessages.MESSAGES.existingSerializationGroup(key,  entry.getGroup());
            }
            // Validate we share a common groupCache with the group
            if (!memberContainer.isCompatibleWith(group)) {
                throw EjbMessages.MESSAGES.incompatibleSerializationGroup(object, group);
            }
            entry.setGroup(group);
            entry.getGroup().addMember(entry);
        } finally {
            entry.unlock();
        }
    }

    @Override
    public void notifyPreReplicate(SerializationGroupMember<K, V, G> entry) {
        log.tracef("notifyPreReplicate(%s)", entry);

        if (!entry.isPreReplicated()) {
            // We just *try* to lock; a preReplication is low priority.
            if (!entry.tryLock()) {
                throw EjbMessages.MESSAGES.cacheEntryInUse(entry);
            }

            try {
                if (entry.isInUse()) {
                    throw EjbMessages.MESSAGES.cacheEntryInUse(entry);
                }

                memberContainer.preReplicate(entry);

                entry.setPreReplicated(true);
            } finally {
                entry.unlock();
            }
        }
    }
}
